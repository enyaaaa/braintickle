import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.sql.*;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/submitAnswer")
public class SubmitAnswerServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String player = request.getParameter("player");
        int questionId;
        String playerAnswer = request.getParameter("playerAnswer");

        try {
            questionId = Integer.parseInt(request.getParameter("questionId"));
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid questionId");
            return;
        }

        String correctAnswerFromDB = null;

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/braintickle", "root", "MySecurePassword")) {
            String sql = "INSERT INTO playerAnswers (player, questionId, playerAnswer) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE playerAnswer = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, player);
            stmt.setInt(2, questionId);
            stmt.setString(3, playerAnswer);
            stmt.setString(4, playerAnswer);
            stmt.executeUpdate();

            sql = "SELECT answer FROM questions WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, questionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                correctAnswerFromDB = rs.getString("answer");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Database error: " + e.getMessage());
            return;
        }

        String result = "Incorrect.";
        // Assuming your 'answer' column in the 'questions' table stores the correct option (e.g., "A", "B", "C", "D")
        if (correctAnswerFromDB != null && correctAnswerFromDB.trim().equalsIgnoreCase(playerAnswer.trim())) {
            result = "Correct!";
        }

        response.getWriter().write("Answer received");
    }
}