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

@WebServlet("/nextQuestion")
public class NextQuestionServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/braintickle";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "MySecurePassword";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String sessionId = request.getParameter("sessionId");

        if (sessionId == null || sessionId.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing sessionId");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Get the current question's category
                String getCurrentQuestionSql = "SELECT q.questionType " +
                                              "FROM quizSessions qs " +
                                              "JOIN questions q ON qs.currentQuestionId = q.id " +
                                              "WHERE qs.id = ?";
                String category;
                try (PreparedStatement stmt = conn.prepareStatement(getCurrentQuestionSql)) {
                    stmt.setString(1, sessionId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        category = rs.getString("questionType");
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("Session not found: " + sessionId);
                        return;
                    }
                }

                // Fetch a new random question from the same category
                String selectRandomQuestionSql = "SELECT id FROM questions WHERE questionType = ? ORDER BY RAND() LIMIT 1";
                int newQuestionId;
                try (PreparedStatement stmt = conn.prepareStatement(selectRandomQuestionSql)) {
                    stmt.setString(1, category);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        newQuestionId = rs.getInt("id");
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("No more questions found for category: " + category);
                        return;
                    }
                }

                // Update the currentQuestionId and set questionStartTime
                String updateSessionSql = "UPDATE quizSessions SET currentQuestionId = ?, questionStartTime = ? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSessionSql)) {
                    stmt.setInt(1, newQuestionId);
                    stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                    stmt.setString(3, sessionId);
                    int rowsUpdated = stmt.executeUpdate();
                    if (rowsUpdated == 0) {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("Session not found: " + sessionId);
                        return;
                    }
                }

                response.setContentType("text/plain");
                response.getWriter().write("Next question set successfully: " + newQuestionId);
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