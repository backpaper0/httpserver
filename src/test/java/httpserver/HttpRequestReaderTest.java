package httpserver;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;

import org.junit.Test;

public class HttpRequestReaderTest {

    private static final String CRLF = "\r\n";

    @Test
    public void リクエストラインを読む() throws Exception {
        String testData = "GET /foo HTTP/1.1" + CRLF;
        byte[] b = testData.getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        HttpRequestReader reader = new HttpRequestReader(in);

        String[] requestLine = reader.readRequestLine();

        assertThat(requestLine[0], is("GET"));
        assertThat(requestLine[1], is("/foo"));
        assertThat(requestLine[2], is("HTTP/1.1"));
    }

    @Test
    public void SPとHTで区切られたリクエストラインを読む() throws Exception {
        String testData = "GET \t/foo \tHTTP/1.1" + CRLF;
        byte[] b = testData.getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        HttpRequestReader reader = new HttpRequestReader(in);

        String[] requestLine = reader.readRequestLine();

        assertThat(requestLine[0], is("GET"));
        assertThat(requestLine[1], is("/foo"));
        assertThat(requestLine[2], is("HTTP/1.1"));
    }

    @Test
    public void ヘッダフィールドを読む() throws Exception {
        String testData = "hoge: foobar" + CRLF;
        byte[] b = testData.getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        HttpRequestReader reader = new HttpRequestReader(in);

        String[] requestHeaderField = reader.readRequestHeaderField();

        assertThat(requestHeaderField[0], is("hoge"));
        assertThat(requestHeaderField[1], is("foobar"));
    }

    @Test
    public void 二行にまたがったヘッダフィールドを読む() throws Exception {
        String testData = "hoge: foo" + CRLF + " bar" + CRLF;
        byte[] b = testData.getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        HttpRequestReader reader = new HttpRequestReader(in);

        String[] requestHeaderField = reader.readRequestHeaderField();

        assertThat(requestHeaderField[0], is("hoge"));
        assertThat(requestHeaderField[1], is("foobar"));
    }
}
