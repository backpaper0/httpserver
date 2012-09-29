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
        ByteArrayOutputStream requestLine = new ByteArrayOutputStream();
        int i;
        while (-1 != (i = in.read())) {
            /*
             * CRを無視してLFで改行の判断をする寛容っぷりを発揮
             * http://www.studyinghttp.net/cgi-bin/rfc.cgi?2616#Sec19.3
             */
            if (i == CR) {
                i = in.read();
            }
            if (i == LF) {
                return requestLine.toString().split(" ");
            }
            requestLine.write(i);
        }
        throw new IllegalArgumentException();
    }

    public Map<String, String> readRequestHeader() throws IOException {
        Map<String, String> requestHeader = new HashMap<>();
        int i;
        while (-1 != (i = in.read())) {
            if (i == CR) {
                i = in.read();
            }
            if (i == LF) {
                break;
            }
            in.unread(i);

            String[] requestHeaderField = readRequestHeaderField();
            requestHeader.put(requestHeaderField[0], requestHeaderField[1]);
        }
        return requestHeader;
    }

    protected String[] readRequestHeaderField() throws IOException {
        ByteArrayOutputStream name = new ByteArrayOutputStream();
        int i;
        while (-1 != (i = in.read())) {
            if (i == ':') {
                break;
            }
            name.write(i);
        }

        //fieldに先行するLWSを読み飛ばす
        i = in.read();
        if (i == CR) {
            i = in.read();
        }
        if (i == LF) {
            i = in.read();
        }
        if ((i != ' ' && i != HT) == false) {
            while (-1 != (i = in.read())) {
                if (i != ' ' && i != HT) {
                    break;
                }
            }
        }

        ByteArrayOutputStream value = new ByteArrayOutputStream();
        value.write(i);
        while (-1 != (i = in.read())) {
            if (i == CR) {
                i = in.read();
            }
            if (i == LF) {
                i = in.read();
                if (i != ' ' && i != HT) {
                    in.unread(i);

                    //field-nameは大文字・小文字を区別しない
                    //ここでは全て小文字にしておく
                    return new String[] {
                        name.toString().toLowerCase(),
                        value.toString() };
                }
                //field-valueはLWSを行頭につけると改行可能
                while (-1 != (i = in.read())) {
                    if (i != ' ' && i != HT) {
                        break;
                    }
                }
            }
            value.write(i);
        }
        throw new IllegalStateException();
    }

    public byte[] readEntityBody(int contentLength) throws IOException {
        int i;
        ByteArrayOutputStream entityBody = new ByteArrayOutputStream();
        while (-1 != (i = in.read())) {
            entityBody.write(i);
            if (entityBody.size() == contentLength) {
                return entityBody.toByteArray();
            }
        }
        throw new IllegalStateException();
    }
}
