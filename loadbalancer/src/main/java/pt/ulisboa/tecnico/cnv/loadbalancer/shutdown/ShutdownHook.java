package pt.ulisboa.tecnico.cnv.loadbalancer.shutdown;

import java.util.ArrayDeque;
import java.util.Deque;

import pt.ulisboa.tecnico.cnv.loadbalancer.logging.ILogger;
import pt.ulisboa.tecnico.cnv.loadbalancer.logging.LoggerFactory;

public class ShutdownHook extends Thread {

    private static final ILogger LOGGER = LoggerFactory.getLogger(ShutdownHook.class);

    private final Deque<ShutdownHookListener> stack = new ArrayDeque<ShutdownHookListener>();

    public void push(ShutdownHookListener listener) {
        stack.push(listener);
    }

    @Override
    public void run() {
        LOGGER.debug("system shutdown started");
        while (!stack.isEmpty()) {
            ShutdownHookListener listener = stack.pop();
            try {
                listener.onShutdownRequest();
            } catch (Exception e) {
                LOGGER.error("%s threw an exception while shutting down: %s", listener.getClass().getSimpleName(),
                        e.getMessage());
            }
        }
        LOGGER.debug("system shutdown completed");
    }

}
