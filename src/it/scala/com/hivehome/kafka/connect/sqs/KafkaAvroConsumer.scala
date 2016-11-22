package com.hivehome.kafka.connect.sqs

import java.time.Instant
import java.util.{Properties, UUID}

import com.typesafe.scalalogging.StrictLogging
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.kafka.clients.consumer.{ConsumerConfig => ConsumerConfigConst, KafkaConsumer}
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Client for consuming from a `Avro` messages from `Kafka` topics.
  * Non-Avro messages _cannot_ be consumed.
  *
  * This class is the Kafka 0.10 version of the old avro consumer which
  * utilizes the new `KafkaConsumer` instead of the `ConsumerIterator`
  * which was used in the previous version.
  */
class KafkaAvroConsumer[V](cons: KafkaConsumer[_, V]) extends StrictLogging {
  val PollingInterval = 100

  /**
    * Gets messages from topic.
    *
    * @param numberOfMessagesExpected number of messages to receive within the `timeout`
    * @param timeout                  duration to wait for messages
    * @param accept                   predicate to determine if a message should be accepted
    * @return messages
    * @throws AssertionError if insufficient messages received.
    */
  def poll(numberOfMessagesExpected: Int,
           timeout: FiniteDuration = 30 seconds,
           accept: V => Boolean = _ => true): Vector[V] = {

    val deadline = timeout.fromNow
    var messages = Vector.empty[V]
    while (deadline.hasTimeLeft && messages.size < numberOfMessagesExpected) {
      val records = cons.poll(PollingInterval)
      // convert to Seq so that we have all the messages once we have
      // exhausted the iterator
      val msgsSeq = records.iterator().asScala.toSeq
      messages = messages ++ msgsSeq.map(_.value()).filter(accept).toVector
    }
    logger.debug("Number of messages received {}", messages.size)

    if (messages.size < numberOfMessagesExpected) {
      throw new AssertionError(s"Expected $numberOfMessagesExpected messages within $timeout, but only received ${messages.size}. $messages")
    }

    // Possibly throw exception if too many messages?
    messages
  }
}

object KafkaAvroConsumer {
  val logger = LoggerFactory.getLogger(getClass)

  def apply[V](kafkaProps: Map[String, String], topicName: String): KafkaAvroConsumer[V] = {
    val props = new Properties()
    props.putAll(kafkaProps.asJava)
    props.put(ConsumerConfigConst.GROUP_ID_CONFIG, "test" + UUID.randomUUID().toString.substring(0, 10))
    props.put(ConsumerConfigConst.KEY_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer])
    props.put(ConsumerConfigConst.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[KafkaAvroDeserializer])

    logger.info(s"Consuming from $topicName with properties $props")
    val cons = new KafkaConsumer[Array[Byte], V](props)
    cons.subscribe(Seq(topicName).asJava)
    new KafkaAvroConsumer(cons)
  }

  def main(args: Array[String]): Unit = {
    val props = Map("bootstrap.servers" -> "localhost:9092", "schema.registry.url" -> "http://localhost:8081")
    val consumer = KafkaAvroConsumer(props, topicName = "connect-test")

    while (true) {
      logger.debug(Instant.now().toString)
      logger.debug(consumer.poll(1, 500 seconds).toString())
      logger.debug(Instant.now().toString)
    }
  }
}
