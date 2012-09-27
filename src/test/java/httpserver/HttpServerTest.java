package httpserver;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class HttpServerTest {

    @Test
    public void GETリクエストを処理する() throws Exception {
        URL url = new URL("http://localhost:8080/hello");
        try (InputStream in = url.openStream()) {
            byte[] b = new byte[8192];
            int i;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (-1 != (i = in.read(b, 0, b.length))) {
                out.write(b, 0, i);
            }
            out.flush();
            out.close();
            String response = out.toString();
            assertThat(response, is("Hello, world!"));
        }
    }

    @Test
    public void POSTリクエストは未実装() throws Exception {
        URL url = new URL("http://localhost:8080/hello");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");

        int statusCode = con.getResponseCode();
        assertThat(statusCode, is(501));
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
                    try (HttpServer server = new HttpServer(8080)) {
                        server.start();
                        base.evaluate();
                    }
                }
            };
        }
    };
}
