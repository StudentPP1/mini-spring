package com.test.http;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public record HttpResponse(HttpStatus httpStatus, Map<String, String> headers, String body) {

    public HttpResponse {
        if (headers == null) headers = new LinkedHashMap<>();
        if (body == null) body = "";
    }

    public static HttpResponse build(HttpStatus status) {
        return new HttpResponse(status, new LinkedHashMap<>(), "");
    }

    public static HttpResponse build(HttpStatus status, Map<String, String> headers, String body) {
        return new HttpResponse(status, headers, body);
    }

    public byte[] toByteArray() {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
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
        System.arraycopy(bodyBytes, 0, fullContent, 0, bodyBytes.length);
        return fullContent;
    }

    @Override
    public String toString() {
        return "HttpResponse{httpStatus=%s, headers=%s, bodyLen=%d}"
                .formatted(httpStatus, headers, body.getBytes(StandardCharsets.UTF_8).length);
    }
}