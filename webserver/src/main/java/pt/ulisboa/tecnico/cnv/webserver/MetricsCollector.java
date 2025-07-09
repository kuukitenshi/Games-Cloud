package pt.ulisboa.tecnico.cnv.webserver;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import pt.ulisboa.tecnico.cnv.mss.MSSClient;
import pt.ulisboa.tecnico.cnv.mss.MSSMetric;

public class MetricsCollector implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(MetricsCollector.class.getSimpleName());
    private static final long SLEEP_TIME = 10000L;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Queue<MSSMetric> collectedMetrics = new ArrayDeque<>();
    private final MSSClient mssClient;

    public MetricsCollector(MSSClient mssClient) {
        this.mssClient = mssClient;
    }

    public void start() {
        this.scheduler.scheduleAtFixedRate(this, 0L, SLEEP_TIME, TimeUnit.MILLISECONDS);
    }

    public synchronized void collectMetric(MSSMetric metric) {
        this.collectedMetrics.add(metric);
    }

    @Override
    public void run() {
        LOGGER.info("Started MetricsCollector");
        while (true) {
            synchronized (this) {
                LOGGER.info("Collecting metrics");
                while (!this.collectedMetrics.isEmpty()) {
                    MSSMetric metric = this.collectedMetrics.poll();
                    try {
                        metric.uploadMetric(this.mssClient);
                    } catch (Exception e) {
                        LOGGER.severe(() -> "Failed to upload metric");
                    }
                }
            }
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
