package com.hivehome.kafka.connect.sqs

import javax.jms.{JMSException, MessageConsumer, Session}

import com.amazon.sqs.javamessaging.SQSConnectionFactory
import com.amazonaws.auth.{AWSCredentialsProviderChain, BasicAWSCredentials, DefaultAWSCredentialsProviderChain}
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.regions.{Region, Regions}

class SQSConsumer(conf: Conf) {
  def create(): MessageConsumer = {
    val chain = buildCredentialsProviderChain
    createSQSConsumer(chain)
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
}

object SQSConsumer {
  def apply(conf: Conf): MessageConsumer = new SQSConsumer(conf).create()
}
