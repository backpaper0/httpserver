package httpserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

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

    public void write(HttpResponse response) throws IOException {
        String httpVersion = response.getHttpVersion();
        Integer statusCode = response.getStatusCode();
        String reasonPhrase = response.getReasonPhase();

        Map<String, Object> responseHeader = response.getMessageHeader();
        try (InputStream messageBodyInputStream = response.getMessageBody()) {

            //ステータスライン
            writeStatusLine(httpVersion, statusCode, reasonPhrase);

            //レスポンスヘッダ
            DateFormat df =
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
            df.setTimeZone(TimeZone.getTimeZone("GMT"));

            LinkedHashMap<String, Object> header = new LinkedHashMap<>();

            //general-header
            header.put("Connection", "close");
            header.put("Date", df.format(new Date()));

            //response-header
            header.put("Server", "SimpleHttpServer/0.1");

            header.putAll(responseHeader);

            //entity-header
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
            if (chunked) {
                header.put("Transfer-Encoding", "chunked");
                writeResponseHeader(header);

                byte[] chunk = messageBodyOutputStream.toByteArray();
                writeChunk(chunk, 0, chunk.length);

                while (-1 != (i =
                    messageBodyInputStream.read(chunk, 0, chunk.length))) {
                    writeChunk(chunk, 0, i);
                }
                writeLastChunk();

            } else {
                byte[] messageBody = messageBodyOutputStream.toByteArray();
                header.put("Content-Length", messageBody.length);
                writeResponseHeader(header);

                //メッセージボディ
                writeResponseBody(messageBody);
            }
        }
    }
}
