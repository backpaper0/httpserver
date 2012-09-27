package httpserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer implements AutoCloseable {

    private final int port;

    private final ServerSocket server;

    private final ExecutorService executor;

    public HttpServer(int port) throws IOException {
        this.port = port;
        this.server = new ServerSocket();
        this.executor = Executors.newSingleThreadExecutor();
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
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        InputStream in = client.getInputStream();
        int i;

        //リクエストラインを読んで標準出力に書き出す 
        //今回はGETリクエストが来ることを前提にしているので変数に保持しない 
        while (-1 != (i = in.read())) {
            if (i == '\r') { //CR 
                in.read(); //LF 
                System.out.println("[Request line] " + data);
                break;
            }
            data.write(i);
        }

        if (data.toString().startsWith("GET ") == false) {
            //今回はGETリクエスト以外は扱わない
            Writer out = new OutputStreamWriter(client.getOutputStream());
            out.write("HTTP/1.0 501 Not Implemented\r\n");
            out.write("\r\n");
            out.flush();
            return;
        }

        //リクエストヘッダを読んで標準出力に書き出す 
        //今回はリクエストヘッダを利用しないので変数に保持しない 
        data = new ByteArrayOutputStream();
        while (-1 != (i = in.read())) {
            if (i == '\r') { //CR 
                in.read(); //LF 
                System.out.println("[Request header] " + data);
                if ((i = in.read()) == '\r') { //CR 
                    in.read(); //LF 
                    break;
                }
                data = new ByteArrayOutputStream();
            }
            data.write(i);
        }

        /* 
         * レスポンスを書く 
         */
        Writer out = new OutputStreamWriter(client.getOutputStream());

        //ステータスライン 
        out.write("HTTP/1.0 200 OK\r\n");

        //レスポンスヘッダ 
        out.write("Content-Type: text/plain; charset=UTF-8\r\n");

        out.write("\r\n");

        //エンティティボディ 
        out.write("Hello, world!");
        out.flush();
    }
}
