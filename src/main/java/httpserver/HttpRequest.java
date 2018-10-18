package httpserver;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class HttpRequest {

    public final String method;
    public final String requestTarget;
    public final String httpVersion;
    public final Map<String, List<String>> headers;
    public final int contentLength;
    public final ByteBuffer entity;

    public HttpRequest(final String method, final String requestTarget, final String httpVersion,
            final Map<String, List<String>> headers, final int contentLength,
            final ByteBuffer entity) {
        this.method = method;
        this.requestTarget = requestTarget;
        this.httpVersion = httpVersion;
        this.headers = headers;
        this.contentLength = contentLength;
        this.entity = entity;
    }
}
