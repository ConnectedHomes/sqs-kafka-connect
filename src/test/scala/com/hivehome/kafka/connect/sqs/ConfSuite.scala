package com.hivehome.kafka.connect.sqs

import org.scalatest.OptionValues._
import org.scalatest.{FunSuite, Matchers}

class ConfSuite extends FunSuite with Matchers {

  val mandatoryProps = Map[String, String](
    Conf.SourceSqsQueue -> "in",
    Conf.DestinationKafkaTopic -> "out"
  )

  val allProps = mandatoryProps
    .updated(Conf.AwsKey, "key")
    .updated(Conf.AwsSecret, "secret")
    .updated(Conf.AwsRegion, "us-east-1")

  test("should parse all configurations from a map") {
    val tryConf = Conf.parse(allProps)

    tryConf.isSuccess shouldBe true
    val conf = tryConf.toOption.value

    conf.queueName.value shouldEqual "in"
    conf.topicName.value shouldEqual "out"
    conf.awsRegion shouldEqual "us-east-1"
    conf.awsKey.value shouldEqual "key"
    conf.awsSecret.value shouldEqual "secret"
  }

  test("should parse mandatory configurations from a map") {
    val tryConf = Conf.parse(mandatoryProps)

    tryConf.isSuccess shouldBe true
    val conf = tryConf.toOption.value

    conf.queueName.value shouldEqual "in"
    conf.topicName.value shouldEqual "out"
  }

  test("should fail when mandatory config is missing") {
    val tryConf = Conf.parse(Map())

    tryConf.isSuccess shouldBe false
  }
}
