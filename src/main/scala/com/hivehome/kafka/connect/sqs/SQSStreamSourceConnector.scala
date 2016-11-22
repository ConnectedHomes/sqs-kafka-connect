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

import java.util.{Map => JMap, List => JList}

import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.source.SourceConnector

import scala.collection.JavaConverters._

/**
  * A simple Amazon SQS source connector to ingest data to Kafka using Connect.
  */
class SQSStreamSourceConnector extends SourceConnector {
  private var conf: Conf = _

  def version: String = Version()

  override def start(props: JMap[String, String]): Unit = {
    conf = Conf.parse(props.asScala.toMap).get
  }

  def taskClass: Class[_ <: Task] = classOf[SQSStreamSourceTask]

  def taskConfigs(maxTasks: Int): JList[JMap[String, String]] = (0 until maxTasks).map(_ => conf.toMap.asJava).asJava

  def stop(): Unit = {}

  def config: ConfigDef = Conf.ConfigDef
}