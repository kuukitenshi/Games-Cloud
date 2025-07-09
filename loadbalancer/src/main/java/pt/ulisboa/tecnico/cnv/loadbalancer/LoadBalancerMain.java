package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.sun.net.httpserver.HttpServer;

import pt.ulisboa.tecnico.cnv.loadbalancer.handlers.HealthCheckHandler;
import pt.ulisboa.tecnico.cnv.loadbalancer.handlers.RequestHandler;
import pt.ulisboa.tecnico.cnv.loadbalancer.logging.ILogger;
import pt.ulisboa.tecnico.cnv.loadbalancer.logging.LoggerFactory;
import pt.ulisboa.tecnico.cnv.loadbalancer.shutdown.ShutdownHook;
import pt.ulisboa.tecnico.cnv.mss.MSSClient;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class LoadBalancerMain {

    private static final ILogger LOGGER = LoggerFactory.getLogger(LoadBalancerMain.class);
    private static final int DEFAULT_PORT = 8000;

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        Configuration configuration = Configuration.defaultConfiguration();
        MSSClient mssClient = new MSSClient();
        EstimationEngine estimationEngine = new EstimationEngine();
        ShutdownHook shutdownHook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        InstanceManager instanceManager = new InstanceManager();
        shutdownHook.push(instanceManager);

        MetricsCache metricsCache = new MetricsCache(mssClient);
        metricsCache.onMetricsUpdate(() -> estimationEngine.updateEstimationData(metricsCache));
        shutdownHook.push(metricsCache);
        metricsCache.start();

        LoadBalancer loadBalancer = new LoadBalancer(instanceManager, estimationEngine, configuration);
        AutoScaler autoScaler = new AutoScaler(instanceManager, configuration);
        shutdownHook.push(autoScaler);
        autoScaler.start();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", new RequestHandler(loadBalancer));
        server.createContext("/health", new HealthCheckHandler());
        server.start();
        shutdownHook.push(() -> {
            ILogger logger = LoggerFactory.getLogger(HttpServer.class);
            server.stop(1);
            logger.debug("shutdown complete!");
        });
        LOGGER.info("Load balancer running on port %s", port);
        instanceManager.start();
    }
}
