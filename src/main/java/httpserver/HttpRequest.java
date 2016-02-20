package httpserver;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequest {
    public String method;
    public String requestTarget;
    public String httpVersion;
    public Map<String, List<String>> headers = new HashMap<>();
    public int contentLength = -1;
    public ByteBuffer entity;

    public HttpRequest(String method, String requestTarget, String httpVersion,
            Map<String, List<String>> headers, int contentLength, ByteBuffer entity) {
        this.method = method;
        this.requestTarget = requestTarget;
        this.httpVersion = httpVersion;
        this.headers = headers;
        this.contentLength = contentLength;
        this.entity = entity;
    }
}
