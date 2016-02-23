package httpserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HttpServer {

    private static final Logger logger = Logger.getLogger(HttpServer.class.getName());
    private final String host;
    private final int port;
    private final HttpHandler handler;
    private final Worker worker;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final List<Worker> workers;

    public HttpServer(String host, int port, HttpHandler handler) {
        this.host = host;
        this.port = port;
        this.handler = handler;
        this.worker = new Worker();
        this.workers = IntStream.range(0, Runtime.getRuntime().availableProcessors() - 1)
                .mapToObj(i -> new Worker()).collect(Collectors.toList());
    }

    public void start() throws IOException {
        logger.info(() -> "start");
        workers.forEach(Thread::start);
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        ssc.bind(new InetSocketAddress(host, port));
        worker.register(ssc, SelectionKey.OP_ACCEPT, new AcceptHandler());
        worker.start();
    }

    public void stop() {
        logger.info(() -> "stop");
        worker.shutdown();
        workers.forEach(Worker::shutdown);
    }

    interface Handler {

        void handle(SelectionKey key) throws IOException;
    }

    class AcceptHandler implements Handler {

        @Override
        public void handle(SelectionKey key) throws IOException {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel sc = ssc.accept();
            sc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            sc.configureBlocking(false);
            int index = counter.getAndIncrement() % workers.size();
            workers.get(index).register(sc, SelectionKey.OP_READ, new IOHandler());
        }
    }

    class IOHandler implements Handler {

        private final HttpRequestParser parser = new HttpRequestParser();
        private final ByteBuffer buf = ByteBuffer.allocate(8192);
        private ByteBuffer responseEntity;

        @Override
        public void handle(SelectionKey key) throws IOException {
            SocketChannel sc = (SocketChannel) key.channel();
            if (key.isReadable()) {
                int i;
                while ((i = sc.read(buf)) > 0) {
                    buf.flip();
                    boolean parsed = parser.parse(buf);
                    if (parsed) {
                        HttpRequest request = parser.build();
                        HttpResponse response;
                        try {
                            response = handler.handle(request);
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "exception in handle request", e);
                            response = createErrorResponse(e);
                        }
                        HttpResponseFormatter formatter = new HttpResponseFormatter();
                        responseEntity = formatter.format(response);
                        if ((key.interestOps() & SelectionKey.OP_WRITE) != SelectionKey.OP_WRITE) {
                            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                        }
                    }
                    buf.clear();
                }
                if (i < 0) {
                    //key.cancel();
                    key.interestOps(key.interestOps() ^ SelectionKey.OP_READ);
                }
            }
            if (key.isWritable()) {
                sc.write(responseEntity);
                if (responseEntity.hasRemaining() == false) {
                    //key.cancel();
                    key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
                }
            }
        }

        private HttpResponse createErrorResponse(Exception e) {
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Content-Type", Arrays.asList("text/plain"));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (PrintStream out = new PrintStream(baos)) {
                e.printStackTrace(out);
            }
            ByteBuffer entity = ByteBuffer.wrap(baos.toByteArray());
            return new HttpResponse(500, "Internal Server Error", headers, entity);
        }
    }

    class Worker extends Thread {

        private final Selector selector;
        private final BlockingQueue<IOAction> queue = new LinkedBlockingQueue<>();
        private final AtomicBoolean running = new AtomicBoolean(true);

        public Worker() {
            try {
                selector = Selector.open();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void run() {
            logger.info(() -> getName() + " begin");
            try {
                while (running.get()) {
                    int count = selector.select();
                    if (count > 0) {
                        Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                        while (it.hasNext()) {
                            SelectionKey key = it.next();
                            it.remove();
                            Handler h = (Handler) key.attachment();
                            h.handle(key);
                        }
                    }
                    IOAction task;
                    while ((task = queue.poll()) != null) {
                        task.act();
                    }
                }
                logger.info(() -> getName() + " end");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "exception in run", e);
            } finally {
                try {
                    for (SelectionKey key : selector.keys()) {
                        key.channel().close();
                    }
                    selector.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "exception in close", e);
                }
            }
        }

        public void register(AbstractSelectableChannel channel, int op, Handler handler) {
            queue.add(() -> channel.register(selector, op, handler));
            selector.wakeup();
        }

        public void shutdown() {
            running.set(false);
            if (selector.isOpen()) {
                selector.wakeup();
            }
        }
    }

    @FunctionalInterface
    interface IOAction {
        void act() throws IOException;
    }
}
