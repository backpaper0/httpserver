package httpserver;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class HttpServerTest {

    @Test
    public void GETリクエストでtxtファイルを貰う() throws Exception {
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

    @Test
    public void GETリクエストでjsonファイルを貰う() throws Exception {
        URL url = new URL("http://localhost:8080/hello.json");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        int statusCode = con.getResponseCode();
        assertThat(statusCode, is(200));

        System.out.println(con.getHeaderFields());
        assertThat(con.getContentType(), is("application/json; charset=UTF-8"));

        try (InputStream in = con.getInputStream()) {
            String response = readAll(in);
            assertThat(response, is("{ \"message\" : \"Hello, world!\" }"));
        }
    }

    @Test
    public void ファイルが見つからなかったら404NotFound() throws Exception {
        URL url = new URL("http://localhost:8080/hello.unknown");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        int statusCode = con.getResponseCode();
        assertThat(statusCode, is(404));

        System.out.println(con.getHeaderFields());
        assertThat(con.getContentType(), is("text/plain; charset=UTF-8"));

        try (InputStream in = con.getErrorStream()) {
            String response = readAll(in);
            assertThat(response, is("404 Not Found"));
        }
    }

    @Test
    public void POSTリクエストでエンティティをそのまま返して貰う() throws Exception {
        URL url = new URL("http://localhost:8080/echo");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
        try (OutputStream out = con.getOutputStream()) {
            out.write("Hello, POST method!".getBytes());
        }

        int statusCode = con.getResponseCode();
        assertThat(statusCode, is(200));

        System.out.println(con.getHeaderFields());
        assertThat(con.getContentType(), is("text/plain; charset=UTF-8"));

        try (InputStream in = con.getInputStream()) {
            String response = readAll(in);
            assertThat(response, is("Hello, POST method!"));
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
                        new HttpServer(Paths.get("src", "main", "webapp"), 8080)) {
                        server.start();
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

}
