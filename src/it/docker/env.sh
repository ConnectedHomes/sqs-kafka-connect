#!/usr/bin/env bash

DIR=$(dirname $_)

# If we are running in GoCD, then compute the DOCKER_PROJECT
if [[ ! -z "$GO_PIPELINE_NAME" ]]; then
    export DOCKER_PROJECT="$(echo $GO_PIPELINE_NAME | tr -cd [^[:alnum:]])${GO_PIPELINE_COUNTER}"
    echo "Running in GoCD. DOCKER_PROJECT=$DOCKER_PROJECT"
fi

if [[ -z "$DOCKER_PROJECT" ]]; then
    echo "DOCKER_PROJECT not set. Defaulting to 'it'."
    export DOCKER_PROJECT=it
fi

export IMAGE_NAME=dp-heating-period-sample-job
export COMPOSE_PROJECT_NAME=$DOCKER_PROJECT
export COMPOSE_FILE=$DIR/docker-compose.yml
export CHECKPOINT_DIR=/var/spark/checkpoints

if [ "$(uname)" == "Darwin" ]; then
    export HOST_IP=$(route -n get default|grep interface|awk '{print $2}'|xargs ipconfig getifaddr)
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    export HOST_IP=$(/sbin/ip route|awk '/default/ { print $3 }')
else
    echo "CANNOT DETERMINE OS WHEN DETECTING IP ADDRESS"
    exit 1
fi

