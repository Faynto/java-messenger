import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private final DatabaseManager dbManager;
    private PrintWriter out;
    private String username;
    private int userId = -1;

    public ClientHandler(Socket socket, ChatServer server, DatabaseManager dbManager) {
        this.socket = socket;
        this.server = server;
        this.dbManager = dbManager;
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            this.out = writer;

            out.println("Enter your username:");
            this.username = in.readLine();
            if (this.username == null || this.username.isBlank()) {
                this.username = "Anonymous";
            }

            if (dbManager != null) {
                try {
                    this.userId = dbManager.getOrCreateUserId(this.username);
                    List<String> history = dbManager.getRecentMessages(20);
                    if (!history.isEmpty()) {
                        out.println("--- Recent Chat History ---");
                        for (String line : history) {
                            out.println(line);
                        }
                        out.println("---------------------------");
                    }
                } catch (SQLException e) {
                    System.out.println("Database error on join for " + username + ": " + e.getMessage());
                }
            }

            System.out.println(username + " joined the chat.");
            server.broadcast("[SERVER] " + username + " joined the chat!", this);

            String message;
            while ((message = in.readLine()) != null) {
                if (dbManager != null && userId != -1) {
                    try {
                        dbManager.saveMessage(userId, message);
                    } catch (SQLException e) {
                        System.out.println("Failed to save message from " + username + ": " + e.getMessage());
                    }
                }
                server.broadcast(username + ": " + message, this);
            }
        } catch (IOException e) {
            System.out.println(
                    "Connection error with " + (username != null ? username : "a client") + ": " + e.getMessage());
        } finally {
            server.removeClient(this);
            if (username != null) {
                System.out.println(username + " disconnected.");
                server.broadcast("[SERVER] " + username + " left the chat.", this);
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}