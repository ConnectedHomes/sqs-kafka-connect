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

import com.amazon.sqs.javamessaging.message.{SQSBytesMessage, SQSObjectMessage, SQSTextMessage}
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.source.{SourceRecord, SourceTask}

import scala.collection.JavaConverters._
import scala.collection.mutable

object SQSStreamSourceTask {
  val SqsQueueField: String = "queue"
  val MessageId: String = "messageId"
  private val ValueSchema: Schema = Schema.STRING_SCHEMA
}

class SQSStreamSourceTask extends SourceTask with StrictLogging {
  private var conf: Conf = _
  private var consumer: MessageConsumer = null
  // MessageId to MessageHandle used to ack the message on the commitRecord method invocation
  private val unAckedMessages = mutable.Map[String, Message]()

  def version: String = Version()

  def start(props: JMap[String, String]): Unit = {
    conf = Conf.parse(props.asScala.toMap).get

    logger.debug("Creating consumer...")
    synchronized {
      try {
        consumer = SQSConsumer(conf)
        logger.info("Created consumer to  SQS topic {} for reading", conf.queueName)
      }
      catch {
        case e: JMSException =>
          logger.error("JMSException", e)
      }
    }
  }

  @throws(classOf[InterruptedException])
  def poll: JList[SourceRecord] = {
    def offsetKey(queueName: String) = Collections.singletonMap(SQSStreamSourceTask.SqsQueueField, queueName)
    def offsetValue(msgId: String) = Collections.singletonMap(SQSStreamSourceTask.MessageId, msgId)

    assert(consumer != null) // should be initialised as part of start()
    try {
      val msg = consumer.receive
      logger.debug("Received message {}", msg)
      unAckedMessages.update(msg.getJMSMessageID, msg)
      val extracted = extract(msg)
      val key = offsetKey(conf.queueName.get)
      val value = offsetValue(msg.getJMSMessageID)
      List(new SourceRecord(key, value, conf.topicName.get, SQSStreamSourceTask.ValueSchema, extracted)).asJava
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

  @throws(classOf[InterruptedException])
  override def commitRecord(record: SourceRecord): Unit = {
    val offset = record.sourceOffset()
    val msgId = offset.get(SQSStreamSourceTask.MessageId).asInstanceOf[String]
    val msg = unAckedMessages.remove(msgId)
    msg.foreach(_.acknowledge())
  }

  def stop() {
    logger.debug("Stopping task")
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