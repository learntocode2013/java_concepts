package com.github.learntocode2013.threads;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class VirtualThreadBasedHttpServer {
  public static final int PORT = 8080;
  private final AtomicInteger connectionCounter = new AtomicInteger(0);
  public static void main(String[] args) {
    new VirtualThreadBasedHttpServer().start();
  }

  // Demonstrate thread per request model using java virtual threads
  private void start() {
    try {
      var server = new ServerSocket(PORT, 5);
      System.out.printf("Server started listening on port: %d %n", PORT);
      while (!Thread.currentThread().isInterrupted()) {
        Socket clientSocket = server.accept();
        connectionCounter.incrementAndGet();
        System.out.printf("Accepted new client connection on port: %d | Total: %d %n",
            clientSocket.getLocalPort(), connectionCounter.get());
        Thread.startVirtualThread(new SimpleWorker(clientSocket));
      }
      System.out.printf("%s thread was interrupted. Exiting http server... %n",
          Thread.currentThread().getName());
    } catch (Exception cause) {
        cause.printStackTrace();
        throw new RuntimeException(cause);
    }
  }

  private class SimpleWorker implements Runnable {
    private final Socket clientSocket;

    public SimpleWorker(Socket clientSocket) {
      this.clientSocket = clientSocket;
    }
    @Override
    public void run() {
      try {
        var dataReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String line = null;
        while((line = dataReader.readLine()) != null) {
          System.out.printf("Client sent: %s %n", line);
        }
        dataReader.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
