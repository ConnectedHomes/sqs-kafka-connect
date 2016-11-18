package com.hivehome.kafka.connect.sqs

import com.amazonaws.services.sqs.model.{SendMessageRequest, SendMessageResult}
import org.apache.kafka.connect.source.SourceRecord
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.collection.JavaConverters._

/*
 * Note: This class requires AWS keys and secret in the environment or
 * system properties to run. The keys should have access to SQS in AWS.
 */
class SQSStreamSourceTaskSuite extends FunSuite with BeforeAndAfterAll with Matchers with Eventually with SQSSupport {

  val props = Map[String, String](
    Conf.SourceSqsQueue -> queueName,
    Conf.DestinationKafkaTopic -> "out"
  ).asJava

  override def beforeAll() = createQueue()

  override def afterAll() = deleteQueue()

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

    // and
    val secondMsgText = "This is my message text."
    val secondSendResult = sendMessage(secondMsgText)
    // no more messages and will block forever
    val secondSourceRecords = eventually(task.poll.asScala)

    // then
    secondSourceRecords should have size 1

    // and
    val secondSourceRecord = secondSourceRecords.head
    verify(secondSourceRecord, secondSendResult.getMessageId, secondMsgText)

    // when
    task.commitRecord(secondSourceRecord)

    // should not throw exception
    task.stop()
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

    // original message is redelivered
    val secondSourceRecords = eventually (task.poll.asScala)

    // then
    val secondSourceRecord = secondSourceRecords.head
    verify(secondSourceRecord, sendResult.getMessageId, msgText)

    // should not throw exception
    task.stop()
  }

  def verify(record: SourceRecord, msgId: String, msgText: String): Unit = {
    record.value().asInstanceOf[String] shouldEqual msgText
    record.sourcePartition() shouldEqual Map("queue" -> queueName).asJava
    record.sourceOffset() shouldEqual Map("messageId" -> ("ID:" + msgId)).asJava
    record.topic() shouldEqual "out"
  }

}
