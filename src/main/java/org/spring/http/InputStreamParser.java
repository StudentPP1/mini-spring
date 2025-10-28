package org.spring.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class InputStreamParser {
    private InputStreamParser() {

    }

    public static String readLine(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(128);
        int previous = -1;
        int current;
        while ((current = inputStream.read()) != -1) {
            if (previous == '\r' && current == '\n') {
                break;
            }
            if (previous != -1) {
                buffer.write(previous);
            }
            previous = current;
        }
        if (previous != -1 && !(previous == '\r' && current == '\n')) {
            buffer.write(previous);
        }
        if (buffer.size() == 0 && current == -1) {
            return null;
        }
        return buffer.toString();
    }

    public static Map<String, String> readHeaders(InputStream inputStream) throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        String line;
        while ((line = readLine(inputStream)) != null) {
            if (line.isEmpty()) {
                break;
            }
            int colonIndex = line.indexOf(':');
            String headerName = line.substring(0, colonIndex).trim();
            String headerValue = line.substring(colonIndex + 1).trim();
            headers.put(headerName, headerValue);
        }
        return headers;
    }

    public static String readBody(InputStream inputStream, int length) throws IOException {
        byte[] data = new byte[length];
        int offsize = 0;
        while (offsize < length) {
            int readBytes = inputStream.read(data, offsize, length - offsize);
            if (readBytes == -1) {
                throw new IOException("Unexpected EOF");
            }
            offsize += readBytes;
        }
        return new String(data, StandardCharsets.UTF_8);
    }
}
