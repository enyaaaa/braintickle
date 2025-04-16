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
            response.getWriter().write("{\"error\":\"Missing sessionId\"}");
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish database connection
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Get the current question ID from quizSessions
                //String getQuestionIdSql = "SELECT currentQuestionId FROM quizSessions WHERE id = ?";
                //int questionId;
                String getSessionInfoSql = "SELECT status, currentQuestionId FROM quizSessions WHERE id = ?";
                String quizStatus = null;
                int questionId = 0;
                try (PreparedStatement stmt = conn.prepareStatement(getSessionInfoSql)) {
                    stmt.setString(1, sessionId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        quizStatus = rs.getString("status");
                        questionId = rs.getInt("currentQuestionId");
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("{\"status\":\"error\", \"message\":\"Session not found\"}");
                        return;
                    }
                }
                if ("waiting".equalsIgnoreCase(quizStatus)) {
                    response.getWriter().write("{\"status\":\"waiting\"}");
                    return;
                } else if ("ended".equalsIgnoreCase(quizStatus)) {
                    response.getWriter().write("{\"status\":\"ended\"}");
                    return;
                } else if ("started".equalsIgnoreCase(quizStatus) && questionId > 0) {
                    // Fetch the question details
                    String getQuestionSql = "SELECT id, questionType, question, option1, option2, option3, option4 FROM questions WHERE id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(getQuestionSql)) {
                        stmt.setInt(1, questionId);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            String jsonResponse = "{\"status\":\"started\", " +
                                                  "\"questionId\":\"" + rs.getInt("id") + "\", " +
                                                  "\"questionType\":\"" + rs.getString("questionType") + "\", " +
                                                  "\"question_text\":\"" + rs.getString("question") + "\", " +
                                                  "\"option1\":\"" + rs.getString("option1") + "\", " +
                                                  "\"option2\":\"" + rs.getString("option2") + "\", " +
                                                  "\"option3\":\"" + rs.getString("option3") + "\", " +
                                                  "\"option4\":\"" + rs.getString("option4") + "\"}";
                            response.getWriter().write(jsonResponse);
                        } else {
                            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            response.getWriter().write("{\"status\":\"error\", \"message\":\"Question not found\"}");
                        }
                    }
                } else {
                    response.getWriter().write("{\"status\":\"waiting\", \"message\":\"Waiting for quiz to start\"}"); // Default waiting message if status isn't clear or no question ID yet
                }

            }
        } catch (ClassNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\", \"message\":\"Database driver not found: " + e.getMessage() + "\"}");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\", \"message\":\"Database error: " + e.getMessage() + "\"}");
        }
    }
}
                // try (PreparedStatement stmt = conn.prepareStatement(getQuestionIdSql)) {
                //     stmt.setString(1, sessionId);
                //     ResultSet rs = stmt.executeQuery();
                //     if (rs.next()) {
                //         questionId = rs.getInt("currentQuestionId");
                //     } else {
                //         response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                //         response.getWriter().write("Session not found: " + sessionId);
                //         return;
                //     }
                // }

    //             // Fetch the question details
    //             String getQuestionSql = "SELECT id, question, option1, option2, option3, option4, answer FROM questions WHERE id = ?";
    //             try (PreparedStatement stmt = conn.prepareStatement(getQuestionSql)) {
    //                 stmt.setInt(1, questionId);
    //                 ResultSet rs = stmt.executeQuery();
    //                 if (rs.next()) {
    //                     String questionData = rs.getInt("id") + "|" +
    //                                          rs.getString("question") + "|" +
    //                                          rs.getString("option1") + "|" +
    //                                          rs.getString("option2") + "|" +
    //                                          rs.getString("option3") + "|" +
    //                                          rs.getString("option4") + "|" +
    //                                          rs.getInt("answer");
    //                     response.setContentType("text/plain");
    //                     response.getWriter().write(questionData);
    //                 } else {
    //                     response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    //                     response.getWriter().write("Question not found for ID: " + questionId);
    //                 }
    //             }
    //         }
    //     } catch (ClassNotFoundException e) {
    //         response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    //         response.getWriter().write("Database driver not found: " + e.getMessage());
    //     } catch (SQLException e) {
    //         response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    //         response.getWriter().write("Database error: " + e.getMessage());
    //     }
    // }
