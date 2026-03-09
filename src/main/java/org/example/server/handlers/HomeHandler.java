package org.example.server.handlers;

import lombok.AllArgsConstructor;

import java.io.BufferedWriter;

@AllArgsConstructor
public class HomeHandler implements  Runnable
{
    private BufferedWriter writer;

    @Override
    public void run() {
        try {

            String body = "Hello World, From Java Server.";

            String response =
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: " + body.length() + "\r\n" +
                            "\r\n" +
                            body;

            writer.write(response);
            writer.flush();

        } catch (Exception e) { e.printStackTrace(); }
    }
}
