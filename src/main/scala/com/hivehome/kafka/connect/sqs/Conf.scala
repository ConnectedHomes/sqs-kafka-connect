/**
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  **/
package com.hivehome.kafka.connect.sqs

import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.common.config.ConfigDef.{Importance, Type}
import org.apache.kafka.connect.errors.ConnectException

import scala.util.Try

case class Conf(queueName: Option[String] = None,
                topicName: Option[String] = None,
                awsRegion: String = "eu-west-1",
                awsKey: Option[String] = None,
                awsSecret: Option[String] = None) {
  def toMap: Map[String, String] = {
    import Conf._
    Map[String, Option[String]]()
      .updated(SourceSqsQueue, queueName)
      .updated(DestinationKafkaTopic, topicName)
      .updated(AwsKey, awsKey)
      .updated(AwsSecret, awsSecret)
      .collect { case (k, Some(v)) => (k, v) }
  }
}

object Conf {
  val DestinationKafkaTopic = "destination.topic"
  val SourceSqsQueue = "source.queue"
  val AwsKey = "aws.key"
  val AwsSecret = "aws.secret"
  val AwsRegion = "aws.region"

  val ConfigDef = new ConfigDef()
    .define(SourceSqsQueue, Type.STRING, Importance.HIGH, "Source SQS queue name to consumer from.")
    .define(DestinationKafkaTopic, Type.STRING, Importance.HIGH, "Destination Kafka topicName to publish data to")
    .define(AwsKey, Type.STRING, "", Importance.MEDIUM, "AWS Key to connect to SQS")
    .define(AwsSecret, Type.STRING, "", Importance.MEDIUM, "AWS secret to connect to SQS")

  def parse(props: Map[String, String]): Try[Conf] = Try {
    val queueName = props.get(Conf.SourceSqsQueue)
    val topicName = props.get(Conf.DestinationKafkaTopic)
    val awsKey = props.get(Conf.AwsKey).filter(_.nonEmpty)
    val awsSecret = props.get(Conf.AwsSecret).filter(_.nonEmpty)
    val awsRegion = props.get(Conf.AwsRegion)

    if (queueName == null || queueName.isEmpty)
      throw new ConnectException("Configuration must include 'queueName' setting")
    if (queueName.contains(","))
      throw new ConnectException("Configuration should only have a single queueName when used as a source.")
    if (topicName == null || topicName.isEmpty)
      throw new ConnectException("Configuration must include 'topicName' setting")

    val conf = Conf(queueName = queueName, topicName = topicName, awsKey = awsKey, awsSecret = awsSecret)
    awsRegion match {
      case Some(region) => conf.copy(awsRegion = region)
      case _ => conf
    }
  }
}

