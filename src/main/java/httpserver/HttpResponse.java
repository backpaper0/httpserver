package httpserver;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class HttpResponse {

    public final int statusCode;
    public final String reasonPhrase;
    public final Map<String, List<String>> headers;
    public final ByteBuffer entity;

    public HttpResponse(int statusCode, String reasonPhrase, Map<String, List<String>> headers,
            ByteBuffer entity) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.headers = headers;
        this.entity = entity;
    }
}
