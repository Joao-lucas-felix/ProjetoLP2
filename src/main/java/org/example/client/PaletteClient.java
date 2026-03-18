package org.example.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class PaletteClient {
    private static final Logger LOG = Logger.getLogger(PaletteClient.class.getName());
    private static final Path INPUT_PATH = Path.of("input.png");
    private static final Path OUTPUT_PATH = Path.of("palette-from-server.png");
    private static final URI ENDPOINT = URI.create("http://localhost:8080/extract-palette");

    public static void main(String[] args) {
        setupLogger();
        LOG.info("Starting PaletteClient");

        if (!Files.exists(INPUT_PATH)) {
            LOG.severe("Input file not found: " + INPUT_PATH.toAbsolutePath());
            return;
        }

        try {
            byte[] payload = Files.readAllBytes(INPUT_PATH);
            LOG.info("Read input.png bytes: " + payload.length);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(ENDPOINT)
                    .header("Content-Type", "image/png")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                    .build();

            LOG.info("Sending POST to " + ENDPOINT);
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            LOG.info("Response status: " + response.statusCode());

            String contentType = response.headers().firstValue("Content-Type").orElse("unknown");
            LOG.info("Response Content-Type: " + contentType);

            if (response.statusCode() == 200 && contentType.startsWith("image/")) {
                Files.write(OUTPUT_PATH, response.body());
                LOG.info("Saved response image to " + OUTPUT_PATH.toAbsolutePath());
            } else {
                String bodyText = new String(response.body());
                LOG.warning("Non-image response body: " + bodyText);
            }
        } catch (IOException | InterruptedException e) {
            LOG.log(Level.SEVERE, "Client failed", e);
            Thread.currentThread().interrupt();
        }
    }

    private static void setupLogger() {
        try {
            FileHandler fileHandler = new FileHandler("palette-client.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOG.addHandler(fileHandler);
            LOG.setUseParentHandlers(false);
        } catch (IOException e) {
            System.err.println("Failed to set up logger: " + e.getMessage());
        }
    }
}
