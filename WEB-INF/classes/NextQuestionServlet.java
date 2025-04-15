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

@WebServlet("/nextQuestion")
public class NextQuestionServlet extends HttpServlet {
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
                Integer currentQuestionId = null;

                String getSessionSql = "SELECT currentQuestionId FROM quizSessions WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(getSessionSql)) {
                    stmt.setString(1, sessionId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        currentQuestionId = rs.getInt("currentQuestionId");
                        if (rs.wasNull()) {
                            currentQuestionId = null;
                        }
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("Session not found: " + sessionId);
                        return;
                    }
                }

                // If currentQuestionId is null, get first question in category
                if (currentQuestionId == null) {
                    // Step 2: Find the category (we assume first question for the session is from a known category)
                    String getCategorySql = "SELECT questionType FROM questions LIMIT 1"; // fallback
                    String category = null;

                    try (PreparedStatement stmt = conn.prepareStatement(getCategorySql)) {
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            category = rs.getString("questionType");
                        } else {
                            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            response.getWriter().write("No categories found in questions table");
                            return;
                        }
                    }

                    // Step 3: Get first question for that category
                    String firstQuestionSql = "SELECT id FROM questions WHERE questionType = ? ORDER BY id ASC LIMIT 1";
                    int firstQuestionId;
                    try (PreparedStatement stmt = conn.prepareStatement(firstQuestionSql)) {
                        stmt.setString(1, category);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            firstQuestionId = rs.getInt("id");
                        } else {
                            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            response.getWriter().write("No questions found in category: " + category);
                            return;
                        }
                    }

                    // Step 4: Update session
                    String updateSql = "UPDATE quizSessions SET currentQuestionId = ? WHERE id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                        stmt.setInt(1, firstQuestionId);
                        stmt.setString(2, sessionId);
                        stmt.executeUpdate();
                    }

                    response.setContentType("text/plain");
                    response.getWriter().write("Started quiz with first question: " + firstQuestionId);
                    return;
                }
            

                // Get the current question ID and category from quizSessions
//                 String getSessionSql = "SELECT currentQuestionId FROM quizSessions WHERE id = ?";
//                 // int currentQuestionId;
//                 try (PreparedStatement stmt = conn.prepareStatement(getSessionSql)) {
//                     stmt.setString(1, sessionId);
//                     ResultSet rs = stmt.executeQuery();
//                     if (!rs.next()) {
//                         response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//                         response.getWriter().write("Session not found: " + sessionId);
//                         return;
//                     }
//                     currentQuestionId = rs.getInt("currentQuestionId");
// if (rs.wasNull()) {
//     response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//     response.getWriter().write("Quiz has not started yet (questionId is null)");
//     return;
// }

    

                // Get the category of the current question
                String getCategorySql = "SELECT questionType FROM questions WHERE id = ?";
                String category;
                try (PreparedStatement stmt = conn.prepareStatement(getCategorySql)) {
                    stmt.setInt(1, currentQuestionId);
                    ResultSet rs = stmt.executeQuery();
                    if (!rs.next()) {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("Question not found: " + currentQuestionId);
                        return;
                    }
                    category = rs.getString("questionType");
                }

                // Fetch the next question ID in the same category
                String getNextQuestionSql = "SELECT id FROM questions WHERE questionType = ? AND id > ? LIMIT 1";
                int nextQuestionId = -1;
                try (PreparedStatement stmt = conn.prepareStatement(getNextQuestionSql)) {
                    stmt.setString(1, category);
                    stmt.setInt(2, currentQuestionId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        nextQuestionId = rs.getInt("id");
                    }
                }

                if (nextQuestionId == -1) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("No more questions in category: " + category);
                    return;
                }

                // Update the currentQuestionId in quizSessions
                String updateSessionSql = "UPDATE quizSessions SET currentQuestionId = ? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSessionSql)) {
                    stmt.setInt(1, nextQuestionId);
                    stmt.setString(2, sessionId);
                    stmt.executeUpdate();
                }

                // Respond with success message
                response.setContentType("text/plain");
                response.getWriter().write("Moved to next question successfully");
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