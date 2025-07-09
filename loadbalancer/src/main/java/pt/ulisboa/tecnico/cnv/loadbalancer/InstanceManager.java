package pt.ulisboa.tecnico.cnv.loadbalancer;

import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ec2.waiters.Ec2Waiter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pt.ulisboa.tecnico.cnv.loadbalancer.logging.ILogger;
import pt.ulisboa.tecnico.cnv.loadbalancer.logging.LoggerFactory;
import pt.ulisboa.tecnico.cnv.loadbalancer.shutdown.ShutdownHookListener;
import pt.ulisboa.tecnico.cnv.loadbalancer.vm.VMInstance;
import pt.ulisboa.tecnico.cnv.loadbalancer.vm.VMStatus;
import pt.ulisboa.tecnico.cnv.loadbalancer.vm.VMWaiter;

public class InstanceManager implements Runnable, ShutdownHookListener {

    private static final ILogger LOGGER = LoggerFactory.getLogger(InstanceManager.class);
    private static final InstanceType VMS_TYPE = InstanceType.T2_MICRO;
    private static final String IMAGE_ID = System.getenv("AWS_IMAGE_ID");
    private static final String SECURITY_GROUP = System.getenv("AWS_SECURITY_GROUP");
    private static final String KEY_PAIR_NAME = System.getenv("AWS_KEYPAIR_NAME");
    private static final long TASK_UPDATE_RATE_SECONDS = 30L;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Ec2Client ec2 = Ec2Client.create();
    private final Ec2Waiter waiter = ec2.waiter();
    private final List<VMInstance> availableVMs = new ArrayList<>();

    public void start() {
        List<Instance> activeInstances = findAlreadyRunningInstances();
        if (!activeInstances.isEmpty()) {
            LOGGER.debug("found %s active web server instances, using them", activeInstances.size());
            connectToRunningInstances(activeInstances);
            return;
        }
        LOGGER.debug("Launching initial VM...");
        Optional<VMInstance> optInstance = launchVM();
        if (optInstance.isEmpty()) {
            LOGGER.error("Failed to launch initial VM instance.");
            throw new RuntimeException("Cannot launch starting instance");
        }
        VMInstance vm = optInstance.get();
        LOGGER.debug("initial VM created with id %s, waiting until running", vm.instanceId());
        VMWaiter waiter = vm.waiter();
        try {
            waiter.waitUntilRunning();
        } catch (InterruptedException e) {
            LOGGER.error("failed to wait until vm running");
            e.printStackTrace();
        }
        LOGGER.debug("initial VM running!");
        LOGGER.info("service started!");
        scheduler.scheduleAtFixedRate(this, TASK_UPDATE_RATE_SECONDS, TASK_UPDATE_RATE_SECONDS, TimeUnit.SECONDS);
    }

    public synchronized Optional<VMInstance> launchVM() {
        RunInstancesMonitoringEnabled monitoring = RunInstancesMonitoringEnabled.builder().enabled(true).build();
        RunInstancesRequest request = RunInstancesRequest.builder()
                .imageId(IMAGE_ID)
                .instanceType(VMS_TYPE)
                .keyName(KEY_PAIR_NAME)
                .securityGroupIds(SECURITY_GROUP)
                .monitoring(monitoring)
                .minCount(1)
                .maxCount(1)
                .build();
        RunInstancesResponse response = this.ec2.runInstances(request);
        Optional<String> optId = response.instances().stream().map(i -> i.instanceId()).findFirst();
        if (optId.isEmpty()) {
            return Optional.empty();
        }
        String instanceId = optId.get();
        VMInstance vm = new VMInstance(instanceId);
        this.availableVMs.add(vm);
        CompletableFuture.runAsync(() -> waitForInstanceRunning(vm));
        return Optional.of(vm);
    }

    public synchronized void terminateVM(VMInstance vm) {
        TerminateInstancesRequest terminateRequest = TerminateInstancesRequest.builder()
                .instanceIds(vm.instanceId())
                .build();
        ec2.terminateInstances(terminateRequest);
        vm.setStatus(VMStatus.TERMINATING);
        LOGGER.debug("terminating vm: %s", vm.instanceId());
        CompletableFuture.runAsync(() -> {
            DescribeInstancesRequest describeRequest = DescribeInstancesRequest.builder()
                    .instanceIds(vm.instanceId())
                    .build();
            waiter.waitUntilInstanceTerminated(describeRequest);
            vm.setStatus(VMStatus.TERMINATED);
            synchronized (this) {
                this.availableVMs.remove(vm);
            }
        });
    }

    public synchronized List<VMInstance> availableVMs() {
        return Collections.unmodifiableList(this.availableVMs);
    }

    @Override
    public void onShutdownRequest() {
        scheduler.shutdown();
        synchronized (this) {
            LOGGER.debug("shutdown request received, requesting termination of %s VMs", this.availableVMs.size());
            List<String> instanceIds = this.availableVMs.stream().map(vm -> vm.instanceId()).toList();
            TerminateInstancesRequest terminateRequest = TerminateInstancesRequest.builder()
                    .instanceIds(instanceIds)
                    .build();
            ec2.terminateInstances(terminateRequest);
            LOGGER.debug("termination request sent, waiting for all instances to terminate...");
            DescribeInstancesRequest describeRequest = DescribeInstancesRequest.builder()
                    .instanceIds(instanceIds)
                    .build();
            waiter.waitUntilInstanceTerminated(describeRequest);
            LOGGER.debug("all instances terminated!");
        }
    }

    private boolean waitForInstanceRunning(VMInstance vm) {
        DescribeInstancesRequest describeRequest = DescribeInstancesRequest.builder()
                .instanceIds(vm.instanceId())
                .build();
        WaiterResponse<DescribeInstancesResponse> wr = waiter.waitUntilInstanceRunning(describeRequest);
        Optional<Instance> optionalInstance = wr.matched().response()
                .flatMap(r -> r.reservations().stream().findFirst())
                .flatMap(r -> r.instances().stream().findFirst());
        if (optionalInstance.isPresent()) {
            Instance ec2Instance = optionalInstance.get();
            vm.setEc2Instance(ec2Instance);
            return waitForHealthCheck(vm);
        }
        return false;
    }

    private boolean waitForHealthCheck(VMInstance vm) {
        if (checkHealth(vm)) {
            LOGGER.debug("Instance " + vm.instanceId() + " passed health check.");
            vm.setStatus(VMStatus.RUNNING);
            return true;
        }
        LOGGER.warn("Instance " + vm.instanceId() + " never became healthy.");
        return false;
    }

    private boolean checkHealth(VMInstance vm) {
        String ip = vm.getEc2Instance().publicIpAddress();
        HttpClient client = HttpClient.newHttpClient();
        URI uri = URI.create(String.format("http://%s:%d/", ip, 8000));
        int retries = 10;
        while (retries-- > 0) {
            try {
                HttpRequest request = HttpRequest.newBuilder().GET().uri(uri).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return true;
                }
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(3000); // waits 1s
            } catch (InterruptedException e) {
                break;
            }
        }
        return false;
    }

    private List<Instance> findAlreadyRunningInstances() {
        Filter imageIdFilter = Filter.builder().name("image-id").values(IMAGE_ID).build();
        Filter instnaceStateFilter = Filter.builder().name("instance-state-name").values("pending", "running").build();
        DescribeInstancesRequest describeRequest = DescribeInstancesRequest.builder()
                .filters(imageIdFilter, instnaceStateFilter)
                .build();
        DescribeInstancesResponse response = this.ec2.describeInstances(describeRequest);
        List<Instance> instances = response.reservations().stream()
                .flatMap(r -> r.instances().stream())
                .toList();
        return instances;
    }

    private void connectToRunningInstances(List<Instance> instances) {
        List<VMInstance> vms = instances.stream().map(instance -> {
            VMInstance vm = new VMInstance(instance.instanceId());
            vm.setEc2Instance(instance);
            return vm;
        }).toList();
        this.availableVMs.addAll(vms);

        List<String> instanceIds = instances.stream().map(instance -> instance.instanceId()).toList();
        DescribeInstancesRequest describeRequest = DescribeInstancesRequest.builder()
                .instanceIds(instanceIds)
                .build();
        waiter.waitUntilInstanceRunning(describeRequest);
        List<CompletableFuture<Void>> futures = vms.stream()
                .map(vm -> CompletableFuture.runAsync(() -> waitForHealthCheck(vm)))
                .toList();
        futures.forEach(future -> future.join());
    }

    @Override
    public void run() {
        this.availableVMs.stream().filter(vm -> vm.getStatus() == VMStatus.RUNNING).forEach(vm -> {
            if (!checkHealth(vm)) {
                synchronized (this) {
                    vm.setStatus(VMStatus.TERMINATING);
                    terminateVM(vm);
                }
            }
        });
    }

}
