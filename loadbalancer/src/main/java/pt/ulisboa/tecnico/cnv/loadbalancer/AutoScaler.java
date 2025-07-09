package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pt.ulisboa.tecnico.cnv.loadbalancer.logging.ILogger;
import pt.ulisboa.tecnico.cnv.loadbalancer.logging.LoggerFactory;
import pt.ulisboa.tecnico.cnv.loadbalancer.shutdown.ShutdownHookListener;
import pt.ulisboa.tecnico.cnv.loadbalancer.vm.VMInstance;
import pt.ulisboa.tecnico.cnv.loadbalancer.vm.VMStatus;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsResponse;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.StateValue;

public class AutoScaler implements Runnable, ShutdownHookListener {

    private static final ILogger LOGGER = LoggerFactory.getLogger(AutoScaler.class);
    private static final long TASK_UPDATE_RATE_SECONDS = 15L;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final CloudWatchClient cloudWatch = CloudWatchClient.create();
    private final InstanceManager instanceManager;
    private final Configuration configuration;
    private Instant lastScaleDown = Instant.MIN;
    private Instant lastScaleUp = Instant.MIN;
    private MetricAlarm highCpuAlarm = null;
    private MetricAlarm lowCpuAlarm = null;

    public AutoScaler(InstanceManager instanceManager, Configuration configuration) {
        this.instanceManager = instanceManager;
        this.configuration = configuration;
    }

    public void start() {
        this.scheduler.scheduleAtFixedRate(this, TASK_UPDATE_RATE_SECONDS, TASK_UPDATE_RATE_SECONDS, TimeUnit.SECONDS);
        LOGGER.info("service started!");
    }

    @Override
    public void onShutdownRequest() {
        this.scheduler.shutdown();
        LOGGER.debug("shutdown complete!");
    }

    @Override
    public void run() {
        retrieveAlarms();
        synchronized (this.instanceManager) {
            if (checkScaleUp()) {
                LOGGER.warn("starting to scale up!");
                scaleUp();
            } else if (checkScaleDown()) {
                LOGGER.warn("starting to scale down!");
                scaleDown();
            }
            terminateMarkedVMs();
        }
    }

    public boolean checkScaleUp() {
        Optional<VMInstance> starting = this.instanceManager.availableVMs().stream()
                .filter(vm -> vm.getStatus() == VMStatus.STARTING)
                .findAny();
        List<VMInstance> running = this.instanceManager.availableVMs().stream()
                .filter(vm -> vm.getStatus() == VMStatus.RUNNING)
                .toList();
        long totalWork = running.stream()
                .map(vm -> vm.getCurrentWork())
                .reduce(0L, (l1, l2) -> l1 + l2);
        boolean inAlarm = this.highCpuAlarm.stateValue() == StateValue.ALARM
                && (this.highCpuAlarm.stateUpdatedTimestamp().isAfter(this.lastScaleUp)
                        || Instant.now().minusSeconds(300L).isAfter(this.lastScaleUp));

        return starting.isEmpty() && (inAlarm || totalWork >= this.configuration.maxVmWork() * running.size());
    }

    public boolean checkScaleDown() {
        List<VMInstance> terminating = this.instanceManager.availableVMs().stream()
                .filter(vm -> vm.getStatus() == VMStatus.MARKED_FOR_TERMINATION
                        || vm.getStatus() == VMStatus.TERMINATING)
                .toList();
        List<VMInstance> running = this.instanceManager.availableVMs().stream()
                .filter(vm -> vm.getStatus() == VMStatus.RUNNING)
                .toList();
        boolean inAlarm = this.lowCpuAlarm.stateValue() == StateValue.ALARM
                && (this.lowCpuAlarm.stateUpdatedTimestamp().isAfter(this.lastScaleDown)
                        || Instant.now().minusSeconds(300L).isAfter(this.lastScaleDown));

        return running.size() > 1 && terminating.isEmpty() && inAlarm;
    }

    private void retrieveAlarms() {
        DescribeAlarmsRequest request = DescribeAlarmsRequest.builder().build();
        DescribeAlarmsResponse response = this.cloudWatch.describeAlarms(request);
        response.metricAlarms().forEach(alarm -> {
            if (alarm.alarmName().equals("HighCPUUtilization")) {
                this.highCpuAlarm = alarm;
            } else if (alarm.alarmName().equals("LowCPUUtilization")) {
                this.lowCpuAlarm = alarm;
            }
        });
    }

    private void scaleUp() {
        LOGGER.warn("System overutilized: scaling up");
        Optional<VMInstance> toDelete = this.instanceManager.availableVMs().stream()
                .filter(m -> m.getStatus() == VMStatus.MARKED_FOR_TERMINATION).sorted().findFirst();
        if (toDelete.isPresent()) {
            toDelete.get().setStatus(VMStatus.RUNNING);
        } else {
            this.instanceManager.launchVM();
        }
        this.lastScaleUp = Instant.now();
    }

    private void scaleDown() {
        LOGGER.warn("System underutilized: scaling down");
        VMInstance leastWorkVM = this.instanceManager.availableVMs().stream().sorted().findFirst().get();
        leastWorkVM.setStatus(VMStatus.MARKED_FOR_TERMINATION);
        this.lastScaleDown = Instant.now();
    }

    private void terminateMarkedVMs() {
        List<VMInstance> toTerminate = this.instanceManager.availableVMs().stream()
                .filter(vm -> vm.getStatus() == VMStatus.MARKED_FOR_TERMINATION && vm.getCurrentWork() == 0)
                .toList();
        toTerminate.forEach(this.instanceManager::terminateVM);
    }
}
