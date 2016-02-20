package httpserver;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpResponseFormatter {

    ByteBuffer buf = ByteBuffer.allocate(32);

    public ByteBuffer format(HttpResponse response) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.putAll(response.headers);

        ByteBuffer entity = response.entity;

        int statusCode = response.statusCode;
        if (statusCode < -1) {
            statusCode = entity != null ? 200 : 204;
        }

        put("HTTP/1.1 ");
        put(String.valueOf(statusCode));
        put(" ");
        put(response.reasonPhrase);
        put("\r\n");

        if (entity != null) {
            headers.computeIfAbsent("Content-Length", key -> new ArrayList<>())
                    .add(String.valueOf(entity.limit()));
        }
        headers.computeIfAbsent("Server", key -> new ArrayList<>()).add("backpaper0-http-server");

        headers.forEach((key, values) -> {
            put(key);
            put(": ");
            put(values.stream().collect(Collectors.joining(",")));
            put("\r\n");
        });
        put("\r\n");
        if (entity != null) {
            put(entity);
        }
        buf.flip();
        return buf;
    }

    void put(ByteBuffer b) {
        if ((buf.position() + b.limit() < buf.capacity()) == false) {
            ByteBuffer next = ByteBuffer
                    .allocate(Math.max(buf.capacity() * 2, buf.position() + b.limit()));
            buf.flip();
            next.put(buf);
            buf = next;
        }
        buf.put(b);
    }

    void put(String s) {
        byte[] bs = s.getBytes();
        if ((buf.position() + bs.length < buf.capacity()) == false) {
            ByteBuffer next = ByteBuffer
                    .allocate(Math.max(buf.capacity() * 2, buf.position() + bs.length));
            buf.flip();
            next.put(buf);
            buf = next;
        }
        buf.put(bs);
    }
}
