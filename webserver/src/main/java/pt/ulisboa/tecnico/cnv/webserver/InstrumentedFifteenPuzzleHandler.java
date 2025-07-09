package pt.ulisboa.tecnico.cnv.webserver;

import java.io.IOException;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import pt.ulisboa.tecnico.cnv.fifteenpuzzle.FifteenPuzzleHandler;
import pt.ulisboa.tecnico.cnv.javassistagent.tools.ThreadedICount;
import pt.ulisboa.tecnico.cnv.mss.FifteenPuzzleMetric;

public class InstrumentedFifteenPuzzleHandler implements HttpHandler {

    private final FifteenPuzzleHandler handler = new FifteenPuzzleHandler();
    private final MetricsCollector metricsCollector;

    public InstrumentedFifteenPuzzleHandler(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> parameters = HttpUtils.queryToMap(exchange.getRequestURI().getRawQuery());

        int size = Integer.parseInt(parameters.get("size"));
        int shuffles = Integer.parseInt(parameters.get("shuffles"));

        ThreadedICount.startInstrumentation();
        this.handler.handle(exchange);
        long ninsts = ThreadedICount.finishInstrumentation();

        FifteenPuzzleMetric metric = new FifteenPuzzleMetric(size, shuffles, ninsts);
        this.metricsCollector.collectMetric(metric);
    }

}
