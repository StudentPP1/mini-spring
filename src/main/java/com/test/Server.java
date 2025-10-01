package com.test;

import com.test.entity.Data;
import com.test.http.HttpRequest;
import com.test.mapper.ObjectMapper;
import com.test.socketPool.SocketThreadPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static final int SERVER_PORT = 8080;
    private static final int BUFFER_SIZE = 100;
    private static final int INPUT_STREAM_EMPTY = -1;
    private static final int THREAD_POOL_SIZE = 10;
    private static final Logger LOGGER = LogManager.getLogger(Server.class);

    public static void main(String[] args) {
        SocketThreadPool pool = new SocketThreadPool(THREAD_POOL_SIZE);
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            while (true) {
                Socket clientConnection = serverSocket.accept();
                pool.execute(() -> processClientRequest(clientConnection));
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error(e.getMessage());
            pool.shutdown();
        }
    }

    private static void processClientRequest(Socket clientConnection) {
        try (Socket socket = clientConnection) {
            LOGGER.info("Get client connection");
            readInput(socket);
            sendResponse(socket);
        } catch (IOException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendResponse(Socket clientConnection) throws IOException {
        OutputStream outputStream = clientConnection.getOutputStream();
        String response = "Code 200";
        char[] chars = response.toCharArray();
        for (char c : chars) {
            outputStream.write(c);
        }
        LOGGER.info("Send response: '{}' to client", response);
        outputStream.flush();
    }

    private static void readInput(Socket clientSocket) throws IOException, NoSuchFieldException, IllegalAccessException {
        InputStream inputStream = clientSocket.getInputStream();
        LOGGER.debug("Get client input stream");
        StringBuilder content = new StringBuilder();
        byte[] bufferBytes = new byte[BUFFER_SIZE];
        char[] chars = new char[BUFFER_SIZE];
        int lengthReadBytes;
        // inputStream return -1 when it's empty,
        // so every while cycle -> read BUFFER_SIZE bytes or less
        while ((lengthReadBytes = inputStream.read(bufferBytes)) != INPUT_STREAM_EMPTY) {
            LOGGER.debug("Read {} bytes from buffer {}", lengthReadBytes, BUFFER_SIZE);
            for (int i = 0; i < lengthReadBytes; i++) {
                chars[i] = (char) bufferBytes[i];
            }
            LOGGER.debug("Convert {} bytes to string", lengthReadBytes);
            content.append(chars, 0, lengthReadBytes);
        }

        HttpRequest httpRequest = HttpRequest.build(content);
        LOGGER.info("HttpRequest: \n{}", httpRequest);

        Data data = ObjectMapper.parse(httpRequest.getBody(), Data.class);
        LOGGER.info("Data object from request body: \n{}", data);
    }
}