package httpserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

public class Http11ResponseWriterImpl implements HttpResponseWriter {

    private static final byte[] CRLF = "\r\n".getBytes();

    private static final List<String> HEADER_FIELDS = Arrays.asList(
    //general-header
        "cache-control",
        "connection",
        "date",
        "pragma",
        "trailer",
        "transfer-encoding",
        "upgrade",
        "via",
        "warning",
        //        //request-header
        //        "accept",
        //        "accept-charset",
        //        "accept-encoding",
        //        "accept-language",
        //        "authorization",
        //        "expect",
        //        "from",
        //        "host",
        //        "if-match",
        //        "if-modified-since",
        //        "if-none-match",
        //        "if-range",
        //        "if-unmodified-since",
        //        "max-forwards",
        //        "proxy-authorization",
        //        "range",
        //        "referer",
        //        "te",
        //        "user-agent",
        //response-header
        "accept-ranges",
        "age",
        "etag",
        "location",
        "proxy-authenticate",
        "retry-after",
        "server",
        "vary",
        "www-authenticate",
        //entity-header
        "allow",
        "content-encoding",
        "content-language",
        "content-length",
        "content-location",
        "content-md5",
        "content-range",
        "content-type",
        "expires",
        "last-modified");

    private static final Comparator<Entry<String, Object>> HEADER_COMPARATOR =
        new Comparator<Map.Entry<String, Object>>() {

            @Override
            public int compare(Entry<String, Object> o1,
                    Entry<String, Object> o2) {
                int x = HEADER_FIELDS.indexOf(o1.getKey().toLowerCase());
                if (x == -1) {
                    x = Integer.MAX_VALUE;
                }
                int y = HEADER_FIELDS.indexOf(o2.getKey().toLowerCase());
                if (y == -1) {
                    y = Integer.MAX_VALUE;
                }
                return Integer.compare(x, y);
            }
        };

    private final OutputStream out;

    public Http11ResponseWriterImpl(OutputStream out) {
        this.out = out;
    }

    public void writeStatusLine(Integer statusCode, String reasonPhrase)
            throws IOException {
        out.write("HTTP/1.1 ".getBytes());
        out.write(statusCode.toString().getBytes());
        out.write(' ');
        out.write(reasonPhrase.getBytes());
        out.write(CRLF);
        out.flush();
    }

    public void writeResponseHeader(Collection<Entry<String, Object>> header)
            throws IOException {
        List<Entry<String, Object>> sortedHeader = new ArrayList<>(header);
        Collections.sort(sortedHeader, HEADER_COMPARATOR);
        for (Entry<String, Object> field : sortedHeader) {
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

    @Override
    public void write(HttpResponse response) throws IOException {
        Integer statusCode = response.getStatusCode();
        String reasonPhrase = response.getReasonPhase();

        //ステータスライン
        writeStatusLine(statusCode, reasonPhrase);

        //レスポンスヘッダ
        Map<String, Object> header = new HashMap<>(response.getMessageHeader());

        header.put("Connection", "close");

        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        header.put("Date", df.format(new Date()));

        header.put("Server", "SimpleHttpServer/0.1");

        try (InputStream messageBodyInputStream = response.getMessageBody()) {
            ByteArrayOutputStream messageBodyOutputStream =
                new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int i;
            boolean chunked = false;
            while (-1 != (i = messageBodyInputStream.read(b, 0, b.length))) {
                messageBodyOutputStream.write(b, 0, i);

                if (messageBodyOutputStream.size() > 10000) { //10KB
                    chunked = true;
                    break;
                }
            }

            byte[] messageBody = messageBodyOutputStream.toByteArray();
            if (chunked) {
                header.put("Transfer-Encoding", "chunked");

            } else {
                header.put("Content-Length", messageBody.length);
            }

            writeResponseHeader(header.entrySet());

            if (chunked) {
                byte[] chunk = messageBody;
                writeChunk(chunk, 0, chunk.length);

                while (-1 != (i =
                    messageBodyInputStream.read(chunk, 0, chunk.length))) {
                    writeChunk(chunk, 0, i);
                }

                writeLastChunk();

            } else {
                writeResponseBody(messageBody);
            }
        }
    }
}
