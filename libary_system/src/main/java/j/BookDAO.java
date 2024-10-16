package j;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


public class BookDAO {
    public static void addBook(String title, String author, String isbn) throws Exception {
        String sql = "INSERT INTO books(title, author, isbn) VALUES(?, ?, ?)";

        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, author);
            pstmt.setString(3, isbn);
            pstmt.executeUpdate();
        }
    }

    public static void deleteBook(int bookId) throws Exception {
        String sql = "DELETE FROM books WHERE id = ?";

        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Deleting book failed, no rows affected.");
            }
        }
    }


    public static List<Book> getAllBooks() throws Exception {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books";

        try (Connection conn = Database.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        rs.getInt("available")
                ));
            }
        }
        return books;
    }

    public static void updateBookAvailability(int bookId, boolean isAvailable) throws Exception {
        String sql = "UPDATE books SET available = ? WHERE id = ?";

        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, isAvailable ? 1 : 0);
            pstmt.setInt(2, bookId);
            pstmt.executeUpdate();
        }
    }

    public static void addTransaction(int bookId, String action) throws Exception {
        String sql = "INSERT INTO transactions(book_id, action) VALUES(?, ?)";

        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            pstmt.setString(2, action);
            pstmt.executeUpdate();
        }
    }

    public static Book getBookById(int id) throws Exception {
        String sql = "SELECT * FROM books WHERE id = ?";
        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        rs.getInt("available")
                );
            }
        }
        return null;
    }

    public static List<Book> searchBooks(String searchText, String searchCriteria) throws Exception {
        List<Book> books = new ArrayList<>();
        String sql = "";

        if (searchCriteria.equalsIgnoreCase("Title")) {
            sql = "SELECT * FROM books WHERE LOWER(title) LIKE ?";
        } else if (searchCriteria.equalsIgnoreCase("Author")) {
            sql = "SELECT * FROM books WHERE LOWER(author) LIKE ?";
        } else {
            throw new IllegalArgumentException("Invalid search criteria");
        }

        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + searchText.toLowerCase() + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        rs.getInt("available")
                ));
            }
        }

        return books;
    }

}
