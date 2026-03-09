package org.example.server.handlers;

import lombok.AllArgsConstructor;

import java.io.BufferedWriter;
@AllArgsConstructor
public class ErrorPageHandler  implements  Runnable
{
    private BufferedWriter writer;

    @Override
    public void run() {
        try {

            String body = "Not Found";

            String response =
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: " + body.length() + "\r\n" +
                            "\r\n" +
                            body;

            writer.write(response);
            writer.flush();

        } catch (Exception e) { e.printStackTrace(); }
    }
}
