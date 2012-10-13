package httpserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HttpRequestReader {

    private static final char CR = '\r';

    private static final char LF = '\n';

    private static final char HT = '\t';

    private final PushbackInputStream in;

    public HttpRequestReader(InputStream in) {
        this.in = new PushbackInputStream(in);
    }

    public String[] readRequestLine() throws IOException {
        int b;
        ByteArrayOutputStream method = new ByteArrayOutputStream();
        while (-1 != (b = in.read())) {
            if (b == ' ' || b == HT) {
                break;
            }
            method.write(b);
        }
        if (method.size() == 0) {
            throw new IllegalStateException();
        }

        //SPとHTを読み捨てる
        while (-1 != (b = in.read())) {
            if (b != ' ' && b != HT) {
                in.unread(b);
                break;
            }
        }

        ByteArrayOutputStream requestUri = new ByteArrayOutputStream();
        while (-1 != (b = in.read())) {
            if (b == ' ' || b == HT) {
                break;
            }
            requestUri.write(b);
        }
        if (requestUri.size() == 0) {
            throw new IllegalStateException();
        }

        //SPとHTを読み捨てる
        while (-1 != (b = in.read())) {
            if (b != ' ' && b != HT) {
                in.unread(b);
                break;
            }
        }

        ByteArrayOutputStream httpVersion = new ByteArrayOutputStream();
        while (-1 != (b = in.read())) {
            /*
             * CRを無視してLFで改行の判断をする寛容っぷりを発揮
             * http://www.studyinghttp.net/cgi-bin/rfc.cgi?2616#Sec19.3
             */
            if (b == CR) {
                b = in.read();
            }
            if (b == LF) {
                return new String[] {
                    method.toString(),
                    requestUri.toString(),
                    httpVersion.toString() };
            }
            httpVersion.write(b);
        }

        throw new IllegalArgumentException();
    }

    public Map<String, String> readRequestHeader() throws IOException {
        Map<String, String> requestHeader = new HashMap<>();
        int b;
        while (-1 != (b = in.read())) {
            if (b == CR) {
                b = in.read();
            }
            if (b == LF) {
                break;
            }
            in.unread(b);

            String[] requestHeaderField = readRequestHeaderField();
            requestHeader.put(requestHeaderField[0], requestHeaderField[1]);
        }
        return requestHeader;
    }

    protected String[] readRequestHeaderField() throws IOException {
        ByteArrayOutputStream name = new ByteArrayOutputStream();
        int b;
        while (-1 != (b = in.read())) {
            if (b == ':') {
                break;
            }
            name.write(b);
        }

        //fieldに先行するLWSを読み飛ばす
        b = in.read();
        if (b == CR) {
            b = in.read();
        }
        if (b == LF) {
            b = in.read();
        }
        if ((b != ' ' && b != HT) == false) {
            while (-1 != (b = in.read())) {
                if (b != ' ' && b != HT) {
                    break;
                }
            }
        }

        ByteArrayOutputStream value = new ByteArrayOutputStream();
        value.write(b);
        while (-1 != (b = in.read())) {
            if (b == CR) {
                b = in.read();
            }
            if (b == LF) {
                b = in.read();
                if (b != ' ' && b != HT) {
                    in.unread(b);

                    //field-nameは大文字・小文字を区別しない
                    //ここでは全て小文字にしておく
                    return new String[] {
                        name.toString().toLowerCase(),
                        value.toString() };
                }
                //field-valueはLWSを行頭につけると改行可能
                while (-1 != (b = in.read())) {
                    if (b != ' ' && b != HT) {
                        break;
                    }
                }
            }
            value.write(b);
        }
        throw new IllegalStateException();
    }

    public InputStream readEntityBody(int contentLength) throws IOException {
        byte[] entityBody = new byte[contentLength];
        int total = 0;
        int readSize;
        while (-1 != (readSize =
            in.read(entityBody, total, contentLength - total))) {
            total += readSize;
            if (total == contentLength) {
                return new ByteArrayInputStream(entityBody);
            }
        }
        throw new IllegalStateException();
    }

    public HttpRequest read() throws IOException {
        HttpRequest request = new HttpRequest();
        request.setRequestLine(readRequestLine());
        System.out.println("[Request Line] "
            + Arrays.toString(request.getRequestLine()));
        request.setRequestHeader(readRequestHeader());
        System.out.println("[Request header] " + request.getRequestHeader());

        if (Objects.equals(
            request.getRequestHeader().get("transfer-encoding"),
            "chunked")) {
            request.setRequestEntity(readChunk());

        } else if (request.getRequestHeader().containsKey("content-length")) {
            int contentLength =
                Integer.parseInt(request.getRequestHeader().get(
                    "content-length"));
            request.setRequestEntity(readEntityBody(contentLength));
        }
        return request;
    }

    InputStream readChunk() throws IOException {
        int b;
        ByteArrayOutputStream chunk = new ByteArrayOutputStream();
        while (-1 != (b = in.read())) {
            in.unread(b);
            ByteArrayOutputStream chunkSize = new ByteArrayOutputStream();
            while (-1 != (b = in.read())) {
                if (b == CR) {
                    b = in.read();
                }
                if (b == LF) {
                    break;
                }
                chunkSize.write(b);
            }
            byte[] bs = new byte[8192];
            int i;
            int size = Integer.parseInt(chunkSize.toString(), 16);
            if (size != 0) {
                while (size > 0 && -1 != (i = in.read(bs, 0, size))) {
                    chunk.write(bs, 0, i);
                    size -= i;
                }
                b = in.read();
                if (b == CR) {
                    b = in.read();
                }
                if (b != LF) {
                    throw new IllegalArgumentException();
                }
            } else {
                b = in.read();
                if (b == CR) {
                    b = in.read();
                }
                if (b != LF) {
                    throw new IllegalArgumentException();
                }
                return new ByteArrayInputStream(chunk.toByteArray());
            }
        }
        throw new IllegalArgumentException();
    }
}
