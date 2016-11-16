///**
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements.  See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License.  You may obtain a copy of the License at
// *
// *    http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// **/
//package com.hivehome.kafka.connect.sqs;
//
//import org.apache.kafka.common.config.ConfigDef;
//import org.apache.kafka.common.config.ConfigDef.Importance;
//import org.apache.kafka.common.config.ConfigDef.Type;
//import org.apache.kafka.common.utils.AppInfoParser;
//import org.apache.kafka.connect.connector.Task;
//import org.apache.kafka.connect.errors.ConnectException;
//import org.apache.kafka.connect.source.SourceConnector;
//
//import java.util.*;
//
///**
// * A simple Amazon SQS source connector to ingest data to Kafka using Connect.
// */
//public class SQSStreamSourceConnector extends SourceConnector {
//    public static final String DESTINATION_KAFKA_TOPIC = "destination.topic";
//    public static final String SOURCE_SQS_QUEUE = "source.queue";
//    public static final String AWS_KEY = "aws.key";
//    public static final String AWS_SECRET = "aws.secret";
//
//    private static final ConfigDef CONFIG_DEF = new ConfigDef()
//        .define(SOURCE_SQS_QUEUE, Type.STRING, Importance.HIGH, "Source SQS queue name to consumer from.")
//        .define(DESTINATION_KAFKA_TOPIC, Type.STRING, Importance.HIGH, "Destination Kafka topicName to publish data to")
//        .define(AWS_KEY, Type.STRING, Importance.MEDIUM, "AWS Key to connect to SQS")
//        .define(AWS_SECRET, Type.STRING, Importance.MEDIUM, "AWS secret to connect to SQS");
//
//    private String queueName;
//    private String topicName;
//    private Optional<String> awsKey;
//    private Optional<String> awsSecret;
//
//    @Override
//    public String version() {
//        return AppInfoParser.getVersion();
//    }
//
//    @Override
//    public void start(Map<String, String> props) {
//        queueName = props.get(SOURCE_SQS_QUEUE);
//        topicName = props.get(DESTINATION_KAFKA_TOPIC);
//        awsKey = Optional.ofNullable(props.get(AWS_KEY));
//        awsSecret = Optional.ofNullable(props.get(AWS_SECRET));
//        validate();
//    }
//
//    private void validate() {
//        if (queueName == null || queueName.isEmpty())
//            throw new ConnectException("SQSStreamSourceConnector configuration must include 'queueName' setting");
//        if (queueName.contains(","))
//            throw new ConnectException("SQSStreamSourceConnector should only have a single queueName when used as a source.");
//        if (topicName == null || topicName.isEmpty())
//            throw new ConnectException("SQSStreamSourceConnector configuration must include 'queueName' setting");
//    }
//
//    @Override
//    public Class<? extends Task> taskClass() {
//        return SQSStreamSourceTask.class;
//    }
//
//    @Override
//    public List<Map<String, String>> taskConfigs(int maxTasks) {
//        ArrayList<Map<String, String>> configs = new ArrayList<>();
//        // Only one input stream makes sense.
//        Map<String, String> config = new HashMap<>();
//        if (queueName != null)
//            config.put(SOURCE_SQS_QUEUE, queueName);
//        config.put(DESTINATION_KAFKA_TOPIC, topicName);
//        config.put(AWS_KEY, awsKey.orElse(null));
//        config.put(AWS_SECRET, awsSecret.orElse(null));
//        configs.add(config);
//        return configs;
//    }
//
//    @Override
//    public void stop() {
//        // Nothing to do since SQSStreamSourceConnector has no background monitoring.
//    }
//
//    @Override
//    public ConfigDef config() {
//        return CONFIG_DEF;
//    }
//}
