package com.hivehome.kafka.connect.sqs

import org.scalatest.{FunSuite, Matchers}

import scala.collection.JavaConverters._

class SQSSourceConnectorSuite extends FunSuite with Matchers {

  val connector = new SQSSourceConnector()

  val props = Map[String, String](
    Conf.SourceSqsQueue -> "in",
    Conf.DestinationKafkaTopic -> "out"
  ).asJava

  test("should return task class") {
    connector.taskClass shouldEqual classOf[SQSSourceTask]
  }

  test("should return config def") {
    connector.config shouldEqual Conf.ConfigDef
  }

  test("should return successfully from start") {
    connector.start(props)
  }

  test("should create task configs") {
    connector.start(props)
    val maxTasks = 10
    val taskConfigs = connector.taskConfigs(maxTasks).asScala

    taskConfigs should have size maxTasks
    taskConfigs foreach { taskConfig =>
      taskConfig shouldEqual props
    }
  }
}
