package com.hivehome.kafka.connect.sqs

import java.time.Instant

import com.typesafe.scalalogging.StrictLogging
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * End to end integration test which sends a message to SQS and expects the
  * kafka connect plugin to save it to a kafka topic.
  *
  * This test expects the following
  * - Running kafka, zookeeper and schema registry
  * - SQS connector already running in kafka connect
  * - SQS queue named `test-sqs`
  * - Kafka topic named `connect-test`
  *
  * Should this test move to the kafka-connect-deployment project?
  */
class E2ESpec extends FunSuite with Matchers with SQSSupport with StrictLogging {
  private val KafkaTopic: String = "connect-test"
  override val queueName = "test-sqs" // kafka connect should be setup with this SQS
  queueUrl = sqs.getQueueUrl(queueName).getQueueUrl

  private val props = Map(
    "bootstrap.servers" -> sys.env.getOrElse("KAFKA", "localhost:9092"),
    "schema.registry.url" -> sys.env.getOrElse("SCHEMA_REGISTRY", "http://localhost:8081"))

  val consumer = KafkaAvroConsumer[String, String](props, topicName = KafkaTopic)

  test("should route message SQS -> Kafka") {
    Future {
      // sleep is required so that the message to SQS
      // is sent after the consumer is listening on the kafka topic
      Thread.sleep(500)
      logger.debug("sending message..")
      sendMessage(Instant.now().toString)
      logger.debug("sent message..")
    }

    val msgs = consumer.poll(1, accept = _ => true)

    msgs should have size 1
  }
}
