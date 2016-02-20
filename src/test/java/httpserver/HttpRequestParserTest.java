package httpserver;

import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;

public class HttpRequestParserTest {

    @Test
    public void post() throws Exception {
        StringBuilder buf = new StringBuilder();
        buf.append("POST / HTTP/1.1\r\n");
        buf.append("Host: localhost:8080\r\n");
        buf.append("User-Agent: curl/7.43.0\r\n");
        buf.append("Accept: */*\r\n");
        buf.append("Content-Length: 25\r\n");
        buf.append("Content-Type: application/x-www-form-urlencoded\r\n");
        buf.append("\r\n");
        buf.append("greeting=Hello&name=world");
        HttpRequestParser parser = new HttpRequestParser();
        ByteBuffer in = ByteBuffer.wrap(buf.toString().getBytes());
        boolean parsed = parser.parse(in);
        assertThat(parsed).isTrue();
        HttpRequest request = parser.build();
        assertThat(request.method).isEqualTo("POST");
        assertThat(request.requestTarget).isEqualTo("/");
        assertThat(request.httpVersion).isEqualTo("HTTP/1.1");
        assertThat(request.headers).hasSize(5)
                .containsEntry("Host", Arrays.asList("localhost:8080"))
                .containsEntry("User-Agent", Arrays.asList("curl/7.43.0"))
                .containsEntry("Accept", Arrays.asList("*/*"))
                .containsEntry("Content-Length", Arrays.asList("25"))
                .containsEntry("Content-Type", Arrays.asList("application/x-www-form-urlencoded"));
        assertThat(request.contentLength).isEqualTo(25);
        assertThat(request.entity)
                .isEqualTo(ByteBuffer.wrap("greeting=Hello&name=world".getBytes()));
    }

    @Test
    public void get() throws Exception {
        StringBuilder buf = new StringBuilder();
        buf.append("GET /?text=HelloWorld HTTP/1.1\r\n");
        buf.append("Host: localhost:8080\r\n");
        buf.append("User-Agent: curl/7.43.0\r\n");
        buf.append("Accept: */*\r\n");
        buf.append("\r\n");
        HttpRequestParser parser = new HttpRequestParser();
        ByteBuffer in = ByteBuffer.wrap(buf.toString().getBytes());
        boolean parsed = parser.parse(in);
        assertThat(parsed).isTrue();
        HttpRequest request = parser.build();
        assertThat(request.method).isEqualTo("GET");
        assertThat(request.requestTarget).isEqualTo("/?text=HelloWorld");
        assertThat(request.httpVersion).isEqualTo("HTTP/1.1");
        assertThat(request.headers).hasSize(3)
                .containsEntry("Host", Arrays.asList("localhost:8080"))
                .containsEntry("User-Agent", Arrays.asList("curl/7.43.0"))
                .containsEntry("Accept", Arrays.asList("*/*"));
        assertThat(request.contentLength).isEqualTo(-1);
        assertThat(request.entity).isNull();
    }

    @Test
    public void post_partial() throws Exception {
        StringBuilder buf = new StringBuilder();
        buf.append("POST / HTTP/1.1\r\n");
        buf.append("Host: localhost:8080\r\n");
        buf.append("User-Agent: curl/7.43.0\r\n");
        buf.append("Accept: */*\r\n");
        buf.append("Content-Length: 25\r\n");
        buf.append("Content-Type: application/x-www-form-urlencoded\r\n");
        buf.append("\r\n");
        buf.append("greeting=Hello&name=world");
        HttpRequestParser parser = new HttpRequestParser();
        byte[] bytes = buf.toString().getBytes();
        assertThat(parser.parse(ByteBuffer.wrap(bytes, 0, 10))).isFalse();
        assertThat(parser.parse(ByteBuffer.wrap(bytes, 10, 10))).isFalse();
        assertThat(parser.parse(ByteBuffer.wrap(bytes, 20, 10))).isFalse();
        assertThat(parser.parse(ByteBuffer.wrap(bytes, 30, 10))).isFalse();
        assertThat(parser.parse(ByteBuffer.wrap(bytes, 40, 10))).isFalse();
        assertThat(parser.parse(ByteBuffer.wrap(bytes, 50, bytes.length - 50))).isTrue();
        HttpRequest request = parser.build();
        assertThat(request.method).isEqualTo("POST");
        assertThat(request.requestTarget).isEqualTo("/");
        assertThat(request.httpVersion).isEqualTo("HTTP/1.1");
        assertThat(request.headers).hasSize(5)
                .containsEntry("Host", Arrays.asList("localhost:8080"))
                .containsEntry("User-Agent", Arrays.asList("curl/7.43.0"))
                .containsEntry("Accept", Arrays.asList("*/*"))
                .containsEntry("Content-Length", Arrays.asList("25"))
                .containsEntry("Content-Type", Arrays.asList("application/x-www-form-urlencoded"));
        assertThat(request.contentLength).isEqualTo(25);
        assertThat(request.entity)
                .isEqualTo(ByteBuffer.wrap("greeting=Hello&name=world".getBytes()));
    }
}
