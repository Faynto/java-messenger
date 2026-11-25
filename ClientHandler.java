import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
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

            System.out.println(username + " joined the chat.");
            server.broadcast("[SERVER] " + username + " joined the chat!", this);

            String message;
            while ((message = in.readLine()) != null) {
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