import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.sql.*;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/getResults")
public class GetResultsServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sessionId = request.getParameter("sessionId");
        if (sessionId == null || sessionId.trim().isEmpty()) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Missing sessionId\"}");
            return;
        }

        StringBuilder results = new StringBuilder("[");
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/braintickle", "root", "MySecurePassword")) {
            // Get player answers for the session
            String sql = "SELECT questionId, player, playerAnswer FROM playerAnswers WHERE sessionId = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, sessionId); // Treat sessionId as a string
            ResultSet rs = stmt.executeQuery();

            boolean first = true;
            while (rs.next()) {
                if (!first) results.append(",");
                int questionId = rs.getInt("questionId");
                String player = rs.getString("player");
                String playerAnswer = rs.getString("playerAnswer");

                // Get correct answer for this question
                String correctAnswerSql = "SELECT answer FROM questions WHERE id = ?";
                PreparedStatement correctStmt = conn.prepareStatement(correctAnswerSql);
                correctStmt.setInt(1, questionId);
                ResultSet correctRs = correctStmt.executeQuery();
                String correctAnswer = null;
                if (correctRs.next()) {
                    correctAnswer = correctRs.getString("answer");
                }

                boolean isCorrect = correctAnswer != null && playerAnswer.equals(correctAnswer);
                results.append("{\"questionId\":").append(questionId)
                        .append(",\"player\":\"").append(player)
                        .append("\",\"answer\":\"").append(playerAnswer)
                        .append("\",\"isCorrect\":").append(isCorrect)
                        .append(",\"correctAnswer\":\"").append(correctAnswer != null ? correctAnswer : "")
                        .append("\"}");
                first = false;
            }
            results.append("]");
        } catch (SQLException e) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Database error: " + e.getMessage() + "\"}");
            return;
        }

        response.setContentType("application/json");
        response.getWriter().write("{\"results\":" + results.toString() + "}");
    }
}