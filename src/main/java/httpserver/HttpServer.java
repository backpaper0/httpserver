package httpserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer implements AutoCloseable {

    private static final byte[] CRLF = "\r\n".getBytes();

    private final ServerSocket server;

    private final ExecutorService executor;

    private final HttpRequestHandler httpRequestHandler;

    public HttpServer(HttpRequestHandler httpRequestHandler, int port)
            throws IOException {
        this.server = new ServerSocket(port);
        this.httpRequestHandler = httpRequestHandler;
        this.executor = Executors.newSingleThreadExecutor();
        this.executor.submit(new Callable<Void>() {

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

        Object[] response =
            httpRequestHandler.handleRequest(
                client.getOutputStream(),
                requestLine,
                requestHeader,
                requestEntity);
        String httpVersion = (String) response[0];
        Integer statusCode = (Integer) response[1];
        String reasonPhrase = (String) response[2];
        @SuppressWarnings("unchecked")
        Map<String, Object> responseHeader = (Map<String, Object>) response[3];
        try (InputStream messageBodyInputStream = (InputStream) response[4]) {

            OutputStream out = client.getOutputStream();

            //ステータスライン
            out.write(httpVersion.getBytes());
            out.write(' ');
            out.write(statusCode.toString().getBytes());
            out.write(' ');
            out.write(reasonPhrase.getBytes());
            out.write(CRLF);

            //レスポンスヘッダ
            DateFormat df =
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
            df.setTimeZone(TimeZone.getTimeZone("GMT"));

            LinkedHashMap<String, Object> header = new LinkedHashMap<>();

            //general-header
            header.put("Connection", "close");
            header.put("Date", df.format(new Date()));

            //response-header
            header.put("Server", "SimpleHttpServer/0.1");

            header.putAll(responseHeader);

            //entity-header
            ByteArrayOutputStream messageBodyOutputStream =
                new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int i;
            while (-1 != (i = messageBodyInputStream.read(b, 0, b.length))) {
                messageBodyOutputStream.write(b, 0, i);
            }
            byte[] messageBody = messageBodyOutputStream.toByteArray();
            responseHeader.put("Content-Length", messageBody.length);

            for (Entry<String, Object> field : header.entrySet()) {
                out.write(field.getKey().getBytes());
                out.write(": ".getBytes());
                out.write(field.getValue().toString().getBytes());
                out.write(CRLF);
            }

            out.write(CRLF);

            //メッセージボディ
            out.write(messageBody);

            out.flush();
        }
    }

}
