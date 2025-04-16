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

@WebServlet("/braintickle/getLeaderboard")
public class GetLeaderboardServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("GetLeaderboardServlet initialized");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("Received request for /braintickle/getLeaderboard");
        String sessionId = request.getParameter("sessionId");
        response.setContentType("text/plain");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");

        try {
            System.out.println("Loading MySQL driver...");
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("Connecting to database...");
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/braintickle", "root", "MySecurePassword");
            System.out.println("Database connection established");

            PreparedStatement stmt = conn.prepareStatement(
                "SELECT player, COUNT(*) as score " +
                "FROM playerAnswers " +
                "WHERE sessionId = ? AND isCorrect = 1 " +
                "GROUP BY player " +
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
            System.out.println("Error in GetLeaderboardServlet: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error fetching leaderboard: " + e.getMessage());
        }
    }
}