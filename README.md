## Games@Cloud

### Developed by:
**Group 21**
- Xiting Wang - 112191
- Laura Cunha - 112269
- Rodrigo Correia - 112270
----

### Subprojects

This project contains some sub-projects:

1. `capturetheflag` - the Capture the Flag workload
2. `fifteenpuzzle` - the 15-Puzzle Solver workload
3. `gameoflife` - the Conway's Game of Life workload
4. `webserver` - the web server exposing the functionality of the workloads
5. `mss`- classes to store measures of the instrumentalization into DynamoDB
6. `loadbalancer`- the loadbalancer that according to the measures from MSS will create or delete AWS instances, with the heals of an AutoScalling, and manage the worloads according to the fullness of the machines.
----

### How to build?

1. Make sure your `JAVA_HOME` environment variable is set to Java 17 distribution
2. Run `mvn clean package`
3. Create in the project's root an `.env`file with the credentials of AWS as the provided example. This file will sourced from the scripts to access resources in your AWS account
4. On the root of the project run (if some of the scripts do not run execute `chmod +x <script-path>`)
- Run to create the AMI (Amazon Image) with the webserver.
```bash
./scripts/create-ami.sh
```
- Run to create the AMI (Amazon Image) with the loadbalancer.
```bash
./scripts/create-lb-ami.sh
```
- Run to launch the loadbalancer instance.
```bash
./scripts/launch-deployment.sh
```
5. After the usage run to terminate all instances, clean the DynamoDB tables and delete alarms.
```bash
./scripts/terminate-deployment.sh
```
6. Run to deregister the webserver AMI.
```bash
./scripts/deregister-ami.sh
``` 
- Run to deregister the load AMI.
```bash
./scripts/deregister-lb-ami.sh
``` 
---

### Architecture

#### Metrics
- Main metric: **executed instructions** 
- Effectively estimates request complexity with minimal overhead. 
- In **ThreadedICount** into the `javassist` project there is a **shared map** to store instruction counts per thread ID.

#### Instrumentation
- Done by adding a **method call to increase the number of instructions** in every basic block found. 
- Instrumentation code only injected in the game **handlers packages,** to only collect metrics about the game specific code. 
- Functiona to **start** and **finish** the instrumentation of a request to make each request unique and only count the instructions used to perform that single request.

#### AMI
- Builds an Amazon Machine Image (AMI) that starts the **WebServer** automatically when the VM boots up. There's also an AMI for the **Load balancer**.

#### MSS
- Creates three tables using the **DynamoDB API**.
- Stores metrics for each different game into the Dynamo.
- Columns contains the parameters used in each game and the resulting number of instructions calculated with the instrumentation tool.

#### Load Balancer / Auto Scaler
- The metric uses a KNN algorithm with 5 neighbors.
- It has a complexity counter that increments whenever a request is sent, helping determine when a machine becomes overloaded and deciding to which machine the request should be routed.
- It estimates the complexity of a new request based on past requests stored in the MSS (DynamoDB).
- The estimated complexity is used to decide which machine the request should be forwarded to.
- The auto-scaler has CPU utilization alarms set at a high threshold of 80% and a low threshold of 20%, and it creates or destroys machines accordingly.
- The load balancer performs health checks on worker machines to detect faults and resend requests to other machines when necessary.
- It prioritizes fulfilling requests on existing machines instead of sending them to Lambdas. 
- Lambdas are only chosen when a small request arrives, all machines are at full capacity, and a new machine is being started but is not yet running.

 