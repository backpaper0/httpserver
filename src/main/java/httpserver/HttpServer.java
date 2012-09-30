package httpserver;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer implements AutoCloseable {

    private final int port;

    private final ServerSocket server;

    private final ExecutorService executor;

    private final Path webappDir;

    private final Map<String, String> contentTypes;

    public HttpServer(Path webappDir, int port) throws IOException {
        this.webappDir = webappDir;
        this.port = port;
        this.server = new ServerSocket();
        this.executor = Executors.newSingleThreadExecutor();
        this.contentTypes = new HashMap<>();

        this.contentTypes.put(".txt", "text/plain");
        this.contentTypes.put(".json", "application/json");
    }

    public void start() {
        executor.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                process();
                return null;
            }
        });
        System.out.println("[Server started]");
    }

    @Override
    public void close() throws IOException {
        System.out.println("[Server closing]");
        server.close();
        executor.shutdownNow();
    }

    private void process() throws IOException {

        SocketAddress endpoint = new InetSocketAddress(port);
        server.bind(endpoint);

        while (Thread.currentThread().isInterrupted() == false
            && server.isClosed() == false) {

            try (Socket client = server.accept()) {
                handleClientSocket(client);
            }
        }
    }

    private void handleClientSocket(Socket client) throws IOException {

        /* 
         * リクエストを読む 
         */
        HttpRequestReader reader =
            new HttpRequestReader(client.getInputStream());

        //リクエストラインを読んで標準出力に書き出す 
        String[] requestLine = reader.readRequestLine();

        //リクエストヘッダを読む
        Map<String, String> requestHeader = reader.readRequestHeader();
        System.out.println("[Request header] " + requestHeader);

        byte[] requestEntity = null;
        if (requestHeader.containsKey("content-length")) {
            int contentLength =
                Integer.parseInt(requestHeader.get("content-length"));
            requestEntity = reader.readEntityBody(contentLength);
        }

        handleRequest(
            client.getOutputStream(),
            requestLine,
            requestHeader,
            requestEntity);
    }

    private void handleRequest(OutputStream responseStream,
            String[] requestLine, Map<String, String> requestHeader,
            byte[] requestEntity) throws IOException,
            UnsupportedEncodingException {

        //ちょくちょく使うのでインスタンス化しとく
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));

        if (requestLine[0].equals("GET") == false
            && requestLine[0].equals("POST") == false) {
            //今回はGETリクエスト、POSTリクエスト以外は扱わない
            System.out.println(requestLine[0] + "は扱えないメソッド");
            String content = "501 Not Implemented";
            Writer out = new OutputStreamWriter(responseStream);
            out.write("HTTP/1.0 501 Not Implemented\r\n");

            //general-header
            out.write("Connection: close\r\n");
            out.write("Date: " + df.format(new Date()) + "\r\n");

            //response-header
            out.write("Server: SimpleHttpServer/0.1\r\n");

            //entity-header
            out.write("Content-Length: "
                + content.getBytes("UTF-8").length
                + "\r\n");
            out.write("Content-Type: text/plain; charset=UTF-8\r\n");
            out.write("\r\n");
            out.write(content);
            out.flush();
            return;
        }

        if (requestLine[0].equals("POST")) {
            //POSTリクエストは取りあえずリクエストエンティティを
            //そのまま返す実装にしておく
            String contentType = requestHeader.get("content-type");

            /* 
             * レスポンスを書く 
             */
            Writer out = new OutputStreamWriter(responseStream);

            //ステータスライン 
            out.write("HTTP/1.0 200 OK\r\n");

            //レスポンスヘッダ 

            //general-header
            out.write("Connection: close\r\n");
            out.write("Date: " + df.format(new Date()) + "\r\n");

            //response-header
            out.write("Server: SimpleHttpServer/0.1\r\n");

            //entity-header
            out.write("Content-Length: " + requestEntity.length + "\r\n");
            out.write("Content-Type: " + contentType + "\r\n");

            out.write("\r\n");
            out.flush();

            //エンティティボディ 
            responseStream.write(requestEntity);
            responseStream.flush();
            return;
        }

        /*
         * リクエストを解析（というほどのことはしていないが）
         */
        //Request-URI は abs_path であることを前提にしておく
        Path requestPath = webappDir.resolve(requestLine[1].substring(1));

        if (Files.notExists(requestPath)) {
            //ファイルがなければ404
            System.out.println(requestPath + "が見つからない");
            String content = "404 Not Found";
            Writer out = new OutputStreamWriter(responseStream);
            out.write("HTTP/1.0 404 Not Found\r\n");

            //general-header
            out.write("Connection: close\r\n");
            out.write("Date: " + df.format(new Date()) + "\r\n");

            //response-header
            out.write("Server: SimpleHttpServer/0.1\r\n");

            //entity-header
            out.write("Content-Length: "
                + content.getBytes("UTF-8").length
                + "\r\n");
            out.write("Content-Type: text/plain; charset=UTF-8\r\n");
            out.write("\r\n");

            out.write(content);
            out.flush();
            return;
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
        Writer out = new OutputStreamWriter(responseStream);

        //ステータスライン 
        out.write("HTTP/1.0 200 OK\r\n");

        //レスポンスヘッダ 

        //general-header
        out.write("Connection: close\r\n");
        out.write("Date: " + df.format(new Date()) + "\r\n");

        //response-header
        out.write("Server: SimpleHttpServer/0.1\r\n");

        //entity-header
        out.write("Content-Length: " + fileContent.length + "\r\n");
        out.write("Content-Type: " + contentType + "; charset=UTF-8\r\n");
        out.write("Last-Modified: " + lastModified + "\r\n");

        out.write("\r\n");
        out.flush();

        //エンティティボディ 
        responseStream.write(fileContent);
        responseStream.flush();
    }
}
