#!/usr/bin/env bash

source scripts/config.sh

# Install java.
cmd="sudo yum update -y; sudo yum install java-17-amazon-corretto-headless.x86_64 -y;"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

# Install web server.
webserver_path="webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar"
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH "$webserver_path" ec2-user@$(cat instance.dns):webserver.jar

# Setup web server to start on instance launch.
tool=ThreadedICount
packages=pt.ulisboa.tecnico.cnv.capturetheflag,pt.ulisboa.tecnico.cnv.fifteenpuzzle,pt.ulisboa.tecnico.cnv.gameoflife
jar=/home/ec2-user/webserver.jar
main=pt.ulisboa.tecnico.cnv.webserver.WebServer
run_cmd="AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY AWS_REGION=$AWS_DEFAULT_REGION java -cp $jar -javaagent:$jar=$tool:$packages:output $main"

cmd="echo \"$run_cmd\" | sudo tee -a /etc/rc.local; sudo chmod +x /etc/rc.local"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd
