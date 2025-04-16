import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/getQuestionByCategory")
public class GetQuestionByCategoryServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/braintickle";
    private static final String DB_USER = "root"; // Replace with your MySQL username
    private static final String DB_PASSWORD = "MySecurePassword"; // Replace with your MySQL password

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String category = request.getParameter("category");
        String sessionId = request.getParameter("sessionId");

        if (category == null || sessionId == null || category.trim().isEmpty() || sessionId.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing category or sessionId");
            return;
        }

        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish database connection
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Fetch a random question ID for the category
                String selectRandomQuestionSql = "SELECT id FROM questions WHERE questionType = ? ORDER BY RAND() LIMIT 1";
                int questionId;
                try (PreparedStatement stmt = conn.prepareStatement(selectRandomQuestionSql)) {
                    stmt.setString(1, category);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        questionId = rs.getInt("id");
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("No questions found for category: " + category);
                        return;
                    }
                }

                // Insert new session into quizSessions table with the question ID
                String insertSessionSql = "INSERT INTO quizSessions (id, currentQuestionId, status, created_at) VALUES (?, ?, 'active', ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertSessionSql)) {
                    stmt.setString(1, sessionId);
                    stmt.setInt(2, questionId);
                    stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                    stmt.executeUpdate();
                }

                // Respond with success message
                response.setContentType("text/plain");
                response.getWriter().write("Session created successfully with question ID: " + questionId);
            }
        } catch (ClassNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Database driver not found: " + e.getMessage());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Database error: " + e.getMessage());
        }
    }
}