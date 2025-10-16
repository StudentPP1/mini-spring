package com.test.server;

import com.test.http.HttpRequest;
import com.test.http.HttpResponse;
import com.test.http.HttpStatus;
import com.test.server.socket.SocketThreadPool;
import com.test.servlets.DispatcherServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class HttpServer implements AutoCloseable {
    private static final Logger log = LogManager.getLogger(HttpServer.class);
    private final int port;
    private final SocketThreadPool threadPool;
    private final DispatcherServlet dispatcherServlet;
    private volatile boolean running = true;

    public HttpServer(int port, int threads, DispatcherServlet dispatcherServlet) {
        this.port = port;
        this.threadPool = new SocketThreadPool(threads);
        this.dispatcherServlet = dispatcherServlet;
    }

    public void start() throws Exception {
        dispatcherServlet.init();
        ServerSocket serverSocket = new ServerSocket(port);
        log.debug("Servet start at port: {}", port);
        while (running) {
            Socket client = serverSocket.accept();
            log.debug("Accept client socket: {}", client);
            threadPool.execute(() -> {
                try (client) {
                    log.debug("Accept in thread: {}", Thread.currentThread().getName());
                    InputStream inputStream = client.getInputStream();
                    OutputStream outputStream = client.getOutputStream();
                    HttpRequest request = HttpRequest.build(inputStream);
                    HttpResponse response = HttpResponse.build(HttpStatus.OK);
                    dispatcherServlet.handle(request, response);
                    outputStream.write(response.toByteArray());
                    outputStream.flush();
                } catch (final Exception _) {
                    try { write500(client); } catch (final Exception _) {}
                }
            });
        }
    }

    private static void write500(Socket s) throws IOException {
        OutputStream out = s.getOutputStream();
        byte[] body = "{\"error\":\"Internal Server Error\"}".getBytes(StandardCharsets.UTF_8);
        String head = "HTTP/1.1 500 Internal Server Error\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + body.length + "\r\n" +
                "Connection: close\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }

    @Override
    public void close() {
        running = false;
        threadPool.shutdown();
        dispatcherServlet.destroy();
    }
}
