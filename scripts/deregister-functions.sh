#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
source $SCRIPT_DIR/config.sh

echo "deleting capturetheflag"
aws lambda delete-function --function-name capturetheflag

echo "deleting gameoflife"
aws lambda delete-function --function-name gameoflife

echo "deleting fifteenpuzzle"
aws lambda delete-function --function-name fifteenpuzzle

echo "Dettaching role policy"
aws iam detach-role-policy \
	--role-name lambda-role \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

echo "Deleting lambda-role"
aws iam delete-role --role-name lambda-role
