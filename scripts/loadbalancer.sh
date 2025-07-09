#!/usr/bin/env bash

source scripts/config.sh

AWS_REGION=$AWS_DEFAULT_REGION \
AWS_IMAGE_ID=$(cat image.id) \
java -jar loadbalancer/target/loadbalancer-1.0.0-SNAPSHOT-jar-with-dependencies.jar $1
