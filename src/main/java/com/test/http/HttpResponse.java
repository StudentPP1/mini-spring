package com.test.http;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponse {
    private HttpStatus httpStatus;
    private Map<String, String> headers;
    private String body;

    private HttpResponse(HttpStatus httpStatus, Map<String, String> headers, String body) {
        this.httpStatus = httpStatus;
        this.headers = headers;
        this.body = body;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public static HttpResponse build(HttpStatus status) {
        return new HttpResponse(status, new LinkedHashMap<>(), "");
    }

    public static HttpResponse build(HttpStatus status, Map<String, String> headers, String body) {
        return new HttpResponse(status, headers, body);
    }

    public byte[] toByteArray() {
        byte[] bodyBytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        headers.putIfAbsent("Connection", "close");
        headers.putIfAbsent("Content-Length", String.valueOf(bodyBytes.length));
        String statusLine = "HTTP/1.1 " + httpStatus.getStatus() + "\r\n";
        StringBuilder builder = new StringBuilder(statusLine);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }
        builder.append("\r\n");
        byte[] firstLineAndHeaders = builder.toString().getBytes(StandardCharsets.UTF_8);
        byte[] fullContent = new byte[firstLineAndHeaders.length + bodyBytes.length];
        System.arraycopy(firstLineAndHeaders, 0, fullContent, 0, firstLineAndHeaders.length);
        System.arraycopy(bodyBytes, 0, fullContent, firstLineAndHeaders.length, bodyBytes.length);
        return fullContent;
    }

    @Override
    public String toString() {
        return "HttpResponse{httpStatus=%s, headers=%s, body=%s, bodyLen=%d}"
                .formatted(httpStatus, headers, body, body.getBytes(StandardCharsets.UTF_8).length);
    }

    public void setHeader(String name, String value) {
        this.headers.putIfAbsent(name, value);
    }
}