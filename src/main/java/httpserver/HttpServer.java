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
        HttpRequest request = new HttpRequest();
        HttpRequestReader reader =
            new HttpRequestReader(client.getInputStream());

        //リクエストラインを読んで標準出力に書き出す 
        request.setRequestLine(reader.readRequestLine());

        //リクエストヘッダを読む
        request.setRequestHeader(reader.readRequestHeader());
        System.out.println("[Request header] " + request.getRequestHeader());

        if (request.getRequestHeader().containsKey("content-length")) {
            int contentLength =
                Integer.parseInt(request.getRequestHeader().get(
                    "content-length"));
            request.setRequestEntity(reader.readEntityBody(contentLength));
        }

        HttpResponse response = httpRequestHandler.handleRequest(request);
        String httpVersion = response.getHttpVersion();
        Integer statusCode = response.getStatusCode();
        String reasonPhrase = response.getReasonPhase();
        Map<String, Object> responseHeader = response.getMessageHeader();
        try (InputStream messageBodyInputStream = response.getMessageBody()) {

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
