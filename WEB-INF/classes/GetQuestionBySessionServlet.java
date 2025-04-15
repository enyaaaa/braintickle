import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.sql.*;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/getQuestionBySession")
public class GetQuestionBySessionServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sessionId = request.getParameter("sessionId");
        if (sessionId == null || sessionId.trim().isEmpty()) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Session ID is required\"}");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/braintickle", "root", "MySecurePassword")) {
            String sql = "SELECT currentQuestionId, category FROM quizSessions WHERE id = ? AND status = 'active'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, Integer.parseInt(sessionId));
            ResultSet rs = stmt.executeQuery();

            int currentQuestionId;
            String category;
            if (rs.next()) {
                currentQuestionId = rs.getInt("currentQuestionId");
                category = rs.getString("category");
            } else {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\":\"Session not found or not active\"}");
                return;
            }

            // Check if we've exceeded the question limit (e.g., 5 questions)
            if (currentQuestionId > 5) {
                sql = "UPDATE quizSessions SET status = 'finished' WHERE id = ?";
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, Integer.parseInt(sessionId));
                stmt.executeUpdate();

                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"error\":\"Quiz finished\"}");
                return;
            }

            sql = "SELECT * FROM questions WHERE id = ? AND category = ? LIMIT 1";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentQuestionId);
            stmt.setString(2, category);
            rs = stmt.executeQuery();

            if (rs.next()) {
                String jsonResponse = String.format(
                    "{\"id\":%d,\"question\":\"%s\",\"option1\":\"%s\",\"option2\":\"%s\",\"option3\":\"%s\",\"option4\":\"%s\",\"answer\":\"%s\"}",
                    rs.getInt("id"),
                    rs.getString("question").replace("\"", "\\\""),
                    rs.getString("option1").replace("\"", "\\\""),
                    rs.getString("option2").replace("\"", "\\\""),
                    rs.getString("option3").replace("\"", "\\\""),
                    rs.getString("option4").replace("\"", "\\\""),
                    rs.getString("answer")
                );
                response.setContentType("application/json");
                response.getWriter().write(jsonResponse);
            } else {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\":\"Question not found\"}");
            }
        } catch (SQLException e) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Database error: " + e.getMessage() + "\"}");
        }
    }
}