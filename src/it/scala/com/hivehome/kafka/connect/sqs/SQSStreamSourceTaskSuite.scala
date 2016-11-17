package com.hivehome.kafka.connect.sqs

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.{CreateQueueRequest, SendMessageRequest, SendMessageResult}
import org.apache.kafka.connect.source.SourceRecord
import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/*
 * Note: This class requires AWS keys and secret in the environment or
 * system properties to run. The keys should have access to SQS in AWS.
 */
class SQSStreamSourceTaskSuite extends FunSuite with BeforeAndAfterAll with Matchers {

  val queueName = Gen.alphaStr
    .map(a => if (a.length > 10) a.substring(0, 10) else a)
    .map(a => s"test-connect-$a")
    .sample.get
  var queueUrl: String = null

  val sqs = new AmazonSQSClient()
  sqs.setRegion(Region.getRegion(Regions.EU_WEST_1))

  val props = Map[String, String](
    Conf.SourceSqsQueue -> queueName,
    Conf.DestinationKafkaTopic -> "out"
  ).asJava

  override def beforeAll() = {
    val request = new CreateQueueRequest(queueName)
      .withAttributes(Map("VisibilityTimeout" -> "2").asJava)
    val result = sqs.createQueue(request)
    queueUrl = result.getQueueUrl
    println("Url for created Queue = " + queueUrl)
  }

  override def afterAll() = {
    sqs.deleteQueue(queueUrl)
  }

  test("should receive message from SQS and acknowledge") {
    // given
    val msgText = "This is my message text."
    val sendResult = sendMessage(msgText)

    // when
    val task = new SQSStreamSourceTask
    task.start(props)
    val sourceRecords = task.poll.asScala

    // then
    sourceRecords should have size 1

    // and
    val sourceRecord = sourceRecords.head
    verify(sourceRecord, sendResult.getMessageId, msgText)

    // when
    task.commitRecord(sourceRecord)

    // no more messages and will block forever
    val fut = Future(task.poll.asScala)
    Thread.sleep(1000)

    // then
    fut.isCompleted shouldBe false

    // when
    task.stop()

    //then
    fut.isCompleted shouldBe true
  }

  test("should receive redelivered message after visibility timeout") {
    // given
    val msgText = "This is my redelivery message text."
    val sendResult = sendMessage(msgText)

    // when
    val task = new SQSStreamSourceTask
    task.start(props)
    val sourceRecords = task.poll.asScala

    // then
    sourceRecords should have size 1

    // and
    val sourceRecord = sourceRecords.head
    verify(sourceRecord, sendResult.getMessageId, msgText)

    // when no commitRecord
    // task.commitRecord(sourceRecord)

    // no more messages and will block forever
    val fut = Future (task.poll.asScala)
    Thread.sleep(3000) // more than message visibility timeout

    // then
    fut.isCompleted shouldBe true
    val secondSourceRecord = fut.value.flatMap(_.toOption).flatMap(_.headOption).get
    verify(secondSourceRecord, sendResult.getMessageId, msgText)

    // when
    task.stop()

    //then
    fut.isCompleted shouldBe true
  }

  def verify(record: SourceRecord, msgId: String, msgText: String): Unit = {
    record.value().asInstanceOf[String] shouldEqual msgText
    record.sourcePartition() shouldEqual Map("queue" -> queueName).asJava
    record.sourceOffset() shouldEqual Map("messageId" -> ("ID:" + msgId)).asJava
    record.topic() shouldEqual "out"
  }

  def sendMessage(msgText: String): SendMessageResult = {
    sqs.sendMessage(new SendMessageRequest()
      .withQueueUrl(queueUrl)
      .withMessageBody(msgText))
  }
}
