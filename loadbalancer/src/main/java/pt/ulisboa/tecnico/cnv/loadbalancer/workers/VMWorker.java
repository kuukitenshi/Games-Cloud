package pt.ulisboa.tecnico.cnv.loadbalancer.workers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

import com.sun.net.httpserver.HttpExchange;

import pt.ulisboa.tecnico.cnv.loadbalancer.InstanceManager;
import pt.ulisboa.tecnico.cnv.loadbalancer.logging.ILogger;
import pt.ulisboa.tecnico.cnv.loadbalancer.logging.LoggerFactory;
import pt.ulisboa.tecnico.cnv.loadbalancer.vm.VMInstance;

public class VMWorker implements AssignedWorker {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ILogger LOGGER = LoggerFactory.getLogger(VMWorker.class);

    private final InstanceManager instanceManager;
    private final VMInstance vm;
    private final long complexity;

    public VMWorker(InstanceManager instanceManager, VMInstance vm, long complexity) {
        this.instanceManager = instanceManager;
        this.vm = vm;
        this.complexity = complexity;
    }

    @Override
    public String process(HttpExchange he) throws IOException, InterruptedException {
        String path = he.getRequestURI().getPath();
        String query = he.getRequestURI().getQuery();
        String ip = vm.getEc2Instance().publicIpAddress();
        URI instanceUrl = URI.create(String.format("http://%s:8000%s?%s", ip, path, query));

        LOGGER.debug("Forwarding request to VM at " + instanceUrl);
        HttpRequest request = HttpRequest.newBuilder().GET().uri(instanceUrl).build();
        return HTTP_CLIENT.send(request, BodyHandlers.ofString()).body();
    }

    @Override
    public void finish() {
        LOGGER.debug("Worker finished!");
        synchronized (this.instanceManager) {
            this.vm.removeWork(this.complexity);
        }
    }

}
