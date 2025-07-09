package pt.ulisboa.tecnico.cnv.loadbalancer.logging;

public final class LoggerFactory {

    public static ILogger getLogger(String name) {
        return new PrettyLogger(name);
    }

    public static ILogger getLogger(Class<?> clazz) {
        return getLogger(clazz.getSimpleName());
    }

}
