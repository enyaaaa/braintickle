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

@WebServlet("/getQuestion")
public class GetQuestionServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/braintickle";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "MySecurePassword";
    private static final int QUESTION_DURATION_SECONDS = 30;

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
                // Get the current question ID and start time from quizSessions
                String getQuestionIdSql = "SELECT currentQuestionId, questionStartTime FROM quizSessions WHERE id = ?";
                Integer questionId = null;
                Timestamp startTime = null;
                try (PreparedStatement stmt = conn.prepareStatement(getQuestionIdSql)) {
                    stmt.setString(1, sessionId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        questionId = rs.getInt("currentQuestionId");
                        startTime = rs.getTimestamp("questionStartTime");
                        if (rs.wasNull()) {
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            response.getWriter().write("{\"error\":\"No current question set for session\"}");
                            return;
                        }
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("{\"error\":\"Session not found: " + sessionId + "\"}");
                        return;
                    }
                }

                // Calculate remaining time
                long remainingTimeSeconds = QUESTION_DURATION_SECONDS;
                if (startTime != null) {
                    long elapsedTimeMillis = System.currentTimeMillis() - startTime.getTime();
                    long elapsedTimeSeconds = elapsedTimeMillis / 1000;
                    remainingTimeSeconds = Math.max(0, QUESTION_DURATION_SECONDS - elapsedTimeSeconds);
                }

                // Fetch the question details
                String getQuestionSql = "SELECT id, question, option1, option2, option3, option4, answer FROM questions WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(getQuestionSql)) {
                    stmt.setInt(1, questionId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        String questionData = rs.getInt("id") + "|" +
                                             rs.getString("question") + "|" +
                                             rs.getString("option1") + "|" +
                                             rs.getString("option2") + "|" +
                                             rs.getString("option3") + "|" +
                                             rs.getString("option4") + "|" +
                                             rs.getInt("answer") + "|" +
                                             remainingTimeSeconds;
                        response.setContentType("text/plain");
                        response.getWriter().write(questionData);
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