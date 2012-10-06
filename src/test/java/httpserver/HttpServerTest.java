package httpserver;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

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
                        new HttpServerStarter(new MockHttpRequestHandler(
                            Paths.get("src", "main", "webapp")), 8080).start()) {
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

        private final Path webappDir;

        private final Map<String, String> contentTypes;

        public MockHttpRequestHandler(Path webappDir) {
            this.webappDir = webappDir;
            this.contentTypes = new HashMap<>();

            this.contentTypes.put(".txt", "text/plain");
            this.contentTypes.put(".json", "application/json");
        }

        public HttpResponse handleRequest(OutputStream responseStream,
                HttpRequest request) throws IOException,
                UnsupportedEncodingException {

            //ちょくちょく使うのでインスタンス化しとく
            DateFormat df =
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
            df.setTimeZone(TimeZone.getTimeZone("GMT"));

            if (request.getRequestLine()[0].equals("GET") == false
                && request.getRequestLine()[0].equals("POST") == false) {
                //今回はGETリクエスト、POSTリクエスト以外は扱わない
                System.out.println(request.getRequestLine()[0] + "は扱えないメソッド");

                HttpResponse response = new HttpResponse();
                response.setHttpVersion("HTTP/1.0");
                response.setStatusCode(501);
                response.setReasonPhase("Not Implemented");
                response.getMessageHeader().put(
                    "Content-Type",
                    "text/plain; charset=UTF-8");
                response.setMessageBody(new ByteArrayInputStream(
                    "501 Not Implemented".getBytes("UTF-8")));
                return response;
            }

            if (request.getRequestLine()[0].equals("POST")) {
                //POSTリクエストは取りあえずリクエストエンティティを
                //そのまま返す実装にしておく
                String contentType =
                    request.getRequestHeader().get("content-type");

                /* 
                 * レスポンスを書く 
                 */
                HttpResponse response = new HttpResponse();
                response.setHttpVersion("HTTP/1.0");
                response.setStatusCode(200);
                response.setReasonPhase("OK");
                response.getMessageHeader().put("Content-Type", contentType);
                response.setMessageBody(new ByteArrayInputStream(request
                    .getRequestEntity()));
                return response;
            }

            /*
             * リクエストを解析（というほどのことはしていないが）
             */
            //Request-URI は abs_path であることを前提にしておく
            Path requestPath =
                webappDir.resolve(request.getRequestLine()[1].substring(1));

            if (Files.notExists(requestPath)) {
                //ファイルがなければ404
                System.out.println(requestPath + "が見つからない");

                HttpResponse response = new HttpResponse();
                response.setHttpVersion("HTTP/1.0");
                response.setStatusCode(404);
                response.setReasonPhase("Not Found");
                response.getMessageHeader().put(
                    "Content-Type",
                    "text/plain; charset=UTF-8");
                response.setMessageBody(new ByteArrayInputStream(
                    "404 Not Found".getBytes("UTF-8")));
                return response;
            }

            /*
             * レスポンスの準備
             */
            String lastModified =
                df.format(new Date(Files
                    .getLastModifiedTime(requestPath)
                    .toMillis()));

            byte[] fileContent = Files.readAllBytes(requestPath);

            String contentType = null;
            String fileName = requestPath.getFileName().toString();
            int index = fileName.lastIndexOf('.');
            if (index > -1) {
                String extension = fileName.substring(index);
                contentType = contentTypes.get(extension);
            }
            //Content-Typeが不明なファイルはoctet-streamにしちゃう
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            /* 
             * レスポンスを書く 
             */
            HttpResponse response = new HttpResponse();
            response.setHttpVersion("HTTP/1.0");
            response.setStatusCode(200);
            response.setReasonPhase("OK");
            response.getMessageHeader().put(
                "Content-Type",
                contentType + "; charset=UTF-8");
            response.getMessageHeader().put("Last-Modified", lastModified);
            response.setMessageBody(new ByteArrayInputStream(fileContent));
            return response;
        }

    }

}
