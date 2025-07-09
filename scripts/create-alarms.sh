#!/usr/bin/env bash

source scripts/config.sh

# Create CloudWatch Alarms
aws cloudwatch put-metric-alarm \
  --alarm-name HighCPUUtilization \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 60 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --dimension Name=ImageId,Value=$(cat image.id) \
  --evaluation-periods 2 \
  --unit Percent

aws cloudwatch put-metric-alarm \
  --alarm-name LowCPUUtilization \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 60 \
  --threshold 20 \
  --comparison-operator LessThanThreshold \
  --dimension Name=ImageId,Value=$(cat image.id) \
  --evaluation-periods 2 \
  --unit Percent
