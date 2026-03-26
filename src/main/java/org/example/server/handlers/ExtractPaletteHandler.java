package org.example.server.handlers;

import lombok.AllArgsConstructor;
import org.example.pixelforging.PixelForging;
import org.example.server.HttpRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@AllArgsConstructor
public class ExtractPaletteHandler implements Runnable {
    private OutputStream rawOut;
    private HttpRequest request;

    @Override
    public void run() {
        try {
            if (!"POST".equalsIgnoreCase(request.method)) {
                sendTextResponse(405, "Method Not Allowed: use POST /extract-palette");
                return;
            }

            if (request.body == null || request.body.length == 0) {
                sendTextResponse(400, "Bad Request: No image data provided");
                return;
            }

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(request.body));
            if (image == null) {
                sendTextResponse(400, "Bad Request: Invalid image format");
                return;
            }

            BufferedImage palette = PixelForging.extractColorPalette(image, 3, 50, 50, 6);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(palette, "png", baos);
            byte[] paletteBytes = baos.toByteArray();

            String headers =
                    "HTTP/1.1 200 OK\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Content-Type: image/png\r\n" +
                            "Content-Length: " + paletteBytes.length + "\r\n" +
                            "\r\n";

            rawOut.write(headers.getBytes(StandardCharsets.UTF_8));
            rawOut.write(paletteBytes);
            rawOut.flush();

        } catch (Exception e) {
            e.printStackTrace();
            sendTextResponse(500, "Internal Server Error: " + e.getMessage());
        }
    }

    private void sendTextResponse(int status, String body) {
        try {
            String response =
                    "HTTP/1.1 " + status + " " + getStatusText(status) + "\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: " + body.length() + "\r\n" +
                            "\r\n" +
                            body;

            rawOut.write(response.getBytes(StandardCharsets.UTF_8));
            rawOut.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getStatusText(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "Unknown";
        };
    }
}
