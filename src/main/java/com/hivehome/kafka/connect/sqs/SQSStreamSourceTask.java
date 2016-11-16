///**
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements.  See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License.  You may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// **/
//
//package com.hivehome.kafka.connect.sqs;
//
//import com.amazon.sqs.javamessaging.SQSConnectionFactory;
//import com.amazon.sqs.javamessaging.message.SQSBytesMessage;
//import com.amazon.sqs.javamessaging.message.SQSObjectMessage;
//import com.amazon.sqs.javamessaging.message.SQSTextMessage;
//import com.amazonaws.auth.AWSCredentialsProviderChain;
//import com.amazonaws.auth.BasicAWSCredentials;
//import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
//import com.amazonaws.internal.StaticCredentialsProvider;
//import com.amazonaws.regions.Region;
//import com.amazonaws.regions.Regions;
//import org.apache.kafka.connect.data.Schema;
//import org.apache.kafka.connect.errors.ConnectException;
//import org.apache.kafka.connect.source.SourceRecord;
//import org.apache.kafka.connect.source.SourceTask;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.jms.*;
//import javax.jms.Queue;
//import java.util.*;
//
//public class SQSStreamSourceTask extends SourceTask {
//    private static final Logger log = LoggerFactory.getLogger(SQSStreamSourceTask.class);
//    public static final String SQS_QUEUE_FIELD = "queue";
//    public static final String POSITION_FIELD = "position";
//    private static final Schema VALUE_SCHEMA = Schema.STRING_SCHEMA;
//
//    private String queue;
//    private String topic;
//    private Optional<String> awsKey;
//    private Optional<String> awsSecret;
//
//    private MessageConsumer consumer;
//
//    @Override
//    public String version() {
//        return new SQSStreamSourceConnector().version();
//    }
//
//    @Override
//    public void start(Map<String, String> props) {
//        queue = props.get(Conf.SourceSqsQueue());
//        if (queue == null)
//            throw new ConnectException("SQSStreamSourceTask config missing queue setting");
//        topic = props.get(Conf.DestinationKafkaTopic());
//        if (topic == null)
//            throw new ConnectException("SQSStreamSourceTask config missing topic setting");
//        awsKey = Optional.ofNullable(props.get(Conf.AwsKey()));
//        awsSecret = Optional.ofNullable(props.get(Conf.AwsSecret()));
//    }
//
//    @Override
//    public List<SourceRecord> poll() throws InterruptedException {
//        if (consumer == null) {
//            try {
//                createSQSConsumer();
//            } catch (JMSException e) {
//                log.error("JMSException", e);
//                e.printStackTrace();
//            }
//        }
//
//        try {
//            ArrayList<SourceRecord> records = new ArrayList<>();
//            Message msg = consumer.receive();
//            log.info("Received message {}", msg);
//            String extractedMessage = extract(msg);
//            records.add(new SourceRecord(offsetKey(queue), offsetValue(msg.getJMSMessageID()), topic, VALUE_SCHEMA, extractedMessage));
//            return records;
//        } catch (JMSException e) {
//            // Underlying stream was killed, probably as a result of calling stop. Allow to return
//            // null, and driving thread will handle any shutdown if necessary.
//            log.error("JMSException", e);
//        }
//        return null;
//    }
//
//    private String extract(Message msg) throws JMSException {
//        String extracted;
//        if (msg instanceof SQSTextMessage) {
//            SQSTextMessage textMessage = ((SQSTextMessage) msg);
//            extracted = textMessage.getText();
//        } else if (msg instanceof SQSBytesMessage) {
//            SQSBytesMessage bytesMessage = ((SQSBytesMessage) msg);
//            extracted = new String(bytesMessage.getBodyAsBytes());
//        } else if (msg instanceof SQSObjectMessage) {
//            SQSObjectMessage objectMessage = ((SQSObjectMessage) msg);
//            Object obj = objectMessage.getObject();
//            extracted = obj.toString();
//        } else {
//            extracted = msg.toString();
//        }
//        return extracted;
//    }
//
//    private void createSQSConsumer() throws JMSException {
//        AWSCredentialsProviderChain chain = buildCredentialsProviderChain();
//
//        consumer = createConsumer(chain);
//
//        log.debug("Opened SQS topic {} for reading", this.queue);
//    }
//
//    private MessageConsumer createConsumer(AWSCredentialsProviderChain chain) throws JMSException {
//        SQSConnectionFactory connectionFactory =
//                SQSConnectionFactory.builder()
//                        .withRegion(Region.getRegion(Regions.EU_WEST_1))
//                        .withAWSCredentialsProvider(chain)
//                        .build();
//
//        // Create the connection.
//        Connection connection = connectionFactory.createConnection();
//
//        // Create the non-transacted session with AUTO_ACKNOWLEDGE mode
//        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//
//        Queue queue = session.createQueue(this.queue);
//        MessageConsumer consumer = session.createConsumer(queue);
//        connection.start();
//        return consumer;
//    }
//
//    private AWSCredentialsProviderChain buildCredentialsProviderChain() {
//        AWSCredentialsProviderChain chain;
//
//        if (awsKey.isPresent() && awsSecret.isPresent()) {
//            String key = awsKey.get();
//            String secret = awsSecret.get();
//            BasicAWSCredentials credentials = new BasicAWSCredentials(key, secret);
//
//            chain = new AWSCredentialsProviderChain(
//                    new StaticCredentialsProvider(credentials),
//                    new DefaultAWSCredentialsProviderChain()
//            );
//        } else {
//            chain = new DefaultAWSCredentialsProviderChain();
//        }
//        return chain;
//    }
//
//    @Override
//    public void stop() {
//        log.trace("Stopping");
//        synchronized (this) {
//            try {
//                if (consumer != null) {
//                    consumer.close();
//                    log.trace("Closed input stream");
//                }
//            } catch (JMSException e) {
//                log.error("Failed to close consumer stream: ", e);
//            }
//            this.notify();
//        }
//    }
//
//    private Map<String, String> offsetKey(String filename) {
//        return Collections.singletonMap(SQS_QUEUE_FIELD, filename);
//    }
//
//    private Map<String, String> offsetValue(String pos) {
//        return Collections.singletonMap(POSITION_FIELD, pos);
//    }
//}
