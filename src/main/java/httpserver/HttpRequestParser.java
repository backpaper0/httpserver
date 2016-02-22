package httpserver;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequestParser {

    ByteBuffer out = ByteBuffer.allocate(32);
    String method;
    String requestTarget;
    String httpVersion;
    String headerName;
    Map<String, List<String>> headers = new HashMap<>();
    int contentLength = -1;
    ByteBuffer entity;
    PartialParser parser = new MethodParser();

    public boolean parse(ByteBuffer in) {
        while (in.hasRemaining()) {
            boolean parsed = parser.parse(in);
            if (parsed) {
                return true;
            }
        }
        return false;
    }

    public HttpRequest build() {
        return new HttpRequest(method, requestTarget, httpVersion, headers, contentLength, entity);
    }

    private void put(byte b) {
        if (out.hasRemaining() == false) {
            ByteBuffer buf = ByteBuffer.allocate(out.capacity() * 2);
            out.flip();
            buf.put(out);
            out = buf;
        }
        out.put(b);
    }

    private ByteBuffer getAsByteBuffer() {
        out.flip();
        ByteBuffer bs = ByteBuffer.allocate(out.limit());
        bs.put(out);
        bs.flip();
        out.clear();
        return bs;
    }

    private String getAsString() {
        out.flip();
        byte[] bs = new byte[out.limit()];
        out.get(bs, 0, bs.length);
        out.clear();
        return new String(bs);
    }

    interface PartialParser {
        boolean parse(ByteBuffer in);
    }

    class MethodParser implements PartialParser {

        @Override
        public boolean parse(ByteBuffer in) {
            while (in.hasRemaining()) {
                byte b = in.get();
                if (b == ' ') {
                    parser = new RequestTargetParser();
                    method = getAsString();
                    return false;
                }
                put(b);
            }
            return false;
        }
    }

    class RequestTargetParser implements PartialParser {
        @Override
        public boolean parse(ByteBuffer in) {
            while (in.hasRemaining()) {
                byte b = in.get();
                if (b == ' ') {
                    parser = new HttpVersionParser();
                    requestTarget = getAsString();
                    return false;
                }
                put(b);
            }
            return false;
        }
    }

    class HttpVersionParser implements PartialParser {
        @Override
        public boolean parse(ByteBuffer in) {
            while (in.hasRemaining()) {
                byte b = in.get();
                if (b == '\r') {
                } else if (b == '\n') {
                    parser = new HeaderCheckParser();
                    httpVersion = getAsString();
                    return false;
                } else {
                    put(b);
                }
            }
            return false;
        }
    }

    class HeaderCheckParser implements PartialParser {
        @Override
        public boolean parse(ByteBuffer in) {
            while (in.hasRemaining()) {
                byte b = in.get();
                if (b == '\r') {
                } else if (b == '\n') {
                    List<String> value = headers.get("Content-Length");
                    if (value != null) {
                        parser = new EntityParser();
                        contentLength = Integer.parseInt(value.get(0));
                        if (out.capacity() < contentLength) {
                            out = ByteBuffer.allocate(contentLength);
                        }
                        return false;
                    }
                    return true;
                } else {
                    parser = new HeaderNameParser();
                    in.position(in.position() - 1);
                    return false;
                }
            }
            return false;
        }
    }

    class HeaderNameParser implements PartialParser {
        @Override
        public boolean parse(ByteBuffer in) {
            while (in.hasRemaining()) {
                byte b = in.get();
                if (b == ':') {
                    parser = new HeaderIntervalParser();
                    headerName = getAsString();
                    return false;
                }
                put(b);
            }
            return false;
        }
    }

    class HeaderIntervalParser implements PartialParser {
        @Override
        public boolean parse(ByteBuffer in) {
            while (in.hasRemaining()) {
                byte b = in.get();
                if (b != ' ') {
                    parser = new HeaderValueParser();
                    in.position(in.position() - 1);
                    return false;
                }
            }
            return false;
        }
    }

    class HeaderValueParser implements PartialParser {
        @Override
        public boolean parse(ByteBuffer in) {
            while (in.hasRemaining()) {
                byte b = in.get();
                if (b == '\r') {
                } else if (b == '\n') {
                    parser = new HeaderCheckParser();
                    headers.computeIfAbsent(headerName, key -> new ArrayList<>())
                            .add(getAsString());
                    return false;
                } else {
                    put(b);
                }
            }
            return false;
        }
    }

    class EntityParser implements PartialParser {
        @Override
        public boolean parse(ByteBuffer in) {
            while (in.hasRemaining()) {
                byte b = in.get();
                put(b);
                if ((out.position() < contentLength) == false) {
                    entity = getAsByteBuffer();
                    return true;
                }
            }
            return false;
        }
    }
}
