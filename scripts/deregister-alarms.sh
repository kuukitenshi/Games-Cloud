#!/usr/bin/env bash

source scripts/config.sh

aws cloudwatch delete-alarms \
  --alarm-names HighCPUUtilization LowCPUUtilization
