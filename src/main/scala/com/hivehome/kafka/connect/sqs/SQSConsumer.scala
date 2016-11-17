package com.hivehome.kafka.connect.sqs

import javax.jms.{JMSException, MessageConsumer, Session}

import com.amazon.sqs.javamessaging.SQSConnectionFactory
import com.amazonaws.auth.{AWSCredentialsProviderChain, BasicAWSCredentials, DefaultAWSCredentialsProviderChain}
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.regions.{Region, Regions}

object SQSConsumer {
  def apply(conf: Conf): MessageConsumer = {
    val chain = buildCredentialsProviderChain(conf)
    createSQSConsumer(conf, chain)
  }

  @throws(classOf[JMSException])
  private def createSQSConsumer(conf: Conf, chain: AWSCredentialsProviderChain): MessageConsumer = {
    val region = Regions.fromName(conf.awsRegion)
    val connectionFactory = SQSConnectionFactory.builder
      .withRegion(Region.getRegion(region))
      .withAWSCredentialsProvider(chain)
      .build

    val connection = connectionFactory.createConnection
    val session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE)
    val queue = session.createQueue(conf.queueName.get)
    val consumer = session.createConsumer(queue)
    connection.start()
    consumer
  }

  private def buildCredentialsProviderChain(conf: Conf): AWSCredentialsProviderChain = {
    (conf.awsKey, conf.awsSecret) match {
      case (Some(key), Some(secret)) =>
        val credentials = new BasicAWSCredentials(key, secret)
        new AWSCredentialsProviderChain(new StaticCredentialsProvider(credentials), new DefaultAWSCredentialsProviderChain)
      case _ => new DefaultAWSCredentialsProviderChain
    }
  }
}
