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

@WebServlet("/getQuestionByCategory")
public class GetQuestionByCategoryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String category = request.getParameter("category");
        String sessionId = request.getParameter("sessionId");
        response.setContentType("text/plain");

        if (category != null) {
            category = category.toLowerCase();
        }

        try {
            System.out.println("GetQuestionByCategoryServlet called with category: " + category + ", sessionId: " + sessionId);
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/braintickle", "root", "MySecurePassowrd");
            PreparedStatement stmt = conn.prepareStatement("SELECT id FROM questions WHERE questionType = ? LIMIT 1");
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int questionId = rs.getInt("id");
                System.out.println("Found question ID: " + questionId + " for category: " + category);
                stmt = conn.prepareStatement("INSERT INTO quizSessions (id, currentQuestionId, status) VALUES (?, ?, 'active')");
                stmt.setString(1, sessionId);
                stmt.setInt(2, questionId);
                stmt.setString(3, "active");
                int rowsAffected = stmt.executeUpdate();
                System.out.println("Inserted session with ID: " + sessionId + ", rows affected: " + rowsAffected);
                response.getWriter().write("Session created");
            } else {
                System.out.println("No questions found for category: " + category);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No questions found for category: " + category);
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error in GetQuestionByCategoryServlet: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error initializing session: " + e.getMessage());
        }
    }
}