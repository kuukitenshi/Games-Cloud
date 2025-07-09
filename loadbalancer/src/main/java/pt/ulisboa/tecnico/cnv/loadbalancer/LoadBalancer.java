package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.io.IOException;
import java.util.Optional;

import com.sun.net.httpserver.HttpExchange;

import pt.ulisboa.tecnico.cnv.loadbalancer.logging.ILogger;
import pt.ulisboa.tecnico.cnv.loadbalancer.logging.LoggerFactory;
import pt.ulisboa.tecnico.cnv.loadbalancer.vm.VMInstance;
import pt.ulisboa.tecnico.cnv.loadbalancer.vm.VMStatus;
import pt.ulisboa.tecnico.cnv.loadbalancer.vm.VMWaiter;
import pt.ulisboa.tecnico.cnv.loadbalancer.workers.AssignedWorker;
import pt.ulisboa.tecnico.cnv.loadbalancer.workers.LambdaWorker;
import pt.ulisboa.tecnico.cnv.loadbalancer.workers.VMWorker;

public class LoadBalancer {

    private static final ILogger LOGGER = LoggerFactory.getLogger(LoadBalancer.class);

    private final InstanceManager instanceManager;
    private final EstimationEngine estimationEngine;
    private final Configuration configuration;

    public LoadBalancer(InstanceManager instanceManager, EstimationEngine estimationEngine,
            Configuration configuration) {
        this.instanceManager = instanceManager;
        this.estimationEngine = estimationEngine;
        this.configuration = configuration;
    }

    public AssignedWorker handleRequest(HttpExchange he, boolean allowLambda)
            throws IOException, InterruptedException, IllegalArgumentException {
        long complexity = this.estimationEngine.estimateComplexity(he);
        LOGGER.debug(String.format("Estimated complexity for request '%s': %s", he.getRequestURI(), complexity));
        return assingWork(he, complexity, allowLambda);
    }

    private AssignedWorker assingWork(HttpExchange he, long complexity, boolean allowLambda)
            throws InterruptedException {
        VMWaiter waiter = null;
        synchronized (this.instanceManager) {
            Optional<VMInstance> startingVM = this.instanceManager.availableVMs().stream()
                    .filter(vm -> vm.getStatus() == VMStatus.STARTING)
                    .findFirst();
            if (startingVM.isPresent() &&
                    complexity < this.configuration.lambdaThreshold() && allowLambda) {
                LOGGER.debug("vm is starting and small complexity, sending to lambda.");
                return new LambdaWorker();
            }
            Optional<VMInstance> optVm = this.instanceManager.availableVMs().stream()
                    .filter(vm -> vm.getStatus() == VMStatus.RUNNING)
                    .filter(vm -> vm.getCurrentWork() < this.configuration.maxVmWork())
                    .sorted()
                    .findFirst();
            if (optVm.isPresent()) {
                VMInstance vm = optVm.get();
                LOGGER.debug("found available vm instance: %s", vm.instanceId());
                vm.addWork(complexity);
                return new VMWorker(instanceManager, vm, complexity);
            }
            if (complexity < this.configuration.lambdaThreshold() && allowLambda) {
                LOGGER.debug("no vm available, request complexity is low sending to lambda.");
                return new LambdaWorker();
            }
            Optional<VMInstance> optStarting = this.instanceManager.availableVMs().stream()
                    .filter(vm -> vm.getStatus() == VMStatus.STARTING).findAny();
            if (optStarting.isEmpty()) {
                return null;
            }
            waiter = optStarting.get().waiter();
        }
        LOGGER.debug("waiting for VM to come online");
        waiter.waitUntilRunning();
        return assingWork(he, complexity, allowLambda);
    }

}
