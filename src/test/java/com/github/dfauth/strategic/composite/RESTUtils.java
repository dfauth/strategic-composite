package com.github.dfauth.strategic.composite;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class RESTUtils {

    private static final Logger logger = LoggerFactory.getLogger(RESTUtils.class);

    public static int mockRestEndpoint(String endpoint, byte[] response, Consumer<String> c) {
        try {
            AtomicInteger i = new AtomicInteger();
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
            httpServer.createContext(endpoint, exchange -> {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                i.incrementAndGet();
                exchange.close();
            });
            httpServer.start();
            try {
                String url = String.format("http://localhost:%d", httpServer.getAddress().getPort());
                c.accept(url);
            } finally {
                httpServer.stop(0);
                return i.intValue();
            }
        } catch (IOException e) {
            logger.info(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}