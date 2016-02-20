package httpserver;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpResponse {
    public int statusCode = -1;
    public String reasonPhrase;
    public Map<String, List<String>> headers = new HashMap<>();
    public ByteBuffer entity;

    public HttpResponse(int statusCode, String reasonPhrase, Map<String, List<String>> headers,
            ByteBuffer entity) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.headers = headers;
        this.entity = entity;
    }
}
