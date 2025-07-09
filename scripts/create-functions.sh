#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
source $SCRIPT_DIR/config.sh

echo "Creating lambda-role"
aws iam create-role --role-name lambda-role --assume-role-policy-document '{"Version": "2012-10-17","Statement": [{ "Effect": "Allow", "Principal": {"Service": "lambda.amazonaws.com"}, "Action": "sts:AssumeRole"}]}'

sleep 5

echo "Attaching role policy"
aws iam attach-role-policy --role-name lambda-role --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

sleep 5

echo "Creating capturetheflag function"
aws lambda create-function \
	--function-name capturetheflag \
	--zip-file fileb://capturetheflag/target/capturetheflag-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
	--handler pt.ulisboa.tecnico.cnv.capturetheflag.CaptureTheFlagHandler \
	--runtime java17 \
	--timeout 5 \
	--memory-size 256 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role

echo "Creating gameoflife function"
aws lambda create-function \
	--function-name gameoflife \
	--zip-file fileb://gameoflife/target/gameoflife-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
	--handler pt.ulisboa.tecnico.cnv.gameoflife.GameOfLifeHandler \
	--runtime java17 \
	--timeout 5 \
	--memory-size 256 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role

echo "Creating fifteenpuzzle function"
aws lambda create-function \
	--function-name fifteenpuzzle \
	--zip-file fileb://fifteenpuzzle/target/fifteenpuzzle-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
	--handler pt.ulisboa.tecnico.cnv.fifteenpuzzle.FifteenPuzzleHandler \
	--runtime java17 \
	--timeout 5 \
	--memory-size 256 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role


