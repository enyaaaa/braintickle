import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/displayResults")
public class DisplayResultsServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/braintickle";
    private static final String DB_USER = "root"; // Replace with your MySQL username
    private static final String DB_PASSWORD = "MySecurePassword"; // Replace with your MySQL password

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String sessionId = request.getParameter("sessionId");

        if (sessionId == null || sessionId.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Missing sessionId\"}");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Get the current question for the session
                String sessionSql = "SELECT currentQuestionId FROM quizSessions WHERE id = ? AND status = 'active'";
                Integer questionId = null;
                try (PreparedStatement stmt = conn.prepareStatement(sessionSql)) {
                    stmt.setString(1, sessionId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        questionId = rs.getInt("currentQuestionId");
                        if (rs.wasNull()) {
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            response.getWriter().write("{\"error\":\"No current question set for session\"}");
                            return;
                        }
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("{\"error\":\"Session not found or not active: " + sessionId + "\"}");
                        return;
                    }
                }

                // Fetch the current question details
                String questionSql = "SELECT questionType, question, option1, option2, option3, option4 FROM questions WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(questionSql)) {
                    stmt.setInt(1, questionId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        String jsonResponse = String.format(
                            "{\"questionId\":%d,\"questionType\":\"%s\",\"question_text\":\"%s\",\"option1\":\"%s\",\"option2\":\"%s\",\"option3\":\"%s\",\"option4\":\"%s\"}",
                            questionId,
                            rs.getString("questionType"),
                            rs.getString("question"),
                            rs.getString("option1"),
                            rs.getString("option2"),
                            rs.getString("option3"),
                            rs.getString("option4")
                        );
                        response.setContentType("application/json");
                        response.getWriter().write(jsonResponse);
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("{\"error\":\"Question not found for ID: " + questionId + "\"}");
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Database driver not found: " + e.getMessage() + "\"}");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Database error: " + e.getMessage() + "\"}");
        }
    }
}