package pt.ulisboa.tecnico.cnv.loadbalancer.handlers;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import pt.ulisboa.tecnico.cnv.loadbalancer.LoadBalancer;
import pt.ulisboa.tecnico.cnv.loadbalancer.logging.ILogger;
import pt.ulisboa.tecnico.cnv.loadbalancer.logging.LoggerFactory;
import pt.ulisboa.tecnico.cnv.loadbalancer.utils.HttpUtils;
import pt.ulisboa.tecnico.cnv.loadbalancer.workers.AssignedWorker;
import pt.ulisboa.tecnico.cnv.loadbalancer.workers.LambdaWorker;

public class RequestHandler implements HttpHandler {

    private static final ILogger LOGGER = LoggerFactory.getLogger(RequestHandler.class.getSimpleName());
    private static final int RETRY_COUNT = 10;
    private static final long RETRY_TIME = 5000L;

    private final LoadBalancer loadBalancer;

    public RequestHandler(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        LOGGER.debug("Request received: " + he.getRequestURI());

        he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        if ("OPTIONS".equalsIgnoreCase(he.getRequestMethod())) {
            he.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            he.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            he.sendResponseHeaders(204, -1);
            return;
        }

        int retries = RETRY_COUNT + 1;
        boolean allowLambda = true;
        while (retries > 0) {
            LOGGER.debug("processing request");
            AssignedWorker worker = null;
            try {
                worker = loadBalancer.handleRequest(he, allowLambda);
            } catch (IOException e) {
                LOGGER.error("failed to assign a worker for request %s", he.getRequestURI());
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                HttpUtils.sendError(he, 404, e.getMessage());
                break;
            } catch (InterruptedException e) {
                LOGGER.error("request interrupted");
                e.printStackTrace();
                HttpUtils.sendError(he, 500, e.getMessage());
                break;
            }
            if (worker != null) {
                LOGGER.debug("worker assigned to process request, starting work");
                try {
                    String response = worker.process(he);
                    if (response == null && worker instanceof LambdaWorker) {
                        allowLambda = false;
                    } else {
                        LOGGER.info("received worker response!");
                        HttpUtils.sendResponse(he, 200, response);
                        break;
                    }
                } catch (IOException | InterruptedException e) {
                    LOGGER.error("worker failed to process request %s", he.getRequestURI());
                    e.printStackTrace();
                } finally {
                    worker.finish();
                }
            }
            LOGGER.warn("couldn't assign a worker for this request, retrying shortly...");
            retries--;
            try {
                Thread.sleep(RETRY_TIME);
            } catch (InterruptedException e) {
                LOGGER.error("request interrupted");
                e.printStackTrace();
                break;
            }
        }
    }

}
