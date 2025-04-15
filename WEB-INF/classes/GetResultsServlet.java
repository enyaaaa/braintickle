import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.sql.*;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/getResults")
public class GetResultsServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sessionId = request.getParameter("sessionId");
        int questionId;
        try {
            questionId = Integer.parseInt(request.getParameter("questionId"));
        } catch (NumberFormatException e) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid questionId\"}");
            return;
        }

        StringBuilder results = new StringBuilder("[");
        String correctAnswer = null;

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/braintickle", "root", "MySecurePassword")) {
            // Get correct answer
            String sql = "SELECT answer FROM questions WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, questionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                correctAnswer = rs.getString("answer");
            }

            // Get player answers
            sql = "SELECT player, playerAnswer FROM playerAnswers WHERE sessionId = ? AND questionId = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, Integer.parseInt(sessionId));
            stmt.setInt(2, questionId);
            rs = stmt.executeQuery();

            boolean first = true;
            while (rs.next()) {
                if (!first) results.append(",");
                String player = rs.getString("player");
                String playerAnswer = rs.getString("playerAnswer");
                boolean isCorrect = playerAnswer.equals(correctAnswer);
                results.append("{\"player\":\"").append(player)
                        .append("\",\"answer\":\"").append(playerAnswer)
                        .append("\",\"isCorrect\":").append(isCorrect).append("}");
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
        response.getWriter().write("{\"results\":" + results.toString() + ",\"correctAnswer\":\"" + correctAnswer + "\"}");
    }
}