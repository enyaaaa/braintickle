import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.sql.*;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/checkAnswer")
public class CheckAnswerServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String player = request.getParameter("player");
        int questionId = Integer.parseInt(request.getParameter("questionId"));

        String playerAnswer = null;
        String correctAnswer = null;

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/braintickle", "root", "MySecurePassword")) {
            // Get player's answer
            String sql = "SELECT playerAnswer FROM playerAnswers WHERE player = ? AND questionId = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, player);
            stmt.setInt(2, questionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) playerAnswer = rs.getString("playerAnswer");

            // Get correct answer
            sql = "SELECT answer FROM questions WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, questionId);
            rs = stmt.executeQuery();
            if (rs.next()) correctAnswer = rs.getString("answer");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String result = (playerAnswer != null && playerAnswer.equals(correctAnswer)) ? "Correct!" : "Wrong!";
        response.setContentType("application/json");
        response.getWriter().write("{\"playerAnswer\":\"" + (playerAnswer != null ? playerAnswer : "") + "\",\"result\":\"" + result + "\"}");
    }
}