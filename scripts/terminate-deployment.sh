#!/usr/bin/env bash

SCRIPTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
source $SCRIPTS_DIR/config.sh

echo "terminating all instances"
$SCRIPTS_DIR/terminate-all.sh

echo "deleting cloudwatch alarms"
$SCRIPTS_DIR/deregister-alarms.sh

echo "deleting lambda functions"
$SCRIPTS_DIR/deregister-functions.sh

echo "Deleting dynamo tables"
aws dynamodb delete-table \
	--table-name CaptureTheFlag
aws dynamodb delete-table \
	--table-name GameOfLife
aws dynamodb delete-table \
	--table-name FifteenPuzzle
