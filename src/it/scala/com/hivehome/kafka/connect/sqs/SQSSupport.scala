package com.hivehome.kafka.connect.sqs

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.{CreateQueueRequest, SendMessageRequest, SendMessageResult}
import org.scalacheck.Gen

import scala.collection.JavaConverters._

trait SQSSupport {
  val queueName = Gen.alphaStr
    .map(a => s"test-connect-${a.take(10)}")
    .sample.get
  var queueUrl: String = null

  val sqs = new AmazonSQSClient()
  sqs.setRegion(Region.getRegion(Regions.EU_WEST_1))

  def createQueue(): Unit = {
    val request = new CreateQueueRequest(queueName)
      .withAttributes(Map("VisibilityTimeout" -> "2").asJava)
    val result = sqs.createQueue(request)
    queueUrl = result.getQueueUrl
    println("Url for created Queue = " + queueUrl)
  }

  def deleteQueue(): Unit = {
    sqs.deleteQueue(queueUrl)
  }

  def sendMessage(msgText: String): SendMessageResult = {
    sqs.sendMessage(new SendMessageRequest()
      .withQueueUrl(queueUrl)
      .withMessageBody(msgText))
  }
}
