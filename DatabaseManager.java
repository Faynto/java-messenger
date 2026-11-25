import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://localhost:5432/messenger_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "mohammadE$2";

    public DatabaseManager() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("PostgreSQL JDBC Driver not found: " + e.getMessage());
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public int getOrCreateUserId(String username) throws SQLException {
        String selectSql = "SELECT user_id FROM users WHERE username = ?";
        try (Connection conn = connect();
                PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setString(1, username);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        }

        String insertSql = "INSERT INTO users (username) VALUES (?) RETURNING user_id";
        try (Connection conn = connect();
                PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, username);
            try (ResultSet rs = insertStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        }

        throw new SQLException("Failed to retrieve or create user: " + username);
    }

    public void saveMessage(int senderId, String content) throws SQLException {
        String sql = "INSERT INTO messages (sender_id, content) VALUES (?, ?)";
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, senderId);
            stmt.setString(2, content);
            stmt.executeUpdate();
        }
    }

    public List<String> getRecentMessages(int limit) throws SQLException {
        String sql = """
                SELECT u.username, m.content
                FROM messages m
                JOIN users u ON m.sender_id = u.user_id
                ORDER BY m.message_id DESC
                LIMIT ?
                """;
        List<String> history = new ArrayList<>();

        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    history.add(rs.getString("username") + ": " + rs.getString("content"));
                }
            }
        }

        Collections.reverse(history);
        return history;
    }
}