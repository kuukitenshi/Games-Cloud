#!/usr/bin/env bash

source scripts/config.sh

# Run new instance.
aws ec2 run-instances \
	--image-id resolve:ssm:/aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-x86_64-gp2 \
	--instance-type t2.micro \
	--key-name $AWS_KEYPAIR_NAME \
	--security-group-ids $AWS_SECURITY_GROUP \
	--monitoring Enabled=false | jq -r ".Instances[0].InstanceId" > lb.instance.id
echo "New instance with id $(cat lb.instance.id)."

# Wait for instance to be running.
aws ec2 wait instance-running --instance-ids $(cat lb.instance.id)
echo "New instance with id $(cat lb.instance.id) is now running."

# Extract DNS nane.
aws ec2 describe-instances \
	--instance-ids $(cat lb.instance.id) | jq -r ".Reservations[0].Instances[0].NetworkInterfaces[0].PrivateIpAddresses[0].Association.PublicDnsName" > lb.instance.dns
echo "New instance with id $(cat lb.instance.id) has address $(cat lb.instance.dns)."

# Wait for instance to have SSH ready.
while ! nc -z $(cat lb.instance.dns) 22; do
	echo "Waiting for $(cat lb.instance.dns):22 (SSH)..."
	sleep 0.5
done
echo "New instance with id $(cat lb.instance.id) is ready for SSH access."

# Install java.
cmd="sudo yum update -y; sudo yum install java-17-amazon-corretto-headless.x86_64 -y;"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat lb.instance.dns) $cmd

# Install web server.
loadbalancer_path="loadbalancer/target/loadbalancer-1.0.0-SNAPSHOT-jar-with-dependencies.jar"
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH "$loadbalancer_path" ec2-user@$(cat lb.instance.dns):loadbalancer.jar

# Setup web server to start on instance launch.
jar=/home/ec2-user/loadbalancer.jar
main=pt.ulisboa.tecnico.cnv.loadbalancer.LoadBalancerMain
run_cmd="AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY AWS_REGION=$AWS_DEFAULT_REGION AWS_SECURITY_GROUP=$AWS_SECURITY_GROUP AWS_IMAGE_ID=$(cat image.id) java -cp $jar $main 8000"

cmd="echo \"$run_cmd\" | sudo tee -a /etc/rc.local; sudo chmod +x /etc/rc.local"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat lb.instance.dns) $cmd

# Step 3: create VM image (AIM).
aws ec2 create-image --instance-id $(cat lb.instance.id) --name CNV-LB-Image | jq -r .ImageId > lb.image.id
echo "New VM image with id $(cat image.id)."

# Step 4: Wait for image to become available.
echo "Waiting for image to be ready... (this can take a couple of minutes)"
aws ec2 wait image-available --filters Name=name,Values=CNV--LB-Image
echo "Waiting for image to be ready... done! \o/"

# Step 5: terminate the vm instance.
aws ec2 terminate-instances --instance-ids $(cat lb.instance.id)
