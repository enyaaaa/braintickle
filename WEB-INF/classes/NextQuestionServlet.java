package com.example.braintickle;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/nextQuestion")
public class NextQuestionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sessionId = request.getParameter("sessionId");
        response.setContentType("text/plain");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/braintickle", "root", "MySecurePassword");

            // Get current question ID and category
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT q.id, q.questionType " +
                "FROM quizSessions qs " +
                "JOIN questions q ON qs.currentQuestionId = q.id " +
                "WHERE qs.id = ?"
            );
            stmt.setString(1, sessionId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int currentQuestionId = rs.getInt("id");
                String category = rs.getString("questionType");

                // Ensure category is lowercase to match database
                if (category != null) {
                    category = category.toLowerCase();
                }

                // Get the next question in the same category
                stmt = conn.prepareStatement(
                    "SELECT id FROM questions " +
                    "WHERE questionType = ? AND id > ? " +
                    "ORDER BY id ASC LIMIT 1"
                );
                stmt.setString(1, category);
                stmt.setInt(2, currentQuestionId);
                rs = stmt.executeQuery();

                if (rs.next()) {
                    int nextQuestionId = rs.getInt("id");

                    // Update the session with the next question
                    stmt = conn.prepareStatement("UPDATE quizSessions SET currentQuestionId = ? WHERE id = ?");
                    stmt.setInt(1, nextQuestionId);
                    stmt.setString(2, sessionId);
                    stmt.executeUpdate();

                    response.getWriter().write("Next question set");
                } else {
                    // No more questions, mark session as inactive
                    stmt = conn.prepareStatement("UPDATE quizSessions SET status = 'inactive' WHERE id = ?");
                    stmt.setString(1, sessionId);
                    stmt.executeUpdate();
                    response.getWriter().write("No more questions");
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Session not found");
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error fetching next question");
        }
    }
}