import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.sql.*;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/submitAnswer")
public class SubmitAnswerServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String player = request.getParameter("player");
        int questionId = Integer.parseInt(request.getParameter("questionId"));
        String playerAnswer = request.getParameter("playerAnswer");

        // Database connection
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/braintickle", "root", "MySecurePassword")) {
            String sql = "INSERT INTO playerAnswers (player, questionId, playerAnswer) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, player);
            stmt.setInt(2, questionId);
            stmt.setString(3, playerAnswer);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        response.setContentType("text/plain");
        response.getWriter().write("Answer received");
    }
}