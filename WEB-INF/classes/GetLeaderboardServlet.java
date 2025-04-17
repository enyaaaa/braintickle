package com.example.braintickle;

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

@WebServlet("/getLeaderboard")
public class GetLeaderboardServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/braintickle";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "MySecurePassword";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String sessionId = request.getParameter("sessionId");

        // Validate sessionId parameter
        if (sessionId == null || sessionId.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing sessionId");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Query to get all players in the session
                String allPlayersSql = "SELECT DISTINCT player FROM playerAnswers WHERE sessionId = ?";
                try (PreparedStatement stmt = conn.prepareStatement(allPlayersSql)) {
                    stmt.setString(1, sessionId);
                    ResultSet rs = stmt.executeQuery();

                    StringBuilder leaderboard = new StringBuilder();
                    while (rs.next()) {
                        String player = rs.getString("player");

                        // Query to count correct answers for this player
                        String scoreSql = "SELECT COUNT(*) as score FROM playerAnswers WHERE sessionId = ? AND player = ? AND isCorrect = 1";
                        try (PreparedStatement scoreStmt = conn.prepareStatement(scoreSql)) {
                            scoreStmt.setString(1, sessionId);
                            scoreStmt.setString(2, player);
                            ResultSet scoreRs = scoreStmt.executeQuery();
                            int score = 0;
                            if (scoreRs.next()) {
                                score = scoreRs.getInt("score") * 100; // 100 points per correct answer
                            }

                            if (leaderboard.length() > 0) {
                                leaderboard.append("|");
                            }
                            leaderboard.append(player).append(":").append(score);
                        }
                    }

                    // If no players, return "No scores yet"
                    response.setContentType("text/plain");
                    response.getWriter().write(leaderboard.length() > 0 ? leaderboard.toString() : "No scores yet");
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