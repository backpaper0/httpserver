package httpserver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class HttpServer {
    String host;
    int port;
    HttpHandler handler;
    Worker worker;
    AtomicInteger counter = new AtomicInteger(0);
    Worker[] workers;

    public HttpServer(String host, int port, HttpHandler handler) {
        this.host = host;
        this.port = port;
        this.handler = handler;
        this.worker = new Worker();
        this.workers = IntStream.range(0, Runtime.getRuntime().availableProcessors() - 1)
                .mapToObj(i -> new Worker()).toArray(Worker[]::new);
    }

    public void start() throws Exception {
        Arrays.stream(workers).forEach(Thread::start);
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        ssc.bind(new InetSocketAddress(host, port));
        worker.register(ssc, SelectionKey.OP_ACCEPT, new AcceptHandler());
        worker.start();
    }

    public void stop() throws IOException {
        worker.shutdown();
        for (Worker worker : workers) {
            worker.shutdown();
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
            sc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            sc.socket().setReuseAddress(true);
            sc.configureBlocking(false);
            workers[counter.getAndIncrement() % workers.length].register(sc, SelectionKey.OP_READ,
                    new IOHandler());
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

    class Worker extends Thread {

        final Selector selector;
        final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        final AtomicBoolean running = new AtomicBoolean(true);

        public Worker() {
            try {
                selector = Selector.open();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void run() {
            try {
                while (running.get()) {
                    int count = selector.select();
                    if (count > 0) {
                        for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it
                                .hasNext();) {
                            SelectionKey key = it.next();
                            it.remove();
                            Handler h = (Handler) key.attachment();
                            h.handle(key);
                        }
                    }
                    Runnable task;
                    while ((task = queue.poll()) != null) {
                        task.run();
                    }
                }
            } catch (Exception e) {
                //TODO
                new Exception(Thread.currentThread().getName(), e).printStackTrace();
            } finally {
                try {
                    selector.close();
                } catch (IOException e) {
                    //TODO
                    e.printStackTrace();
                }
            }
        }

        public void register(AbstractSelectableChannel channel, int op, Handler handler) {
            queue.add(() -> {
                try {
                    channel.register(selector, op, handler);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    new Exception(Thread.currentThread().getName(), e).printStackTrace();
                }
            });
            selector.wakeup();
        }

        public void shutdown() throws IOException {
            running.set(false);
            for (SelectionKey key : selector.keys()) {
                key.channel().close();
            }
        }
    }
}
