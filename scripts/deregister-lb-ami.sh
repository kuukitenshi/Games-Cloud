#!/usr/bin/env bash

source scripts/config.sh

aws ec2 deregister-image --image-id $(cat lb.image.id)
