package pt.ulisboa.tecnico.cnv.webserver;

import java.io.IOException;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import pt.ulisboa.tecnico.cnv.capturetheflag.CaptureTheFlagHandler;
import pt.ulisboa.tecnico.cnv.javassistagent.tools.ThreadedICount;
import pt.ulisboa.tecnico.cnv.mss.CaptureTheFlagMetric;

public class InstrumentedCaptureTheFlagHandler implements HttpHandler {

    private final CaptureTheFlagHandler handler = new CaptureTheFlagHandler();
    private final MetricsCollector metricsCollector;

    public InstrumentedCaptureTheFlagHandler(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> parameters = HttpUtils.queryToMap(exchange.getRequestURI().getRawQuery());

        int gridSize = Integer.parseInt(parameters.get("gridSize"));
        int numBlueAgents = Integer.parseInt(parameters.get("numBlueAgents"));
        int numRedAgents = Integer.parseInt(parameters.get("numRedAgents"));
        char flagPlacementType = parameters.get("flagPlacementType").toUpperCase().charAt(0);

        ThreadedICount.startInstrumentation();
        this.handler.handle(exchange);
        long ninsts = ThreadedICount.finishInstrumentation();

        CaptureTheFlagMetric metric = new CaptureTheFlagMetric(gridSize, numBlueAgents, numRedAgents, flagPlacementType,
                ninsts);
        this.metricsCollector.collectMetric(metric);
    }

}
