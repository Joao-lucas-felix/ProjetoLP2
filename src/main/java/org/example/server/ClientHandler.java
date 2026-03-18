package org.example.server;

import lombok.AllArgsConstructor;
import org.example.server.handlers.ErrorPageHandler;
import org.example.server.handlers.ExtractPaletteHandler;
import org.example.server.handlers.HomeHandler;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
class ClientHandler implements  Runnable
{
    private final Socket socket;

    @Override
    public void run() {
        try {
            InputStream rawIn = this.socket.getInputStream();
            OutputStream rawOut = this.socket.getOutputStream();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(rawOut, StandardCharsets.UTF_8));

            HttpRequest request = readRequest(rawIn, out);
            if (request == null) {
                return;
            }
            IO.println(request.path);
            Set<String> x = request.headers.keySet();
            for (String header: x ){
                IO.println(header + " - " + request.headers.get(header));
            }
            // Faz o Roteamento para o Runnable especifico.
            Runnable endpoint = switch (request.path) {
                case "/" -> new HomeHandler(out);
                case "/extract-palette" -> new ExtractPaletteHandler(rawOut, request);
                default -> new ErrorPageHandler(out);
            };

            endpoint.run();
            socket.close();

        } catch (IOException e) { e.printStackTrace(); }
    }

    private static org.example.server.HttpRequest readRequest(InputStream in, BufferedWriter out) throws IOException {
        String requestLine = readLine(in);
        if (requestLine == null || requestLine.isEmpty()) {
            return null;
        }

        String[] parts = requestLine.split(" ");
        if (parts.length < 3) {
            sendTextResponse(out, "400 Bad Request", "Invalid request line");
            return null;
        }

        String method = parts[0];
        String path = parts[1];
        String version = parts[2];

        Map<String, String> headers = new HashMap<>();
        while (true) {
            String line = readLine(in);
            if (line == null || line.isEmpty()) {
                break;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String name = line.substring(0, colon).trim().toLowerCase();
            String value = line.substring(colon + 1).trim();
            headers.put(name, value);
        }

        int contentLength = 0;
        String contentLengthHeader = headers.get("content-length");
        if (contentLengthHeader != null) {
            try {
                contentLength = Integer.parseInt(contentLengthHeader);
            } catch (NumberFormatException ignored) {
                sendTextResponse(out, "400 Bad Request", "Invalid Content-Length");
                return null;
            }
        }

        byte[] body = new byte[0];
        if (contentLength > 0) {
            body = readFixedBytes(in, contentLength);
            if (body == null) {
                sendTextResponse(out, "400 Bad Request", "Unexpected EOF while reading body");
                return null;
            }
        }

        return new HttpRequest(method, path, version, headers, body);
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int prev = -1;
        while (true) {
            int b = in.read();
            if (b == -1) {
                if (buffer.size() == 0) {
                    return null;
                }
                break;
            }
            if (prev == '\r' && b == '\n') {
                buffer.write('\r');
                break;
            }
            buffer.write(b);
            prev = b;
            if (b == '\n') {
                break;
            }
        }
        String line = buffer.toString(StandardCharsets.US_ASCII);
        return line.replace("\r", "").replace("\n", "");
    }

    private static byte[] readFixedBytes(InputStream in, int length) throws IOException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = in.read(data, offset, length - offset);
            if (read == -1) {
                return null;
            }
            offset += read;
        }
        return data;
    }

    private static void sendTextResponse(BufferedWriter out, String status, String body) throws IOException {
        String response =
                "HTTP/1.1 " + status + "\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: " + body.length() + "\r\n" +
                        "\r\n" +
                        body;
        out.write(response);
        out.flush();
    }
}



