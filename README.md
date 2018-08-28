# sqs-kafka-connect
------------------

Kafka Connect plugin to read from Amazon SQS as a source to ingest data to Kafka.

## Running the connector
For running the connector with Kafka Connect the assembled jar should be available in the CLASSPATH. 

### Running in standalone mode
For running the Kafka Connect worker in standalone, from the CONFLUENT_HOME run 

`./bin/connect-standalone ./etc/schema-registry/connect-avro-standalone.properties connect-sqs-source.properties`

Note: Zookeeper, Kafka, Schema Registry services should be running and available.

#### Configuration file
Contents of a sample `connect-sqs-source.properties` are as follows

    name=aws-sqs-source
    connector.class=com.hivehome.kafka.connect.sqs.SQSSourceConnector
    tasks.max=1
    
    source.queue=source-sqs-queue
    destination.topic=destination-kafka-topic
    
    aws.region=eu-west-1
    aws.key=ABC
    aws.secret=DEF
    
### Running in distributed mode
For running the Kafka Connect worker in distributed, from the CONFLUENT_HOME run 

`./bin/connect-distributed ./etc/schema-registry/connect-avro-distributed.properties`

Ensure that the SQS connector jar is exported in the classpath so that Kafka Connect loads it.
To verify the connector has been discovered correctly run 

`curl http://<CONNECT_HOST>:8083/connector-plugins`

and it should list the `com.hivehome.kafka.connect.sqs.SQSSourceConnector` connector in the list

#### Configuration via REST endpoint
Create a connector configuration using the following command

    curl -X POST -H "Content-Type: application/json" -d '{
         "name": "aws-sqs-source",
         "config": {
             "connector.class": "com.hivehome.kafka.connect.sqs.SQSSourceConnector",
             "tasks.max": "10",
             "destination.topic": "kafka-destination",
             "source.queue": "sqs-source",
             "aws.region": "eu-west-1"
         }
     }' "http://<CONNECT_HOST>:8083/connectors"

## Unsupported features
 - Pause and Resume operations are not supported by the connector

## Developing the connector

### Building and running tests
Run `sbt clean test it:test` to run the unit and integration tests. The integration tests require AWS key and secret as 
environment variables or system properties to run. The keys should have access to create, delete, send and receive 
messages.

### Packaging
Use the command `sbt assembly` to package up the fat jar with dependencies which can be used as a Kafka Connect plugin.
