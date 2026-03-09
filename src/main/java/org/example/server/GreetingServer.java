package org.example.server;

import lombok.AllArgsConstructor;
import org.example.server.handlers.ErrorPageHandler;
import org.example.server.handlers.ExtractPaletteHandler;
import org.example.server.handlers.HomeHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GreetingServer {
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
                threadPool.execute(new ClienteHandler(clientSocket));
            }

        }catch (IOException _) { IO.println("Fail While Trying to Start the Server!"); }

    }
}

@AllArgsConstructor
class ClienteHandler implements  Runnable
{
    private final Socket socket;

    @Override
    public void run() {
        try(
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(this.socket.getInputStream())
                );
                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(this.socket.getOutputStream())
                );
        ) {
            // Pega o caminho da requisição
            String requestLine = in.readLine();
            System.out.println(requestLine);

            String[] parts = requestLine.split(" ");
            String path = parts[1];


            // Faz o Roteamento para o Runnable especifico.
            Runnable endpoint = switch (path) {
                case "/" -> new HomeHandler(out);
                case "/extract-palette" -> new ExtractPaletteHandler(out);
                default -> new ErrorPageHandler(out);
            };

            endpoint.run();
            socket.close();

        } catch (IOException e) { e.printStackTrace(); }
    }
}
