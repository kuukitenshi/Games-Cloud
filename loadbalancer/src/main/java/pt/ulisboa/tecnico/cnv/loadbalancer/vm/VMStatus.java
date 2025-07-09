package pt.ulisboa.tecnico.cnv.loadbalancer.vm;

public enum VMStatus {
    STARTING,
    RUNNING,
    MARKED_FOR_TERMINATION,
    TERMINATING,
    TERMINATED
}
