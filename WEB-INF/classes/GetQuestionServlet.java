import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.sql.*;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/getQuestion")
public class GetQuestionServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int questionId;
        try {
            questionId = Integer.parseInt(request.getParameter("questionId"));
        } catch (NumberFormatException e) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid questionId\"}");
            return;
        }

        String question = null;
        String option1 = null, option2 = null, option3 = null, option4 = null;

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/braintickle", "root", "MySecurePassword")) {
            String sql = "SELECT question, option1, option2, option3, option4 FROM questions WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, questionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                question = rs.getString("question");
                option1 = rs.getString("option1");
                option2 = rs.getString("option2");
                option3 = rs.getString("option3");
                option4 = rs.getString("option4");
            } else {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\":\"Question not found\"}");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Database error: " + e.getMessage() + "\"}");
            return;
        }

        response.setContentType("application/json");
        response.getWriter().write("{\"question\":\"" + (question != null ? question : "") + "\",\"option1\":\"" + (option1 != null ? option1 : "") + "\",\"option2\":\"" + (option2 != null ? option2 : "") + "\",\"option3\":\"" + (option3 != null ? option3 : "") + "\",\"option4\":\"" + (option4 != null ? option4 : "") + "\"}");
    }
}