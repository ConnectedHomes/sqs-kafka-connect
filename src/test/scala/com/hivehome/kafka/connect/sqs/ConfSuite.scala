package com.hivehome.kafka.connect.sqs

import org.apache.kafka.connect.errors.ConnectException
import org.scalatest.OptionValues._
import org.scalatest.TryValues._
import org.scalatest.{FunSuite, Matchers}

class ConfSuite extends FunSuite with Matchers {

  private val UsEast = "us-east-1"
  private val EuWest = "eu-west-1"

  val mandatoryProps = Map[String, String](
    Conf.SourceSqsQueue -> "in",
    Conf.DestinationKafkaTopic -> "out"
  )

  val optionalProps = Map[String, String](
    Conf.AwsKey -> "key",
    Conf.AwsSecret -> "secret",
    Conf.AwsRegion -> UsEast
  )

  val allProps = mandatoryProps ++ optionalProps

  test("should parse all configurations from a map") {
    val tryConf = Conf.parse(allProps)

    val conf = tryConf.success.value
    conf.queueName.value shouldEqual "in"
    conf.topicName.value shouldEqual "out"
    conf.awsRegion shouldEqual UsEast
    conf.awsKey.value shouldEqual "key"
    conf.awsSecret.value shouldEqual "secret"
  }

  test("should parse mandatory configurations from a map") {
    val tryConf = Conf.parse(mandatoryProps)

    val conf = tryConf.success.value
    conf.queueName.value shouldEqual "in"
    conf.topicName.value shouldEqual "out"
    conf.awsRegion shouldEqual EuWest
  }

  test("should fail when mandatory config is missing") {
    val tryConf = Conf.parse(Map())

    tryConf.failure.exception.getClass shouldBe classOf[ConnectException]
  }
}
