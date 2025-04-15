import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.sql.*;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/submitAnswer")
public class SubmitAnswerServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String player = request.getParameter("player");
        String questionId = request.getParameter("questionId");
        String playerAnswer = request.getParameter("playerAnswer");
        String sessionId = request.getParameter("sessionId");

        if (player == null || questionId == null || playerAnswer == null || sessionId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing parameters");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/braintickle", "root", "MySecurePassword")) {
            // Get the correct answer from the questions table
            String sql = "SELECT answer FROM questions WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, Integer.parseInt(questionId));
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("Question not found");
                return;
            }

            String correctAnswer = rs.getString("answer");
            boolean isCorrect = playerAnswer.equals(correctAnswer);
            int score = isCorrect ? 10 : 0; // 10 points for correct answer, 0 for incorrect

            // Insert the answer and score into playerAnswers
            sql = "INSERT INTO playerAnswers (player, questionId, playerAnswer, sessionId, score) VALUES (?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, player);
            stmt.setInt(2, Integer.parseInt(questionId));
            stmt.setString(3, playerAnswer);
            stmt.setString(4, sessionId);
            stmt.setInt(5, score);
            stmt.executeUpdate();

            response.setContentType("text/plain");
            response.getWriter().write(isCorrect ? "Correct! +10 points" : "Incorrect");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Database error: " + e.getMessage());
        }
    }
}