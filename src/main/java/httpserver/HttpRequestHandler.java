package httpserver;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public interface HttpRequestHandler {

    HttpResponse handleRequest(OutputStream responseStream,
            String[] requestLine, Map<String, String> requestHeader,
            byte[] requestEntity) throws IOException;

}
