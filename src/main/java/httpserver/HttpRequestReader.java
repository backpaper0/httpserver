package httpserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestReader {

    private static final char CR = '\r';

    private static final char LF = '\n';

    private final InputStream in;

    public HttpRequestReader(InputStream in) {
        this.in = in;
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

            ByteArrayOutputStream data = new ByteArrayOutputStream();
            data.write(i);
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
            if ((i != ' ' && i != '\t') == false) {
                while (-1 != (i = in.read())) {
                    if (i != ' ' && i != '\t') {
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
                    //field-valueはLWSを行頭につけると改行可能だけど
                    //今回はそこまで解析しない
                    String field = data.toString();
                    requestHeader.put(name, field);
                    break;
                }
                data.write(i);
            }
        }
        return requestHeader;
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
