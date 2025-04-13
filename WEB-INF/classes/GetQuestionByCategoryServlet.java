import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.sql.*;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/getQuestionByCategory")
public class GetQuestionByCategoryServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String category = request.getParameter("category");
        if (category == null || category.trim().isEmpty()) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Category parameter is required\"}");
            return;
        }

        String questionText = null;
        String optionA = null, optionB = null, optionC = null, optionD = null;
        String correctAnswer = null; // Declare correctAnswer here

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/braintickle", "root", "MySecurePassword")) {
            // Prepare SQL query to fetch a random question from the specified category
            String sql = "SELECT question, option1, option2, option3, option4, answer FROM questions WHERE questionType = ? ORDER BY RAND() LIMIT 1";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                questionText = rs.getString("question");
                optionA = rs.getString("option1");
                optionB = rs.getString("option2");
                optionC = rs.getString("option3");
                optionD = rs.getString("option4");
                correctAnswer = rs.getString("answer");
            } else {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\":\"No questions found for category: " + category + "\"}");
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
        String jsonResponse = "{"
                + "\"question\":\"" + (questionText != null ? escapeJson(questionText) : "") + "\","
                + "\"option1\":\"" + (optionA != null ? escapeJson(optionA) : "") + "\","
                + "\"option2\":\"" + (optionB != null ? escapeJson(optionB) : "") + "\","
                + "\"option3\":\"" + (optionC != null ? escapeJson(optionC) : "") + "\","
                + "\"option4\":\"" + (optionD != null ? escapeJson(optionD) : "") + "\","
                + "\"answer\":\"" + (correctAnswer != null ? correctAnswer : "") + "\""
                + "}";

        response.getWriter().write(jsonResponse);
    }

    // Helper method to escape special characters in JSON strings
    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}