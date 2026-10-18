package com.crud2gui.statementanalyzer;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import com.crud2gui.statementanalyzer.model.ModelResponse;
import com.crud2gui.statementanalyzer.model.ModelInfo;


/** Servidor HTTP local que registra las peticiones y responde lo que decida el test. */
final class FakeServer implements AutoCloseable {

    record Recorded(String method, String pathAndQuery, Headers headers, String body) { }

    record Reply(int status, String body) {
        static Reply ok(String body) {
            return new Reply(200, body);
        }
    }

    private final HttpServer server;
    private final List<Recorded> requests = new CopyOnWriteArrayList<>();

    FakeServer(Function<Recorded, Reply> handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            byte[] in = exchange.getRequestBody().readAllBytes();
            String query = exchange.getRequestURI().getRawQuery();
            Recorded recorded = new Recorded(exchange.getRequestMethod(),
                    exchange.getRequestURI().getRawPath() + (query == null ? "" : "?" + query),
                    exchange.getRequestHeaders(), new String(in, StandardCharsets.UTF_8));
            requests.add(recorded);
            Reply reply = handler.apply(recorded);
            byte[] out = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(reply.status(), out.length == 0 ? -1 : out.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(out);
            }
        });
        server.start();
    }

    URI uri(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    List<Recorded> requests() {
        return requests;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
