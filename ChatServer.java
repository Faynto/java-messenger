import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {
    private static final int PORT = 5000;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private DatabaseManager dbManager;

    public static void main(String[] args) {
        new ChatServer().start();
    }

    public void start() {
        dbManager = new DatabaseManager();
        System.out.println("Group chat server starting on port " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New socket connected from " + clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(clientSocket, this, dbManager);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    public DatabaseManager getDbManager() {
        return dbManager;
    }

    public void broadcast(String message, ClientHandler sender) {
        System.out.println(message);
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }
}