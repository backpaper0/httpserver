package httpserver;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
    public void GETリクエストでHelloWorld() throws Exception {
        URL url = new URL("http://localhost:8080/hello.txt");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        int statusCode = con.getResponseCode();
        assertThat(statusCode, is(200));

        System.out.println(con.getHeaderFields());
        assertThat(con.getContentType(), is("text/plain; charset=UTF-8"));

        try (InputStream in = con.getInputStream()) {
            String response = readAll(in);
            assertThat(response, is("Hello, world!"));
        }
    }

    /**
     * テスト毎にHTTPサーバを立てては落とす賢いヤツ
     */
    @Rule
    public TestRule serverController = new TestRule() {

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {

                @Override
                public void evaluate() throws Throwable {
                    try (HttpServer server =
                        new HttpServerStarter(
                            new MockHttpRequestHandler(),
                            8080).start()) {
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
            response.setHttpVersion("HTTP/1.0");
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
            response.setMessageBody(new ByteArrayInputStream("Hello, world!"
                .getBytes()));
            return response;
        }

    }

}
