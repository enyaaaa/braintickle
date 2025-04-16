package com.example.braintickle;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private TextView sessionInfoText, sessionStatusText, questionTypeText, questionText, resultText;
    private Button optionA, optionB, optionC, optionD, showLeaderboardButton;
    private RecyclerView leaderboardRecyclerView;
    private LeaderboardAdapter leaderboardAdapter;
    private String playerName;
    private String sessionId;
    private String currentQuestionId;
    private boolean isSessionEnded = false;
    private boolean isLeaderboardVisible = false;
    private final String SERVER_URL = "http://10.0.2.2:9999/braintickle/submitAnswer";
    private final String DISPLAY_URL = "http://10.0.2.2:9999/braintickle/displayResults?sessionId=";
    private final String LEADERBOARD_URL = "http://10.0.2.2:9999/getLeaderboard?sessionId=";
    private OkHttpClient client;
    private Handler leaderboardHandler;
    private Runnable leaderboardRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionId = getIntent().getStringExtra("sessionId");
        playerName = getIntent().getStringExtra("playerName");
        Log.d("DEBUG", "Received sessionId: " + sessionId);
        Log.d("DEBUG", "Received playerName: " + playerName);

        if (sessionId == null || sessionId.isEmpty() || playerName == null || playerName.isEmpty()) {
            Toast.makeText(this, "Invalid session or player name. Returning...", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sessionInfoText = findViewById(R.id.sessionInfoText);
        sessionStatusText = findViewById(R.id.sessionStatusText);
        questionTypeText = findViewById(R.id.questionTypeText);
        questionText = findViewById(R.id.questionText);
        resultText = findViewById(R.id.resultText);
        optionA = findViewById(R.id.optionA);
        optionB = findViewById(R.id.optionB);
        optionC = findViewById(R.id.optionC);
        optionD = findViewById(R.id.optionD);
        showLeaderboardButton = findViewById(R.id.showLeaderboardButton);
        leaderboardRecyclerView = findViewById(R.id.leaderboardRecyclerView);

        sessionInfoText.setText("Session ID: " + sessionId);

        // Initialize RecyclerView for leaderboard
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        leaderboardAdapter = new LeaderboardAdapter();
        leaderboardRecyclerView.setAdapter(leaderboardAdapter);

        // Initialize OkHttpClient
        client = new OkHttpClient();

        // Initialize leaderboard polling handler
        leaderboardHandler = new Handler(Looper.getMainLooper());
        leaderboardRunnable = new Runnable() {
            @Override
            public void run() {
                if (isLeaderboardVisible && !isSessionEnded) {
                    fetchLeaderboard();
                    leaderboardHandler.postDelayed(this, 5000); // Poll every 5 seconds
                }
            }
        };

        // Start polling for questions
        loadQuestion();

        // Set up button listeners
        optionA.setOnClickListener(v -> submitAnswer("A"));
        optionB.setOnClickListener(v -> submitAnswer("B"));
        optionC.setOnClickListener(v -> submitAnswer("C"));
        optionD.setOnClickListener(v -> submitAnswer("D"));
        showLeaderboardButton.setOnClickListener(v -> toggleLeaderboard());
    }

    private void loadQuestion() {
        String url = DISPLAY_URL + sessionId;
        Log.d("DEBUG", "Loading question from: " + url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> resultText.setText("Error loading question: " + e.getMessage()));
                // Retry after a delay even on failure
                new Handler(Looper.getMainLooper()).postDelayed(MainActivity.this::loadQuestion, 3000);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                try {
                    JSONObject data = new JSONObject(responseBody);
                    if (data.has("message")) {
                        String message = data.getString("message");
                        if (message.equals("Waiting for teacher to start quiz.")) {
                            runOnUiThread(() -> {
                                questionText.setText(message);
                                questionTypeText.setText("");
                                optionA.setText("A:");
                                optionB.setText("B:");
                                optionC.setText("C:");
                                optionD.setText("D:");
                            });
                            // Retry after 3 seconds
                            new Handler(Looper.getMainLooper()).postDelayed(MainActivity.this::loadQuestion, 3000);
                            return;
                        } else if (message.equals("Session ended.")) {
                            runOnUiThread(() -> {
                                isSessionEnded = true;
                                sessionStatusText.setText("Session Ended");
                                questionText.setText("Session ended.");
                                questionTypeText.setText("");
                                optionA.setText("A:");
                                optionB.setText("B:");
                                optionC.setText("C:");
                                optionD.setText("D:");
                                resultText.setText("Quiz session has ended.");
                                showLeaderboardButton.setVisibility(View.VISIBLE);
                                fetchLeaderboard(); // Fetch final leaderboard
                            });
                            return;
                        }
                    }

                    currentQuestionId = data.optString("questionId");
                    runOnUiThread(() -> {
                        questionTypeText.setText(data.optString("questionType"));
                        questionText.setText(data.optString("question_text"));
                        optionA.setText("A: " + data.optString("option1"));
                        optionB.setText("B: " + data.optString("option2"));
                        optionC.setText("C: " + data.optString("option3"));
                        optionD.setText("D: " + data.optString("option4"));
                        showLeaderboardButton.setVisibility(View.VISIBLE);
                        // Continue polling for the next question
                        new Handler(Looper.getMainLooper()).postDelayed(MainActivity.this::loadQuestion, 3000);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> resultText.setText("Error parsing question: " + e.getMessage()));
                    // Retry after a delay
                    new Handler(Looper.getMainLooper()).postDelayed(MainActivity.this::loadQuestion, 3000);
                }
            }
        });
    }

    private void submitAnswer(String playerAnswer) {
        if (currentQuestionId == null || currentQuestionId.isEmpty()) {
            Toast.makeText(this, "No question loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        FormBody formBody = new FormBody.Builder()
                .add("player", playerName)
                .add("questionId", currentQuestionId)
                .add("playerAnswer", playerAnswer)
                .add("sessionId", sessionId)
                .build();

        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    resultText.setText("Error: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Submission failed", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    resultText.setText(responseBody);
                    Toast.makeText(MainActivity.this, "Answer submitted", Toast.LENGTH_SHORT).show();
                    fetchLeaderboard(); // Fetch leaderboard after submission
                });
            }
        });
    }

    private void fetchLeaderboard() {
        String url = LEADERBOARD_URL + sessionId;
        Log.d("DEBUG", "Fetching leaderboard from: " + url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    resultText.setText("Error fetching leaderboard: " + e.getMessage());
                    Log.e("DEBUG", "Leaderboard fetch failed: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body().string();
                    runOnUiThread(() -> {
                        resultText.setText("Error fetching leaderboard: HTTP " + response.code() + " - " + errorBody);
                        Log.e("DEBUG", "Leaderboard fetch failed: HTTP " + response.code() + " - " + errorBody);
                    });
                    return;
                }
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    List<LeaderboardEntry> entries = new ArrayList<>();
                    if (responseBody.equals("No scores yet")) {
                        resultText.setText("No scores yet");
                    } else {
                        String[] leaderboardData = responseBody.split("\\|");
                        for (String entry : leaderboardData) {
                            String[] parts = entry.split(":");
                            if (parts.length == 2) {
                                String player = parts[0];
                                int score = Integer.parseInt(parts[1]);
                                entries.add(new LeaderboardEntry(player, score));
                            }
                        }
                        resultText.setText("");
                    }
                    leaderboardAdapter.updateLeaderboard(entries);
                });
            }
        });
    }

    private void toggleLeaderboard() {
        if (isLeaderboardVisible) {
            leaderboardRecyclerView.setVisibility(View.GONE);
            showLeaderboardButton.setText("Show Leaderboard");
            isLeaderboardVisible = false;
            leaderboardHandler.removeCallbacks(leaderboardRunnable);
        } else {
            leaderboardRecyclerView.setVisibility(View.VISIBLE);
            showLeaderboardButton.setText("Hide Leaderboard");
            isLeaderboardVisible = true;
            fetchLeaderboard();
            leaderboardHandler.post(leaderboardRunnable); // Start polling
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        leaderboardHandler.removeCallbacks(leaderboardRunnable);
    }
}