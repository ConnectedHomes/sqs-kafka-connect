#!/usr/bin/env bash
CONNECT_HOST=http://localhost:8083

curl -s -X GET "$CONNECT_HOST/connectors" | jq
curl -s -X GET "$CONNECT_HOST/connector-plugins" | jq
curl -s -X POST -H "Content-Type: application/json" -d '{
    "name": "aws-sqs-source",
    "config": {
        "connector.class": "com.hivehome.kafka.connect.sqs.SQSStreamSourceConnector",
        "tasks.max": "2",
        "destination.topic": "connect-test",
        "source.queue": "test-sqs",
        "aws.region": "eu-west-1"
    }
}' "$CONNECT_HOST/connectors"

curl -s -X GET -H "Content-Type: application/json" "$CONNECT_HOST/connectors/aws-sqs-source/status" | jq