package httpserver;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class HttpServer {
    String host;
    int port;
    HttpHandler handler;

    public HttpServer(String host, int port, HttpHandler handler) {
        this.host = host;
        this.port = port;
        this.handler = handler;
    }

    public void start() throws Exception {
        try (Selector sel = Selector.open(); ServerSocketChannel ssc = ServerSocketChannel.open()) {
            ssc.configureBlocking(false);
            ssc.socket().setReuseAddress(true);
            ssc.bind(new InetSocketAddress(host, port));
            ssc.register(sel, SelectionKey.OP_ACCEPT, new AcceptHandler());
            while (sel.select() > 0) {
                for (Iterator<SelectionKey> it = sel.selectedKeys().iterator(); it.hasNext();) {
                    SelectionKey key = it.next();
                    it.remove();
                    Handler h = (Handler) key.attachment();
                    h.handle(key);
                }
            }
        }
    }

    interface Handler {

        void handle(SelectionKey key) throws Exception;
    }

    class AcceptHandler implements Handler {

        @Override
        public void handle(SelectionKey key) throws Exception {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel sc = ssc.accept();
            sc.configureBlocking(false);
            sc.register(key.selector(), SelectionKey.OP_READ, new IOHandler());
        }
    }

    class IOHandler implements Handler {

        HttpRequestParser parser = new HttpRequestParser();
        private ByteBuffer buf;

        @Override
        public void handle(SelectionKey key) throws Exception {
            SocketChannel sc = (SocketChannel) key.channel();
            int i = -1;
            if (key.isReadable()) {
                ByteBuffer b = ByteBuffer.allocate(5);
                i = sc.read(b);
                if (i > -1) {
                    b.flip();
                    boolean parsed = parser.parse(b);
                    if (parsed) {
                        HttpRequest request = parser.build();
                        HttpResponse response = handler.handle(request);
                        HttpResponseFormatter formatter = new HttpResponseFormatter();
                        buf = formatter.format(response);
                        if ((key.interestOps() & SelectionKey.OP_WRITE) != SelectionKey.OP_WRITE) {
                            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                        }
                    }
                } else {
                    //key.cancel();
                    key.interestOps(key.interestOps() ^ SelectionKey.OP_READ);
                }
            }
            if (key.isWritable()) {
                sc.write(buf);
                if (buf.hasRemaining() == false) {
                    //key.cancel();
                    key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
                }
            }
        }
    }
}
