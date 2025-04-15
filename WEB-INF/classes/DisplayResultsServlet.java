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

@WebServlet("/displayResults")
public class DisplayResultsServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/braintickle?useSSL=false";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "your_password";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        int sessionId = Integer.parseInt(request.getParameter("sessionId"));
        StringBuilder jsonResponse = new StringBuilder("{");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            // Get current question from session
            String sessionSql = "SELECT currentQuestionId FROM quizSessions WHERE id = ?";
            PreparedStatement sessionStmt = conn.prepareStatement(sessionSql);
            sessionStmt.setInt(1, sessionId);
            ResultSet sessionRs = sessionStmt.executeQuery();
            int questionId = 1;
            if (sessionRs.next()) {
                questionId = sessionRs.getInt("currentQuestionId");
            }

            // Get question details
            String questionSql = "SELECT questionType, question, image, option1, option2, option3, option4, answer FROM questions WHERE id = ?";
            PreparedStatement questionStmt = conn.prepareStatement(questionSql);
            questionStmt.setInt(1, questionId);
            ResultSet questionRs = questionStmt.executeQuery();
            if (questionRs.next()) {
                jsonResponse.append("\"questionType\":\"").append(questionRs.getString("questionType")).append("\",");
                jsonResponse.append("\"question_text\":\"").append(questionRs.getString("question")).append("\",");
                jsonResponse.append("\"image\":\"").append(questionRs.getString("image")).append("\",");
                jsonResponse.append("\"option1\":\"").append(questionRs.getString("option1")).append("\",");
                jsonResponse.append("\"option2\":\"").append(questionRs.getString("option2")).append("\",");
                jsonResponse.append("\"option3\":\"").append(questionRs.getString("option3")).append("\",");
                jsonResponse.append("\"option4\":\"").append(questionRs.getString("option4")).append("\",");
                jsonResponse.append("\"answer\":\"").append(questionRs.getString("answer")).append("\"");
            } else {
                jsonResponse.append("\"error\":\"Question not found\"");
            }

            // Get player answers
            String answerSql = "SELECT player, playerAnswer, score FROM playerAnswers WHERE questionId = ? AND sessionId = ?";
            PreparedStatement answerStmt = conn.prepareStatement(answerSql);
            answerStmt.setInt(1, questionId);
            answerStmt.setInt(2, sessionId);
            ResultSet answerRs = answerStmt.executeQuery();
            jsonResponse.append(",\"answers\":[");
            boolean firstAnswer = true;
            while (answerRs.next()) {
                if (!firstAnswer) {
                    jsonResponse.append(",");
                }
                jsonResponse.append("{");
                jsonResponse.append("\"player\":\"").append(answerRs.getString("player")).append("\",");
                jsonResponse.append("\"playerAnswer\":\"").append(answerRs.getString("playerAnswer")).append("\",");
                jsonResponse.append("\"score\":").append(answerRs.getInt("score"));
                jsonResponse.append("}");
                firstAnswer = false;
            }
            jsonResponse.append("]");

            // Get leaderboard
            String leaderboardSql = "SELECT player, SUM(score) as totalScore FROM playerAnswers WHERE sessionId = ? GROUP BY player ORDER BY totalScore DESC";
            PreparedStatement leaderboardStmt = conn.prepareStatement(leaderboardSql);
            leaderboardStmt.setInt(1, sessionId);
            ResultSet leaderboardRs = leaderboardStmt.executeQuery();
            jsonResponse.append(",\"leaderboard\":[");
            boolean firstEntry = true;
            while (leaderboardRs.next()) {
                if (!firstEntry) {
                    jsonResponse.append(",");
                }
                jsonResponse.append("{");
                jsonResponse.append("\"player\":\"").append(leaderboardRs.getString("player")).append("\",");
                jsonResponse.append("\"totalScore\":").append(leaderboardRs.getInt("totalScore"));
                jsonResponse.append("}");
                firstEntry = false;
            }
            jsonResponse.append("]");

            sessionStmt.close();
            questionStmt.close();
            answerStmt.close();
            leaderboardStmt.close();
            conn.close();

            jsonResponse.append("}");
            response.getWriter().write(jsonResponse.toString());
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}