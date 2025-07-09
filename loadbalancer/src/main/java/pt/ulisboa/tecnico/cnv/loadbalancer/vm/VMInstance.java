package pt.ulisboa.tecnico.cnv.loadbalancer.vm;

import software.amazon.awssdk.services.ec2.model.Instance;

public class VMInstance implements Comparable<VMInstance> {

    public static final long MAX_CAPACITY = 25000L;

    private final VMWaiter waiter = new VMWaiter(this);
    private final String instanceId;

    private VMStatus status = VMStatus.STARTING;
    private Instance instance = null;
    private long currentWork = 0;

    public VMInstance(String instanceId) {
        this.instanceId = instanceId;
    }

    public VMStatus getStatus() {
        return this.status;
    }

    public synchronized void setStatus(VMStatus status) {
        this.status = status;
        notifyAll();
    }

    public Instance getEc2Instance() {
        return this.instance;
    }

    public void setEc2Instance(Instance instance) {
        this.instance = instance;
    }

    public long getCurrentWork() {
        return this.currentWork;
    }

    public void addWork(long work) {
        this.currentWork += work;
    }

    public void removeWork(long work) {
        this.currentWork -= work;
    }

    public boolean hasCapacityFor(long work) {
        return this.currentWork + work <= MAX_CAPACITY;
    }

    public String instanceId() {
        return this.instanceId;
    }

    public VMWaiter waiter() {
        return this.waiter;
    }

    @Override
    public int compareTo(VMInstance other) {
        return Long.compare(this.currentWork, other.currentWork);
    }

}
