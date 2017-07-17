package com.hivehome.kafka.connect.sqs

import javax.jms.MessageConsumer

import com.amazon.sqs.javamessaging.message.SQSTextMessage
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class SQSConsumerSuite extends FunSuite with BeforeAndAfterAll with Matchers with SQSSupport with Eventually {

  val conf = Conf(queueName = Some(queueName))
  var consumer: MessageConsumer = _

  override def beforeAll(): Unit = {
    createQueue()
    consumer = SQSConsumer(conf)
  }

  override def afterAll(): Unit = {
    consumer.close()
    deleteQueue()
  }

  test("should create consumer which receives messages") {
    sendMessage("blah")

    val msg = consumer.receive()
    msg.acknowledge()

    val text = msg.asInstanceOf[SQSTextMessage].getText

    text shouldEqual "blah"
  }

  test("should redeliver message when not acked") {
    sendMessage("blah")

    val msg = consumer.receive()
    // msg.acknowledge()

    val text = msg.asInstanceOf[SQSTextMessage].getText
    text shouldEqual "blah"

    val msg2 = eventually(consumer.receive())
    msg2.acknowledge()

    val text2 = msg2.asInstanceOf[SQSTextMessage].getText
    text2 shouldEqual "blah"
  }
}
