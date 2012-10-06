package httpserver;

import java.io.IOException;
import java.io.OutputStream;

public interface HttpRequestHandler {

    HttpResponse handleRequest(OutputStream responseStream, HttpRequest request)
            throws IOException;

}
