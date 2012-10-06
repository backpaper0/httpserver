package httpserver;

import java.io.IOException;

public interface HttpRequestHandler {

    HttpResponse handleRequest(HttpRequest request) throws IOException;

}
