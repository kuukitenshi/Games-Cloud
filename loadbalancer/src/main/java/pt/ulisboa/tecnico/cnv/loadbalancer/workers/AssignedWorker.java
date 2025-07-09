package pt.ulisboa.tecnico.cnv.loadbalancer.workers;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

public interface AssignedWorker {

    String process(HttpExchange he) throws IOException, InterruptedException;

    void finish();

}
