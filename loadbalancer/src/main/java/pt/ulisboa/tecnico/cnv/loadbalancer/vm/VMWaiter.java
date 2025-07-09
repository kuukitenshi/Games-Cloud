package pt.ulisboa.tecnico.cnv.loadbalancer.vm;

public class VMWaiter {

    private final VMInstance vm;

    public VMWaiter(VMInstance vm) {
        this.vm = vm;
    }

    public synchronized void waitUntilStatus(VMStatus status) throws InterruptedException {
        synchronized (vm) {
            while (this.vm.getStatus() != status) {
                vm.wait();
            }
        }
    }

    public void waitUntilRunning() throws InterruptedException {
        waitUntilStatus(VMStatus.RUNNING);
    }

}
