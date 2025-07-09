#!/usr/bin/env bash

SCRIPTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
source $SCRIPTS_DIR/config.sh

echo "Creating cloudwatch alarms"
$SCRIPTS_DIR/create-alarms.sh

echo "Creating lambda functions"
$SCRIPTS_DIR/create-functions.sh

# Run loadbalancer instance
aws ec2 run-instances \
	--image-id $(cat lb.image.id) \
	--instance-type t2.micro \
	--key-name $AWS_KEYPAIR_NAME \
	--security-group-ids $AWS_SECURITY_GROUP \
	--monitoring Enabled=false | jq -r ".Instances[0].InstanceId" > lb.instance.id

# Wait for instance to be running.
aws ec2 wait instance-running --instance-ids $(cat lb.instance.id)

# Extract DNS nane.
aws ec2 describe-instances \
	--instance-ids $(cat lb.instance.id) | jq -r ".Reservations[0].Instances[0].NetworkInterfaces[0].PrivateIpAddresses[0].Association.PublicDnsName" > lb.instance.dns

# Wait for instance to have SSH ready.
while ! nc -z $(cat lb.instance.dns) 8000; do
	echo "Waiting for $(cat lb.instance.dns):8000..."
	sleep 2
done

echo "Loadbalancer is READY!"
