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

@WebServlet("/getQuestionBySession")
public class GetQuestionBySessionServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/braintickle";
    private static final String DB_USER = "root"; // Replace with your MySQL username
    private static final String DB_PASSWORD = "MySecurePassword"; // Replace with your MySQL password

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String sessionId = request.getParameter("sessionId");

        if (sessionId == null || sessionId.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing sessionId");
            return;
        }

        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish database connection
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Get the current question ID from quizSessions
                String getQuestionIdSql = "SELECT currentQuestionId FROM quizSessions WHERE id = ?";
                int questionId;
                try (PreparedStatement stmt = conn.prepareStatement(getQuestionIdSql)) {
                    stmt.setString(1, sessionId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        questionId = rs.getInt("currentQuestionId");
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("Session not found: " + sessionId);
                        return;
                    }
                }

                // Fetch the question details
                String getQuestionSql = "SELECT id, question, option1, option2, option3, option4, answer FROM questions WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(getQuestionSql)) {
                    stmt.setInt(1, questionId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        String questionData = rs.getInt("id") + "|" +
                                             rs.getString("question") + "|" +
                                             rs.getString("option1") + "|" +
                                             rs.getString("option2") + "|" +
                                             rs.getString("option3") + "|" +
                                             rs.getString("option4") + "|" +
                                             rs.getInt("answer");
                        response.setContentType("text/plain");
                        response.getWriter().write(questionData);
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("Question not found for ID: " + questionId);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Database driver not found: " + e.getMessage());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Database error: " + e.getMessage());
        }
    }
}