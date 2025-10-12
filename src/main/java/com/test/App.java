package com.test;

import com.test.controller.CreateNoteController;
import com.test.dispatcher.Dispatcher;
import com.test.exception.DefaultExceptionResolver;
import com.test.filter.LoggingFilter;
import com.test.handler.ControllerMapping;
import com.test.handler.HandlerMapping;
import com.test.http.HttpRequest;
import com.test.http.HttpResponse;
import com.test.socketPool.SocketThreadPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

public class App {
    private static final int SERVER_PORT = 8080;
    private static final int THREAD_POOL_SIZE = 10;
    private static final HandlerMapping HANDLER_MAPPING = new ControllerMapping()
            .addPostHandler("/note/create", new CreateNoteController());
    private static final Dispatcher DISPATCHER = new Dispatcher(
            HANDLER_MAPPING,
            List.of(new LoggingFilter()),
            new DefaultExceptionResolver()
    );
    private static final Logger log = LogManager.getLogger(App.class);

    private static final SocketThreadPool POOL = new SocketThreadPool(THREAD_POOL_SIZE);

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown requested");
            POOL.shutdown();
        }, "shutdown-hook"));

        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            serverSocket.setReuseAddress(true);
            log.info("Server started on port {}", SERVER_PORT);

            while (true) {
                Socket client = serverSocket.accept();
                try {
                    POOL.execute(() -> processClientRequest(client));
                } catch (RejectedExecutionException rex) {
                    write503AndClose(client);
                }
            }
        } catch (IOException e) {
            log.error("Server fatal: {}", e.getMessage(), e);
        } finally {
            POOL.shutdown();
        }
    }

    private static void processClientRequest(Socket socket) {
        try {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            HttpRequest request = HttpRequest.build(inputStream);
            log.info("HttpRequest:\n{}", request);
            HttpResponse response = DISPATCHER.dispatch(request);
            byte[] bytes = response.toByteArray();
            outputStream.write(bytes);
            outputStream.flush();
            log.info("Response sent: {}", response);
            socket.close();
        } catch (Throwable t) {
            log.error("Worker error: {}", t.getMessage(), t);
            try { write500(socket); } catch (Exception ignore) {}
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
    private static void write503AndClose(Socket s) {
        try (s; OutputStream out = s.getOutputStream()) {
            byte[] body = "{\"error\":\"Service Unavailable\"}".getBytes(StandardCharsets.UTF_8);
            String head = "HTTP/1.1 503 Service Unavailable\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + body.length + "\r\n" +
                    "Connection: close\r\n\r\n";
            out.write(head.getBytes(StandardCharsets.US_ASCII));
            out.write(body);
            out.flush();
        } catch (Exception ignore) {}
    }
}