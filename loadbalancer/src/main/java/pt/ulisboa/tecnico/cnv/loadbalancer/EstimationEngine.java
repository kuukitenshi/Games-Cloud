package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.sun.net.httpserver.HttpExchange;

import pt.ulisboa.tecnico.cnv.loadbalancer.logging.ILogger;
import pt.ulisboa.tecnico.cnv.loadbalancer.logging.LoggerFactory;
import pt.ulisboa.tecnico.cnv.loadbalancer.utils.HttpUtils;
import pt.ulisboa.tecnico.cnv.loadbalancer.utils.KNN;
import pt.ulisboa.tecnico.cnv.mss.CaptureTheFlagMetric;
import pt.ulisboa.tecnico.cnv.mss.FifteenPuzzleMetric;
import pt.ulisboa.tecnico.cnv.mss.GameOfLifeMetric;
import weka.core.Instances;

public class EstimationEngine {

    private static final ILogger LOGGER = LoggerFactory.getLogger(EstimationEngine.class);

    private static final long BASE_CTF_COMPLEXITY = 1000;
    private static final long BASE_GOL_COMPLEXITY = 1000;
    private static final long BASE_FP_COMPLEXITY = 1000;

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private KNN ctfKnnModel = null;
    private KNN golKnnModel = null;
    private KNN fpKnnModel = null;

    public void updateEstimationData(MetricsCache metricsCache) {
        Lock lock = this.rwLock.writeLock();
        lock.lock();
        try {
            LOGGER.debug("updating data used for estimation");
            trainCtf(metricsCache.getCtfMetrics());
            trainGol(metricsCache.getGolMetrics());
            trainFp(metricsCache.getFpMetrics());
        } finally {
            lock.unlock();
        }
    }

    public long estimateComplexity(HttpExchange he) {
        Lock lock = this.rwLock.readLock();
        lock.lock();
        try {
            String path = he.getRequestURI().getPath();
            String query = he.getRequestURI().getQuery();
            return switch (path) {
                case "/capturetheflag" -> estimateCaptureTheFlag(query);
                case "/gameoflife" -> estimateGameOfLife(query);
                case "/fifteenpuzzle" -> estimateFifteenPuzzle(query);
                default -> throw new IllegalArgumentException("Path not found!");
            };
        } finally {
            lock.unlock();
        }
    }

    private void trainCtf(List<CaptureTheFlagMetric> metrics) {
        if (metrics.size() <= 5) {
            return;
        }
        try {
            this.ctfKnnModel = new KNN();
            this.ctfKnnModel.train(KNN.buildInstancesCTF(metrics));
        } catch (Exception e) {
            LOGGER.error("Failed to create KNN model: " + e.getMessage());
        }
    }

    private void trainGol(List<GameOfLifeMetric> metrics) {
        if (metrics.size() <= 3) {
            return;
        }
        try {
            this.golKnnModel = new KNN();
            this.golKnnModel.train(KNN.buildInstancesGOF(metrics));
        } catch (Exception e) {
            LOGGER.error("Failed to create KNN model: " + e.getMessage());
        }
    }

    private void trainFp(List<FifteenPuzzleMetric> metrics) {
        if (metrics.size() <= 4) {
            return;
        }
        try {
            this.fpKnnModel = new KNN();
            this.fpKnnModel.train(KNN.buildInstancesFP(metrics));
        } catch (Exception e) {
            LOGGER.error("Failed to create KNN model: " + e.getMessage());
        }
    }

    private long estimateCaptureTheFlag(String query) {
        Map<String, String> params = HttpUtils.queryToMap(query);
        int gridSize = Integer.parseInt(params.get("gridSize"));
        int blue = Integer.parseInt(params.get("numBlueAgents"));
        int red = Integer.parseInt(params.get("numRedAgents"));
        char flagPlacement = params.get("flagPlacementType").charAt(0);
        if (this.ctfKnnModel == null || this.ctfKnnModel.isNotTrained()) {
            return BASE_CTF_COMPLEXITY;
        }
        return estimateKNNCTFInstructCount(gridSize, blue, red, flagPlacement);
    }

    private long estimateKNNCTFInstructCount(int gridSize, int blue, int red, char flagPlacement) {
        CaptureTheFlagMetric metric = new CaptureTheFlagMetric();
        metric.setGridSize(gridSize);
        metric.setBlue(blue);
        metric.setRed(red);
        metric.setFlagPlacement(flagPlacement);
        List<CaptureTheFlagMetric> metrics = List.of(metric);
        Instances dataset = KNN.buildInstancesCTF(metrics);
        return this.ctfKnnModel.predict(dataset);
    }

    private long estimateGameOfLife(String query) {
        Map<String, String> params = HttpUtils.queryToMap(query);
        String filename = params.get("mapFilename");
        int iterations = Integer.parseInt(params.get("iterations"));
        if (this.golKnnModel == null || this.golKnnModel.isNotTrained()) {
            return BASE_GOL_COMPLEXITY;
        }
        return estimateKNNGOFInstructCount(filename, iterations);
    }

    private long estimateKNNGOFInstructCount(String mapFile, int iterations) {
        GameOfLifeMetric metric = new GameOfLifeMetric();
        metric.setMapFile(mapFile);
        metric.setIterations(iterations);
        List<GameOfLifeMetric> metrics = List.of(metric);
        Instances dataset = KNN.buildInstancesGOF(metrics);
        return this.golKnnModel.predict(dataset);
    }

    private long estimateFifteenPuzzle(String query) {
        Map<String, String> params = HttpUtils.queryToMap(query);
        int size = Integer.parseInt(params.get("size"));
        int shuffles = Integer.parseInt(params.get("shuffles"));
        if (this.fpKnnModel == null || this.fpKnnModel.isNotTrained()) {
            return BASE_FP_COMPLEXITY;
        }
        return estimateKNNFPInstructCount(size, shuffles);
    }

    private long estimateKNNFPInstructCount(int size, int shuffles) {
        FifteenPuzzleMetric metric = new FifteenPuzzleMetric();
        metric.setSize(size);
        metric.setShuffles(shuffles);
        List<FifteenPuzzleMetric> metrics = List.of(metric);
        Instances dataset = KNN.buildInstancesFP(metrics);
        return this.fpKnnModel.predict(dataset);
    }

}
