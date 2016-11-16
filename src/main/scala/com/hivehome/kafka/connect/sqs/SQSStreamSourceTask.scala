/**
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  * <p>
  * http://www.apache.org/licenses/LICENSE-2.0
  * <p>
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  **/
package com.hivehome.kafka.connect.sqs

import java.util.{List => JList, Map => JMap, _}
import javax.jms._

import com.amazon.sqs.javamessaging.SQSConnectionFactory
import com.amazon.sqs.javamessaging.message.{SQSBytesMessage, SQSObjectMessage, SQSTextMessage}
import com.amazonaws.auth.{AWSCredentialsProviderChain, BasicAWSCredentials, DefaultAWSCredentialsProviderChain}
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.source.{SourceRecord, SourceTask}

import scala.collection.JavaConverters._

object SQSStreamSourceTask {
  val SqsQueueField: String = "queue"
  val PositionField: String = "position"
  private val ValueSchema: Schema = Schema.STRING_SCHEMA
}

class SQSStreamSourceTask extends SourceTask with StrictLogging {
  private var conf: Conf = _
  private var consumer: MessageConsumer = null

  def version: String = Version()

  def start(props: JMap[String, String]) {
    conf = Conf.parse(props.asScala.toMap).get
  }

  @throws(classOf[InterruptedException])
  def poll: JList[SourceRecord] = {
    def offsetKey(filename: String) = Collections.singletonMap(SQSStreamSourceTask.SqsQueueField, filename)
    def offsetValue(pos: String) = Collections.singletonMap(SQSStreamSourceTask.PositionField, pos)

    if (consumer == null) {
      try {
        val chain = buildCredentialsProviderChain
        consumer = createSQSConsumer(chain)
        logger.debug("Opened SQS topic {} for reading", conf.queueName)
      }
      catch {
        case e: JMSException =>
          logger.error("JMSException", e)
          e.printStackTrace()
      }
    }
    try {
      val msg = consumer.receive
      logger.debug("Received message {}", msg)
      val extractedMessage = extract(msg)
      val key = offsetKey(conf.queueName.get)
      val value = offsetValue(msg.getJMSMessageID)
      List(new SourceRecord(key, value, conf.topicName.get, SQSStreamSourceTask.ValueSchema, extractedMessage)).asJava
    }
    catch {
      case e: JMSException =>
        logger.error("JMSException", e)
        List[SourceRecord]().asJava
    }
  }

  @throws(classOf[JMSException])
  private def extract(msg: Message): String = {
    msg match {
      case text: SQSTextMessage => text.getText
      case bytes: SQSBytesMessage => new String(bytes.getBodyAsBytes)
      case objectMsg: SQSObjectMessage => objectMsg.getObject.toString
      case _ => msg.toString
    }
  }

  @throws(classOf[JMSException])
  private def createSQSConsumer(chain: AWSCredentialsProviderChain): MessageConsumer = {
    val connectionFactory = SQSConnectionFactory.builder
      .withRegion(Region.getRegion(Regions.EU_WEST_1))
      .withAWSCredentialsProvider(chain)
      .build

    val connection = connectionFactory.createConnection
    val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    val queue = session.createQueue(conf.queueName.get)
    val consumer = session.createConsumer(queue)
    connection.start()
    consumer
  }

  private def buildCredentialsProviderChain: AWSCredentialsProviderChain = {
    if (conf.awsKey.isDefined && conf.awsSecret.isDefined) {
      val key = conf.awsKey.get
      val secret = conf.awsSecret.get
      val credentials = new BasicAWSCredentials(key, secret)
      new AWSCredentialsProviderChain(new StaticCredentialsProvider(credentials), new DefaultAWSCredentialsProviderChain)
    } else new DefaultAWSCredentialsProviderChain
  }

  def stop() {
    logger.debug("Stopping")
    synchronized {
      try {
        if (consumer != null) {
          consumer.close()
          logger.debug("Closed input stream")
        }
      }
      catch {
        case e: JMSException =>
          logger.error("Failed to close consumer stream: ", e)
      }
      this.notify()
    }
  }

}