/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.hivehome.kafka.connect.sns;

import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SNSStreamSourceTask extends SourceTask {
    private static final Logger log = LoggerFactory.getLogger(SNSStreamSourceTask.class);
    public static final String SNS_TOPIC_FIELD = "snsTopic";
    public static final String POSITION_FIELD = "position";
    private static final Schema VALUE_SCHEMA = Schema.STRING_SCHEMA;

    private String snsTopic;
    private String topic = null;
    private MessageConsumer consumer;

    @Override
    public String version() {
        return new SNSStreamSourceConnector().version();
    }

    @Override
    public void start(Map<String, String> props) {
        snsTopic = props.get(SNSStreamSourceConnector.SNS_TOPIC_CONFIG);
        if (snsTopic == null)
            throw new ConnectException("SNSStreamSourceTask config missing snsTopic setting");
        topic = props.get(SNSStreamSourceConnector.TOPIC_CONFIG);
        if (topic == null)
            throw new ConnectException("SNSStreamSourceTask config missing topic setting");
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        if (consumer == null) {
            try {
                // TODO: Put in keys
                String key = "???";
                String secret = "???";
                BasicAWSCredentials credentials = new BasicAWSCredentials(key, secret);

                SQSConnectionFactory connectionFactory =
                        SQSConnectionFactory.builder()
                                .withRegion(Region.getRegion(Regions.EU_WEST_1))
                                .withAWSCredentialsProvider(new StaticCredentialsProvider(credentials))
                                .build();

                // Create the connection.
                Connection connection = connectionFactory.createConnection();

                // Create the non-transacted session with AUTO_ACKNOWLEDGE mode
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                Queue queue = session.createQueue(snsTopic);
                consumer = session.createConsumer(queue);
                connection.start();

                log.debug("Opened SNS topic {} for reading", snsTopic);
            } catch (JMSException e) {
                log.error("JMSException", e);
                e.printStackTrace();
            }
        }

        try {
            ArrayList<SourceRecord> records = new ArrayList<>();
            Message msg = consumer.receive();
            log.info("Received message {}", msg);
            records.add(new SourceRecord(offsetKey(snsTopic), offsetValue(msg.getJMSMessageID()), topic, VALUE_SCHEMA, msg.toString()));
            return records;
        } catch (JMSException e) {
            // Underlying stream was killed, probably as a result of calling stop. Allow to return
            // null, and driving thread will handle any shutdown if necessary.
            log.error("JMSException", e);
        }
        return null;
    }

    @Override
    public void stop() {
        log.trace("Stopping");
        synchronized (this) {
            try {
                if (consumer != null) {
                    consumer.close();
                    log.trace("Closed input stream");
                }
            } catch (JMSException e) {
                log.error("Failed to close consumer stream: ", e);
            }
            this.notify();
        }
    }

    private Map<String, String> offsetKey(String filename) {
        return Collections.singletonMap(SNS_TOPIC_FIELD, filename);
    }

    private Map<String, String> offsetValue(String pos) {
        return Collections.singletonMap(POSITION_FIELD, pos);
    }
}
