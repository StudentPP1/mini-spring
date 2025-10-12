package com.test.http;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HttpRequest {
    private static final Logger log = LogManager.getLogger(HttpRequest.class);
    private final HttpMethod method;
    private final String path;
    private final List<String> pathParts;
    private final Map<String, String> headers;
    private final String body;

    private HttpRequest(HttpMethod method, String path, Map<String, String> headers, String body) {
        this.method = method;
        this.path = path;
        this.pathParts = Arrays.stream(path.split("/"))
                .filter(part -> !part.isEmpty())
                .toList();
        this.headers = headers;
        this.body = body;
    }

    public static HttpRequest build(InputStream inputStream) throws IOException {
        String firstLine = InputStreamParser.readLine(inputStream);
        assert firstLine != null;
        String[] lines = firstLine.split("\n");
        String requestLine = lines[0];
        HttpMethod method = parseMethod(requestLine);
        String path = parsePath(requestLine);
        log.trace("method: {}", method);
        log.trace("path: {}", path);
        Map<String, String> headers = InputStreamParser.readHeaders(inputStream);
        String body = null;
        String contentLengthHeader = headers.getOrDefault("content-length", headers.get("Content-Length"));
        if (contentLengthHeader != null) {
            int contentLength = Integer.parseInt(contentLengthHeader.trim());
            body = InputStreamParser.readBody(inputStream, contentLength);
        }
        log.trace("body: {}", body);
        return new HttpRequest(method, path, headers, body);
    }

    private static HttpMethod parseMethod(String requestLine) {
        return HttpMethod.valueOf(requestLine.split(" ")[0]);
    }

    private static String parsePath(String requestLine) {
        return requestLine.split(" ")[1];
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
