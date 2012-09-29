package httpserver;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;

import org.junit.Test;

public class HttpRequestReaderTest {

    private static final String CRLF = "\r\n";

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
