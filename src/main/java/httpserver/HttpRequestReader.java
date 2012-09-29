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
        ByteArrayOutputStream data = new ByteArrayOutputStream();
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
                System.out.println("[Request line] " + data);
                break;
            }
            data.write(i);
        }
        String[] requestLine = data.toString().split(" ");
        return requestLine;
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
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        int i;
        while (-1 != (i = in.read())) {
            if (i == ':') {
                break;
            }
            data.write(i);
        }
        //field-nameは大文字・小文字を区別しない
        //ここでは全て小文字にしておく
        String name = data.toString().toLowerCase();

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

        data = new ByteArrayOutputStream();
        data.write(i);
        while (-1 != (i = in.read())) {
            if (i == CR) {
                i = in.read();
            }
            if (i == LF) {
                i = in.read();
                if (i != ' ' && i != HT) {
                    in.unread(i);

                    String value = data.toString();
                    return new String[] { name, value };
                }
                //field-valueはLWSを行頭につけると改行可能
                while (-1 != (i = in.read())) {
                    if (i != ' ' && i != HT) {
                        break;
                    }
                }
            }
            data.write(i);
        }
        throw new IllegalStateException();
    }

    public byte[] readEntityBody(int contentLength) throws IOException {
        int i;
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        while (-1 != (i = in.read())) {
            data.write(i);
            if (data.size() == contentLength) {
                return data.toByteArray();
            }
        }
        throw new IllegalStateException();
    }
}
