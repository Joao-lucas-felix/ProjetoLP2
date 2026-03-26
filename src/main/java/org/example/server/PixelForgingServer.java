package org.example.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PixelForgingServer {
    public static final int PORT = 8080;
    public static final int POOL_SIZE  = 10;

    public static void main(String[] args) {

        try (
            ExecutorService threadPool = Executors.newFixedThreadPool(POOL_SIZE);
            ServerSocket serverSocket = new ServerSocket(PORT)
        ) {

            IO.println("HTTP server is Running On: localhost:8080");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket));
            }

        }catch (IOException _Exception) { IO.println("Fail While Trying to Start the Server!"); }

    }
}

