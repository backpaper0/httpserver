package httpserver;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.internal.matchers.IsCollectionContaining.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class HttpServerTest {

    @Test
    @HandlerWith(MockHttpRequestHandler.class)
    public void GETリクエストでHelloWorld() throws Exception {
        URL url = new URL("http://localhost:8080/hello.txt");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        int statusCode = con.getResponseCode();
        assertThat(statusCode, is(200));

        System.out.println(con.getHeaderFields());
        assertThat(con.getContentType(), is("text/plain; charset=UTF-8"));

        assertThat(con.getContentLength(), is(13));
        assertThat(
            con.getHeaderFields().keySet(),
            not(hasItem("Transfer-Encoding")));

        try (InputStream in = con.getInputStream()) {
            String response = readAll(in);
            assertThat(response, is("Hello, world!"));
        }
    }

    @Test
    @HandlerWith(ChunkedResponse.class)
    public void チャンク転送() throws Exception {
        URL url = new URL("http://localhost:8080/hello.txt");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        int statusCode = con.getResponseCode();
        assertThat(statusCode, is(200));

        System.out.println(con.getHeaderFields());
        assertThat(con.getContentType(), is("text/plain; charset=UTF-8"));

        assertThat(
            con.getHeaderFields().keySet(),
            not(hasItem("Content-Length")));
        assertThat(con.getHeaderField("Transfer-Encoding"), is("chunked"));

        try (InputStream in = con.getInputStream()) {
            String response = readAll(in);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 30000; i++) {
                sb.append('*');
            }
            assertThat(response, is(sb.toString()));
        }
    }

    /**
     * テスト毎にHTTPサーバを立てては落とす賢いヤツ
     */
    @Rule
    public TestRule serverController = new TestRule() {

        @Override
        public Statement apply(final Statement base,
                final Description description) {
            return new Statement() {

                @Override
                public void evaluate() throws Throwable {
                    try (HttpServer server =
                        new HttpServerStarter(description
                            .getAnnotation(HandlerWith.class)
                            .value()
                            .newInstance(), 8080).start()) {
                        base.evaluate();
                    }
                }
            };
        }
    };

    private static String readAll(InputStream in) throws IOException {
        byte[] b = new byte[8192];
        int i;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (-1 != (i = in.read(b, 0, b.length))) {
            out.write(b, 0, i);
        }
        out.flush();
        out.close();
        return out.toString();
    }

    public static class MockHttpRequestHandler implements HttpRequestHandler {

        public HttpResponse handleRequest(HttpRequest request)
                throws IOException, UnsupportedEncodingException {
            HttpResponse response = new HttpResponse();
            response.setStatusCode(200);
            response.setReasonPhase("OK");
            response.getMessageHeader().put(
                "Content-Type",
                "text/plain; charset=UTF-8");
            DateFormat df =
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            response.getMessageHeader().put(
                "Last-Modified",
                df.format(new Date()));
            response.setMessageBody(new ByteArrayInputStream(getMessageBody()
                .getBytes()));
            return response;
        }

        protected String getMessageBody() {
            return "Hello, world!";
        }
    }

    public static class ChunkedResponse extends MockHttpRequestHandler {

        @Override
        protected String getMessageBody() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 30000; i++) {
                sb.append('*');
            }
            return sb.toString();
        }
    }

    @Retention(RUNTIME)
    @Target(METHOD)
    public @interface HandlerWith {

        Class<? extends HttpRequestHandler> value();
    }
}
