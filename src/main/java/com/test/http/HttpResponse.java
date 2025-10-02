package com.test.http;

import java.util.HashMap;
import java.util.Map;

public record HttpResponse(HttpStatus httpStatus, HashMap<String, String> headers, String body) {

    private HttpResponse(HttpStatus httpStatus) {
        this(httpStatus, null, null);
    }

    public static HttpResponse build(HttpStatus httpStatus) {
        return new HttpResponse(httpStatus);
    }

    public static HttpResponse build(HttpStatus httpStatus, HashMap<String, String> headers, String body) {
        return new HttpResponse(httpStatus, headers, body);
    }

    public byte[] toByteArray() {
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 ")
                .append(this.httpStatus.getStatus())
                .append("\n");
        if (this.headers != null) {
            for (Map.Entry<String, String> header : this.headers.entrySet())
                response.append(header.getKey())
                        .append(": ")
                        .append(header.getValue())
                        .append("\n");
            response.append("\n");
        }
        if (this.body != null) {
            response.append(this.body);
        }
        return response.toString().getBytes();
    }

    @Override
    public String toString() {
        return "HttpResponse{" +
                "httpStatus=" + httpStatus +
                ", headers=" + headers +
                ", body=" + body +
                '}';
    }
}
