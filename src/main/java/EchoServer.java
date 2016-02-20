import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import httpserver.HttpRequest;
import httpserver.HttpResponse;
import httpserver.HttpServer;

public class EchoServer {

    public static void main(String[] args) throws Exception {
        HttpServer server = new HttpServer("0.0.0.0", 8080, EchoServer::handle);
        server.start();
    }

    static HttpResponse handle(HttpRequest request) throws Exception {
        int statusCode = 200;
        String reasonPhrase = "OK";
        Map<String, List<String>> headers = new HashMap<>();
        headers.computeIfAbsent("Content-Type", key -> new ArrayList<>())
                .add("text/plain; charset=UTF-8");
        ByteBuffer entity = request.entity;
        return new HttpResponse(statusCode, reasonPhrase, headers, entity);
    }
}