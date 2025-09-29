package com.test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class ClientSocket {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final Logger LOGGER = LogManager.getLogger(ClientSocket.class);

    public static void main(String[] args) {
        try (Socket clientSocket = new Socket(SERVER_ADDRESS, SERVER_PORT)) {
            LOGGER.info("Client connected to server at " + SERVER_ADDRESS + ":" + SERVER_PORT);
            sendData(clientSocket);
            readResponse(clientSocket);
        } catch (UnknownHostException e) {
            LOGGER.error("Unknown host: {}", e.getMessage());
        } catch (IOException e) {
            LOGGER.error("I/O error when connecting to server: {}", e.getMessage());
        }
    }

    private static void sendData(Socket clientSocket) throws IOException {
        OutputStream outputStream = clientSocket.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        writer.write("POST /data HTTP/1.1\n");
        writer.write("Content-Type: application/json");
        writer.flush();
        LOGGER.info("Client send data to server");
        clientSocket.shutdownOutput();
    }

    private static void readResponse(Socket clientSocket) throws IOException {
        InputStream inputStream = clientSocket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        LOGGER.info("Read response from server: \n{}", content);
    }
}