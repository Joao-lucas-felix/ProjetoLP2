package org.example.server;

import java.util.Map;

public class HttpRequest {
    public final String method;
    public final String path;
    public final String version;
    public final Map<String, String> headers;
    public final byte[] body;

    HttpRequest(String method, String path, String version, Map<String, String> headers, byte[] body) {
        this.method = method;
        this.path = path;
        this.version = version;
        this.headers = headers;
        this.body = body;
    }
}