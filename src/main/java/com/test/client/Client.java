package com.test.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final Logger LOGGER = LogManager.getLogger(Client.class);

    public static void main(String[] args) {
        sendOnce("POST", "/create",
                Map.of("Content-Type", "application/json"),
                """
                {
                  "title": "First note",
                  "content": "Hello from raw socket client"
                }
                """);

        sendOnce("GET", "/read", Map.of(), null);

        sendOnce("PUT", "/update",
                Map.of("Content-Type", "application/json"),
                """
                {
                  "id": 1,
                  "title": "First note (updated)",
                  "content": "Updated content"
                }
                """);

        sendOnce("GET", "/read", Map.of(), null);

        sendOnce("DELETE", "/delete/1", Map.of(), null);
    }

    private static void sendOnce(String method, String path, Map<String, String> extraHeaders, String bodyOrNull) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT)) {
            LOGGER.info("Connected: {}:{}", SERVER_ADDRESS, SERVER_PORT);

            byte[] bodyBytes = bodyOrNull == null ? new byte[0] : bodyOrNull.getBytes(StandardCharsets.UTF_8);

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try (OutputStream out = buf) {
                writeLine(out, method + " " + path + " HTTP/1.1");
                writeLine(out, "Host: " + SERVER_ADDRESS + ":" + SERVER_PORT);
                // ! close for REST API, keep-alive is base for Websocket handshake
                writeLine(out, "Connection: close");
                if (bodyBytes.length > 0) {
                    String ct = extraHeaders.getOrDefault("Content-Type", "application/json");
                    writeLine(out, "Content-Type: " + ct);
                    writeLine(out, "Content-Length: " + bodyBytes.length);
                } else {
                    writeLine(out, "Content-Length: 0");
                }
                for (var e : extraHeaders.entrySet()) {
                    if (!e.getKey().equalsIgnoreCase("Content-Type")) {
                        writeLine(out, e.getKey() + ": " + e.getValue());
                    }
                }
                writeLine(out, "");
                if (bodyBytes.length > 0) out.write(bodyBytes);
            }

            socket.getOutputStream().write(buf.toByteArray());
            socket.getOutputStream().flush();
            socket.shutdownOutput();
            LOGGER.info("Sent {} {}", method, path);

            String response = readAll(socket);
            LOGGER.info("Response:\n{}", response);

        } catch (IOException e) {
            LOGGER.error("I/O error: {}", e.getMessage(), e);
        }
    }

    private static void writeLine(OutputStream out, String line) throws IOException {
        out.write(line.getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static String readAll(Socket socket) throws IOException {
        try (InputStream in = socket.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String s;
            while ((s = reader.readLine()) != null) {
                sb.append(s).append("\n");
            }
            return sb.toString();
        }
    }
}