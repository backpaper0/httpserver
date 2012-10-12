package example;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import org.junit.Test;

public class ChunkedExample {

    private static final String CRLF = "\r\n";

    @Test
    public void transferEncoding_chunked() throws Exception {
        Thread server = new Thread() {

            @Override
            public void run() {
                try (ServerSocket server = new ServerSocket(8080)) {
                    while (Thread.currentThread().isInterrupted() == false) {
                        try (Socket client = server.accept();
                                RequestInputStream in =
                                    new RequestInputStream(
                                        client.getInputStream());
                                ResponseOutputStream out =
                                    new ResponseOutputStream(
                                        client.getOutputStream())) {
                            int b;
                            boolean prepare = false;
                            while (-1 != (b = in.readAndTruncateCR())) {
                                if (b == '\n') {
                                    if (prepare) {
                                        break;
                                    } else {
                                        prepare = true;
                                    }
                                } else if (prepare) {
                                    prepare = false;
                                }
                            }
                            out.write("HTTP/1.1 200 OK".getBytes());
                            out.write(CRLF.getBytes());
                            out.write("Transfer-Encoding: chunked".getBytes());
                            out.write(CRLF.getBytes());
                            out.write(CRLF.getBytes());
                            out.writeChunk("Hello".getBytes());
                            out.writeChunk(", ".getBytes());
                            out.writeChunk("world!".getBytes());
                            out.writeLastChunk();
                            out.flush();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        };
        server.start();

        HttpURLConnection con =
            (HttpURLConnection) new URL("http://localhost:8080/hello")
                .openConnection();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = con.getInputStream()) {
            byte[] b = new byte[8192];
            int i;
            while (-1 != (i = in.read(b, 0, b.length))) {
                out.write(b, 0, i);
            }
        }
        assertThat(out.toString(), is("Hello, world!"));
    }

    static class RequestInputStream extends FilterInputStream {

        public RequestInputStream(InputStream in) {
            super(in);
        }

        /*
         * CRは無視して読み込む
         */
        public int readAndTruncateCR() throws IOException {
            int b;
            while (-1 != (b = in.read()) && b == '\r') {
            }
            return b;
        }

    }

    static class ResponseOutputStream extends FilterOutputStream {

        public ResponseOutputStream(OutputStream out) {
            super(out);
        }

        /*
         * チャンクは
         * 
         * 1. チャンクサイズを16進数で
         * 2. CRLF
         * 3. チャンク
         * 4. CRLF
         * 
         * で一塊
         */
        public void writeChunk(byte[] chunk) throws IOException {
            out.write(Integer.toHexString(chunk.length).getBytes());
            out.write(CRLF.getBytes());
            out.write(chunk);
            out.write(CRLF.getBytes());
        }

        /*
         * チャンク終了の印は
         * 
         * 1. 0を書き出す(サイズ0のチャンク的な)
         * 2. CRLF
         * 3. CRLF
         */
        public void writeLastChunk() throws IOException {
            out.write(Integer.toHexString(0).getBytes());
            out.write(CRLF.getBytes());
            out.write(CRLF.getBytes());
        }

    }

}
