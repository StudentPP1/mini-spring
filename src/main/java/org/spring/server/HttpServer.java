package org.spring.server;

import org.spring.context.DefaultServletContext;
import org.spring.context.Target;
import org.spring.filter.DefaultFilterChain;
import org.spring.filter.FilterChain;
import org.spring.http.HttpRequest;
import org.spring.http.HttpResponse;
import org.spring.http.HttpStatus;
import org.spring.server.socket.SocketThreadPool;
import org.spring.utils.PropertiesUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public final class HttpServer implements AutoCloseable {
    private static final Logger log = LogManager.getLogger(HttpServer.class);
    private final int port = Integer.parseInt(PropertiesUtils.getProperty("server.port"));
    private final SocketThreadPool threadPool;
    private final DefaultServletContext context;
    private volatile boolean running = true;

    public HttpServer(DefaultServletContext context) {
        int poolSize = Integer.parseInt(PropertiesUtils.getProperty("server.thread.pool.size"));
        this.threadPool = new SocketThreadPool(poolSize);
        this.context = context;
    }

    public void start() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.debug("servet start at port: {}", port);
            while (running) {
                Socket client = serverSocket.accept();
                log.debug("accept client socket: {}", client);
                threadPool.execute(() -> {
                    try (client) {
                        log.debug("accept in thread: {}", Thread.currentThread().getName());
                        InputStream inputStream = client.getInputStream();
                        OutputStream outputStream = client.getOutputStream();
                        HttpRequest request = HttpRequest.build(inputStream);
                        HttpResponse response = HttpResponse.build(HttpStatus.OK);
                        findAndRunServlet(request, response);
                        outputStream.write(response.toByteArray());
                        outputStream.flush();
                    } catch (final Exception _) {
                        try {
                            write500(client);
                        } catch (final Exception _) {
                        }
                    }
                });
            }
        }
    }

    private void findAndRunServlet(HttpRequest request, HttpResponse response) throws Exception {
        Target target = this.context.findTarget(request.getPath());
        FilterChain chain = new DefaultFilterChain(target.filters(), target.servlet());
        chain.doFilter(request, response);
    }

    private static void write500(Socket s) throws IOException {
        OutputStream out = s.getOutputStream();
        HttpResponse response = HttpResponse.build(HttpStatus.INTERNAL_SERVER_ERROR);
        out.write(response.toByteArray());
        out.flush();
    }

    @Override
    public void close() {
        running = false;
        threadPool.shutdown();
    }
}
