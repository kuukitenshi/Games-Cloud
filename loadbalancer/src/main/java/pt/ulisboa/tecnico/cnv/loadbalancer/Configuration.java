package pt.ulisboa.tecnico.cnv.loadbalancer;

public record Configuration(long maxVmWork, long lambdaThreshold) {

    private static final long DEFAULT_MAX_VM_WORK = 150_000_000L;
    private static final long DEFAULT_LAMBDA_THRESHOLD = 5_000_000L;

    public static Configuration defaultConfiguration() {
        return new Configuration(DEFAULT_MAX_VM_WORK, DEFAULT_LAMBDA_THRESHOLD);
    }

}
