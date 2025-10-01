package com.test.http;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequest {
    private final HttpMethod method;
    private final String path;
    private final List<String> pathParts;
    private final HashMap<String, String> headers;
    private final String body;

    private HttpRequest(HttpMethod method, String path, HashMap<String, String> headers, String body) {
        this.method = method;
        this.path = path;
        this.pathParts = Arrays.stream(path.split("/"))
                .filter(part -> !part.isEmpty())
                .toList();
        this.headers = headers;
        this.body = body;
    }

    public static HttpRequest build(StringBuilder request) {
        String[] lines = request.toString().split("\n");
        String requestLine = lines[0];
        HttpMethod method = parseMethod(requestLine);
        String path = parsePath(requestLine);
        int emptyLineIndex = findEmptyLineIndex(lines);
        HashMap<String, String> headers = parseHeaders(lines, emptyLineIndex);
        String body = parseBody(lines, emptyLineIndex + 1);
        return new HttpRequest(method, path, headers, body);
    }

    private static HttpMethod parseMethod(String requestLine) {
        return HttpMethod.valueOf(requestLine.split(" ")[0]);
    }

    private static String parsePath(String requestLine) {
        return requestLine.split(" ")[1];
    }

    private static int findEmptyLineIndex(String[] lines) {
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) {
                return i;
            }
        }
        return lines.length;
    }

    private static HashMap<String, String> parseHeaders(String[] lines, int emptyLineIndex) {
        HashMap<String, String> headers = new HashMap<>();
        for (int i = 1; i < emptyLineIndex; i++) {
            String line = lines[i];
            int colonIndex = line.indexOf(":");
            String headerName = line.substring(0, colonIndex).trim();
            String headerValue = line.substring(colonIndex + 1).trim();
            headers.putIfAbsent(headerName, headerValue);
        }
        return headers;
    }

    private static String parseBody(String[] lines, int startIndex) {
        StringBuilder body = new StringBuilder();
        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i];
            if (i != lines.length - 1) {
                line += "\n";
            }
            body.append(line);
        }
        return body.toString();
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "method=" + method +
                ", path='" + path + '\'' +
                ", pathParts=" + pathParts +
                ", headers=" + headers +
                ", body='" + body + '\'' +
                '}';
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public List<String> getPathParts() {
        return pathParts;
    }

    public String getPath() {
        return path;
    }
}
