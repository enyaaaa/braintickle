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

@WebServlet("/submitAnswer")
public class SubmitAnswerServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/braintickle";
    private static final String DB_USER = "root"; // Replace with your MySQL username
    private static final String DB_PASSWORD = "MySecurePassword"; // Replace with your MySQL password

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String player = request.getParameter("player");
        String sessionId = request.getParameter("sessionId");
        String questionIdStr = request.getParameter("questionId");
        String playerAnswerStr = request.getParameter("playerAnswer");

        // Validate inputs
        if (player == null || sessionId == null || questionIdStr == null || playerAnswerStr == null ||
            player.trim().isEmpty() || sessionId.trim().isEmpty() || questionIdStr.trim().isEmpty() || playerAnswerStr.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Missing required parameters\"}");
            return;
        }

        int questionId;
        int playerAnswer;
        try {
            questionId = Integer.parseInt(questionIdStr);
            playerAnswer = mapAnswerToNumber(playerAnswerStr); // Convert A, B, C, D to 1, 2, 3, 4
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid questionId or playerAnswer\"}");
            return;
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid playerAnswer: " + e.getMessage() + "\"}");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Verify session and question
                String sessionSql = "SELECT currentQuestionId FROM quizSessions WHERE id = ? AND status = 'active'";
                try (PreparedStatement stmt = conn.prepareStatement(sessionSql)) {
                    stmt.setString(1, sessionId);
                    ResultSet rs = stmt.executeQuery();
                    if (!rs.next() || rs.getInt("currentQuestionId") != questionId) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.getWriter().write("{\"error\":\"Invalid session or question\"}");
                        return;
                    }
                }

                // Get the correct answer
                String questionSql = "SELECT answer FROM questions WHERE id = ?";
                int correctAnswer;
                try (PreparedStatement stmt = conn.prepareStatement(questionSql)) {
                    stmt.setInt(1, questionId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        correctAnswer = rs.getInt("answer");
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("{\"error\":\"Question not found\"}");
                        return;
                    }
                }

                // Check if the answer is correct
                boolean isCorrect = (playerAnswer == correctAnswer);

                // Store the answer
                String insertSql = "INSERT INTO playerAnswers (sessionId, player, questionId, playerAnswer, isCorrect) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    stmt.setString(1, sessionId);
                    stmt.setString(2, player);
                    stmt.setInt(3, questionId);
                    stmt.setInt(4, playerAnswer);
                    stmt.setBoolean(5, isCorrect);
                    stmt.executeUpdate();
                }

                // Respond with result
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Answer submitted\", \"isCorrect\":" + isCorrect + "}");
            }
        } catch (ClassNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Database driver not found: " + e.getMessage() + "\"}");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Database error: " + e.getMessage() + "\"}");
        }
    }

    private int mapAnswerToNumber(String answer) {
        switch (answer.toUpperCase()) {
            case "A": return 1;
            case "B": return 2;
            case "C": return 3;
            case "D": return 4;
            default: throw new IllegalArgumentException("Answer must be A, B, C, or D");
        }
    }
}