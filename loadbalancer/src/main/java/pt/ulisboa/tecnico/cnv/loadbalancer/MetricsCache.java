package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pt.ulisboa.tecnico.cnv.loadbalancer.logging.ILogger;
import pt.ulisboa.tecnico.cnv.loadbalancer.logging.LoggerFactory;
import pt.ulisboa.tecnico.cnv.loadbalancer.shutdown.ShutdownHookListener;
import pt.ulisboa.tecnico.cnv.mss.CaptureTheFlagMetric;
import pt.ulisboa.tecnico.cnv.mss.FifteenPuzzleMetric;
import pt.ulisboa.tecnico.cnv.mss.GameOfLifeMetric;
import pt.ulisboa.tecnico.cnv.mss.MSSClient;

public class MetricsCache implements Runnable, ShutdownHookListener {

    private static final ILogger LOGGER = LoggerFactory.getLogger(MetricsCache.class);
    private static final long TASK_UPDATE_RATE_SECONDS = 10L;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final MSSClient mssClient;
    private List<CaptureTheFlagMetric> ctfMetrics = new ArrayList<>();
    private List<GameOfLifeMetric> golMetrics = new ArrayList<>();
    private List<FifteenPuzzleMetric> fpMetrics = new ArrayList<>();
    private Runnable onUpdate = null;

    public MetricsCache(MSSClient mssClient) {
        this.mssClient = mssClient;
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
        LOGGER.debug("fetching new metrics data from Dynamo");
        try {
            List<CaptureTheFlagMetric> ctfMetrics = new ArrayList<>();
            List<GameOfLifeMetric> golMetrics = new ArrayList<>();
            List<FifteenPuzzleMetric> fpMetrics = new ArrayList<>();
            mssClient.getCaptureTheFlagTable().scan().items().forEach(ctfMetrics::add);
            mssClient.getGameOfLifeTable().scan().items().forEach(golMetrics::add);
            mssClient.getFifteenPuzzleTable().scan().items().forEach(fpMetrics::add);
            synchronized (this) {
                this.ctfMetrics = ctfMetrics;
                this.golMetrics = golMetrics;
                this.fpMetrics = fpMetrics;
            }
            this.onUpdate.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized List<CaptureTheFlagMetric> getCtfMetrics() {
        return Collections.unmodifiableList(this.ctfMetrics);
    }

    public synchronized List<GameOfLifeMetric> getGolMetrics() {
        return Collections.unmodifiableList(this.golMetrics);
    }

    public synchronized List<FifteenPuzzleMetric> getFpMetrics() {
        return Collections.unmodifiableList(this.fpMetrics);
    }

    public void onMetricsUpdate(Runnable runnable) {
        this.onUpdate = runnable;
    }

}
