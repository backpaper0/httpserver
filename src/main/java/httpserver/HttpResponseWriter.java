package httpserver;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class HttpResponseWriter {

    private static final byte[] CRLF = "\r\n".getBytes();

    private final OutputStream out;

    public HttpResponseWriter(OutputStream out) {
        this.out = out;
    }

    public void writeStatusLine(String httpVersion, Integer statusCode,
            String reasonPhrase) throws IOException {
        out.write(httpVersion.getBytes());
        out.write(' ');
        out.write(statusCode.toString().getBytes());
        out.write(' ');
        out.write(reasonPhrase.getBytes());
        out.write(CRLF);
        out.flush();
    }

    public void writeResponseHeader(LinkedHashMap<String, Object> header)
            throws IOException {
        for (Entry<String, Object> field : header.entrySet()) {
            out.write(field.getKey().getBytes());
            out.write(": ".getBytes());
            out.write(field.getValue().toString().getBytes());
            out.write(CRLF);
        }
        out.write(CRLF);
        out.flush();
    }

    public void writeResponseBody(byte[] messageBody) throws IOException {
        out.write(messageBody);
        out.flush();
    }

    public void writeChunk(byte[] chunk, int offset, int length)
            throws IOException {
        out.write(Integer.toHexString(length).getBytes());
        out.write(CRLF);
        out.write(chunk, offset, length);
        out.write(CRLF);
        out.flush();
    }

    public void writeLastChunk() throws IOException {
        out.write("0".getBytes());
        out.write(CRLF);
        out.write(CRLF);
        out.flush();
    }
}
