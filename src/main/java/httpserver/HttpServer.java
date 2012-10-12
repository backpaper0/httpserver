package httpserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer implements AutoCloseable {

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
        HttpResponseWriter writer =
            new Http11ResponseWriterImpl(client.getOutputStream());
        writer.write(response);
    }
}
