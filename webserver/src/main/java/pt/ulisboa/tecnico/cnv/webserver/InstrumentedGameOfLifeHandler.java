package pt.ulisboa.tecnico.cnv.webserver;

import java.io.IOException;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import pt.ulisboa.tecnico.cnv.gameoflife.GameOfLifeHandler;
import pt.ulisboa.tecnico.cnv.javassistagent.tools.ThreadedICount;
import pt.ulisboa.tecnico.cnv.mss.GameOfLifeMetric;

public class InstrumentedGameOfLifeHandler implements HttpHandler {

    private final GameOfLifeHandler handler = new GameOfLifeHandler();
    private final MetricsCollector metricsCollector;

    public InstrumentedGameOfLifeHandler(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> parameters = HttpUtils.queryToMap(exchange.getRequestURI().getRawQuery());

        int iterations = Integer.parseInt(parameters.get("iterations"));
        String mapFilename = parameters.get("mapFilename");

        ThreadedICount.startInstrumentation();
        this.handler.handle(exchange);
        long ninsts = ThreadedICount.finishInstrumentation();

        GameOfLifeMetric metric = new GameOfLifeMetric(mapFilename, iterations, ninsts);
        this.metricsCollector.collectMetric(metric);
    }

}
