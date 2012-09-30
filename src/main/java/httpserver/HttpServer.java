package httpserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer implements AutoCloseable {

    private final int port;

    private final ServerSocket server;

    private final ExecutorService executor;

    private final HttpRequestHandler httpRequestHandler;

    public HttpServer(Path webappDir, int port) throws IOException {
        this.port = port;
        this.server = new ServerSocket();
        this.executor = Executors.newSingleThreadExecutor();
        this.httpRequestHandler = new HttpRequestHandler(webappDir);
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

        httpRequestHandler.handleRequest(
            client.getOutputStream(),
            requestLine,
            requestHeader,
            requestEntity);
    }

}
