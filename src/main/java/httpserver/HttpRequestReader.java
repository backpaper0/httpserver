package httpserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.HashMap;
import java.util.Map;

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

    public byte[] readEntityBody(int contentLength) throws IOException {
        int b;
        ByteArrayOutputStream entityBody = new ByteArrayOutputStream();
        while (-1 != (b = in.read())) {
            entityBody.write(b);
            if (entityBody.size() == contentLength) {
                return entityBody.toByteArray();
            }
        }
        throw new IllegalStateException();
    }
}
