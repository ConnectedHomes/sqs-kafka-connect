#!/usr/bin/env bash

source $(dirname $0)/env.sh

set -e

docker build -t bgchops-docker-dockerv2-local.artifactoryonline.com/bgch-dp/$IMAGE_NAME $(dirname $0)

docker-compose up -d
