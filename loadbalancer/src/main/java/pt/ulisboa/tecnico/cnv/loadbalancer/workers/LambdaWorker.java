package pt.ulisboa.tecnico.cnv.loadbalancer.workers;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;

import pt.ulisboa.tecnico.cnv.loadbalancer.logging.ILogger;
import pt.ulisboa.tecnico.cnv.loadbalancer.logging.LoggerFactory;
import pt.ulisboa.tecnico.cnv.loadbalancer.utils.HttpUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

public class LambdaWorker implements AssignedWorker {

    private static final ILogger LOGGER = LoggerFactory.getLogger(LambdaWorker.class);
    private static final Gson GSON = new GsonBuilder().create();
    private static final LambdaClient LAMBDA_CLIENT = LambdaClient.create();

    @Override
    public String process(HttpExchange he) {
        String path = he.getRequestURI().getPath();
        String functionName;
        switch (path) {
            case "/capturetheflag":
            case "/gameoflife":
            case "/fifteenpuzzle":
                functionName = path.substring(1);
                break;
            default:
                throw new IllegalArgumentException("Path not found!");
        }
        String query = he.getRequestURI().getQuery();
        Map<String, String> params = HttpUtils.queryToMap(query);
        String json = GSON.toJson(params);

        LOGGER.debug("calling lambda function %s with paylod %s", functionName, json);
        InvokeRequest request = InvokeRequest.builder()
                .functionName(functionName)
                .payload(SdkBytes.fromUtf8String(json))
                .build();
        InvokeResponse response = LAMBDA_CLIENT.invoke(request);
        LOGGER.debug("lambda response: " + response.payload().asUtf8String());
        if (response.functionError() != null) {
            return null;
        }
        return response.payload().asUtf8String();
    }

    @Override
    public void finish() {
    }

}
