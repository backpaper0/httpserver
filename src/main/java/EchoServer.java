import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import httpserver.HttpRequest;
import httpserver.HttpResponse;
import httpserver.HttpServer;

public class EchoServer {

    public static void main(final String[] args) throws Exception {
        final HttpServer server = new HttpServer("0.0.0.0", 8080, EchoServer::handle);
        server.start();
        System.in.read();
        server.stop();
    }

    static HttpResponse handle(final HttpRequest request) throws Exception {
        final int statusCode = 200;
        final String reasonPhrase = "OK";
        final Map<String, List<String>> headers = new HashMap<>();
        headers.computeIfAbsent("Content-Type", key -> new ArrayList<>())
                .add("text/plain; charset=UTF-8");
        String s;
        if (request.method.equals("GET")) {
            s = request.requestTarget;
        } else {
            s = new String(request.entity.array());
        }
        final ByteBuffer entity = ByteBuffer.wrap(s.getBytes());
        return new HttpResponse(statusCode, reasonPhrase, headers, entity);
    }
}
