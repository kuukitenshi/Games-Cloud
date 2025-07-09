package pt.ulisboa.tecnico.cnv.webserver;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

import pt.ulisboa.tecnico.cnv.mss.MSSClient;

public class WebServer {
    public static void main(String[] args) throws Exception {
        MSSClient mssClient = new MSSClient();
        MetricsCollector metricsCollector = new MetricsCollector(mssClient);
        metricsCollector.start();

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", new RootHandler());
        server.createContext("/gameoflife", new InstrumentedGameOfLifeHandler(metricsCollector));
        server.createContext("/fifteenpuzzle", new InstrumentedFifteenPuzzleHandler(metricsCollector));
        server.createContext("/capturetheflag", new InstrumentedCaptureTheFlagHandler(metricsCollector));
        server.start();
        System.out.println("WebServer started!");
    }
}
