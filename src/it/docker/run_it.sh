#!/usr/bin/env bash

source $(dirname $0)/env.sh

set -e

CONTAINER_NAME=$IMAGE_NAME-it
KAFKA="kafka:9092"
SCHEMA_REGISTRY="http://schema_registry:8081"
ZK_CONNECT="zookeeper:2181"
CASSANDRA_HOST="cassandra"
CASSANDRA_PORT=9042

[[ -e build.sbt ]] || {
    echo "No build.sbt file in current directory. Please run from root of project."
    exit 1
}

docker run \
  --rm \
  --name $CONTAINER_NAME \
  --interactive \
  --volume "$HOME/.ivy2":/root/.ivy2 \
  --volume $PWD:/app \
  --net ${DOCKER_PROJECT}_default  \
  --env KAFKA=$KAFKA \
  --env SCHEMA_REGISTRY=$SCHEMA_REGISTRY \
  --env ZK_CONNECT=$ZK_CONNECT \
  --env CASSANDRA_HOST=$CASSANDRA_HOST \
  --env CASSANDRA_PORT=$CASSANDRA_PORT \
  bgchops-docker-dockerv2-local.artifactoryonline.com/bgch-dp/sbt \
  it:test
