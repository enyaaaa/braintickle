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

@WebServlet("/getLeaderboard")
public class GetLeaderboardServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sessionId = request.getParameter("sessionId");
        response.setContentType("text/plain");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/braintickle", "root", "MySecurePassword");

            // Calculate scores: 100 points for each correct answer
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT pa.player, COUNT(*) as score " +
                "FROM playerAnswers pa " +
                "JOIN questions q ON pa.questionId = q.id " +
                "WHERE pa.sessionId = ? AND pa.playerAnswer = q.answer " +
                "GROUP BY pa.player " +
                "ORDER BY score DESC"
            );
            stmt.setString(1, sessionId);
            ResultSet rs = stmt.executeQuery();

            StringBuilder leaderboard = new StringBuilder();
            while (rs.next()) {
                String player = rs.getString("player");
                int score = rs.getInt("score") * 100;
                if (leaderboard.length() > 0) {
                    leaderboard.append("|");
                }
                leaderboard.append(player).append(":").append(score);
            }

            response.getWriter().write(leaderboard.length() > 0 ? leaderboard.toString() : "No scores yet");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error fetching leaderboard");
        }
    }
}