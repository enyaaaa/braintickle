package com.example.braintickle;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private TextView sessionInfoText, sessionStatusText, questionTypeText, questionText, resultText, timerText, scoreText;
    private Button optionA, optionB, optionC, optionD;
    private String playerName;
    private String sessionId;
    private String currentQuestionId;
    private boolean isSessionEnded = false;
    private boolean hasSubmitted = false;
    private boolean quizStarted = false;
    private int currentQuestion = 1; // Track question number locally
    private final int TOTAL_QUESTIONS = 5; // Match with quiz.html
    private final String SERVER_URL = "http://10.0.2.2:9999/braintickle/submitAnswer";
    private final String DISPLAY_URL = "http://10.0.2.2:9999/braintickle/displayResults?sessionId=";
    private final String LEADERBOARD_URL = "http://10.0.2.2:9999/braintickle/getLeaderboard?sessionId=";
    private OkHttpClient client;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long remainingTimeSeconds;

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
        timerText = findViewById(R.id.timerText);
        scoreText = findViewById(R.id.scoreText);
        optionA = findViewById(R.id.optionA);
        optionB = findViewById(R.id.optionB);
        optionC = findViewById(R.id.optionC);
        optionD = findViewById(R.id.optionD);

        sessionInfoText.setText("Session ID: " + sessionId);
        questionText.setText("Waiting for quiz to start..."); // Initial waiting message
        disableOptions();

        client = new OkHttpClient();
        timerHandler = new Handler(Looper.getMainLooper());

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (remainingTimeSeconds > 0 && quizStarted && !hasSubmitted) {
                    remainingTimeSeconds--;
                    timerText.setText(String.valueOf(remainingTimeSeconds));
                    if (remainingTimeSeconds <= 0) {
                        disableOptions();
                    } else {
                        timerHandler.postDelayed(this, 1000);
                    }
                } else {
                    timerText.setText("0");
                }
            }
        };

        loadQuestion();

        optionA.setOnClickListener(v -> submitAnswer("A"));
        optionB.setOnClickListener(v -> submitAnswer("B"));
        optionC.setOnClickListener(v -> submitAnswer("C"));
        optionD.setOnClickListener(v -> submitAnswer("D"));
    }

    private void disableOptions() {
        optionA.setEnabled(false);
        optionB.setEnabled(false);
        optionC.setEnabled(false);
        optionD.setEnabled(false);
        optionA.setAlpha(0.5f);
        optionB.setAlpha(0.5f);
        optionC.setAlpha(0.5f);
        optionD.setAlpha(0.5f);
    }

    private void enableOptions() {
        optionA.setEnabled(true);
        optionB.setEnabled(true);
        optionC.setEnabled(true);
        optionD.setEnabled(true);
        optionA.setAlpha(1.0f);
        optionB.setAlpha(1.0f);
        optionC.setAlpha(1.0f);
        optionD.setAlpha(1.0f);
    }

    private void loadQuestion() {
        hasSubmitted = false;
        String url = DISPLAY_URL + sessionId;
        Log.d("DEBUG", "Loading question from: " + url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> resultText.setText("Error loading question: " + e.getMessage()));
                new Handler(Looper.getMainLooper()).postDelayed(MainActivity.this::loadQuestion, 3000);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                try {
                    JSONObject data = new JSONObject(responseBody);
                    if (data.has("status") && data.getString("status").equals("waiting")) {
                        quizStarted = false;
                        runOnUiThread(() -> {
                            questionText.setText("Waiting for quiz to start...");
                            questionTypeText.setText("");
                            timerText.setText("");
                            disableOptions();
                        });
                        new Handler(Looper.getMainLooper()).postDelayed(MainActivity.this::loadQuestion, 3000);
                        return;
                    } else if (data.has("status") && data.getString("status").equals("ended")) {
                        isSessionEnded = true;
                        runOnUiThread(() -> {
                            sessionStatusText.setText("Session Ended");
                            questionText.setText("Session ended.");
                            questionTypeText.setText("");
                            timerText.setText("");
                            disableOptions();
                            resultText.setText("Quiz session has ended. Redirecting...");
                            // Redirect to JoinSessionActivity
                            Intent intent = new Intent(MainActivity.this, JoinSessionActivity.class);
                            startActivity(intent);
                            finish(); // Close the current activity
                        });
                        return;
                    }

                    quizStarted = true;
                    currentQuestionId = data.optString("questionId");
                    remainingTimeSeconds = data.optLong("remainingTime", 30);
                    if (remainingTimeSeconds < 0) remainingTimeSeconds = 0;
                    runOnUiThread(() -> {
                        questionTypeText.setText(data.optString("questionType"));
                        questionText.setText(data.optString("question_text"));
                        timerText.setText(String.valueOf(remainingTimeSeconds));
                        optionA.setText("A: " + data.optString("option1"));
                        optionB.setText("B: " + data.optString("option2"));
                        optionC.setText("C: " + data.optString("option3"));
                        optionD.setText("D: " + data.optString("option4"));

                        if (!hasSubmitted) enableOptions();

                        timerHandler.removeCallbacks(timerRunnable);
                        if (remainingTimeSeconds > 0) timerHandler.post(timerRunnable);
                        else disableOptions();
                        new Handler(Looper.getMainLooper()).postDelayed(MainActivity.this::loadQuestion, 3000);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> resultText.setText("Error parsing question: " + e.getMessage()));
                    new Handler(Looper.getMainLooper()).postDelayed(MainActivity.this::loadQuestion, 3000);
                }
            }
        });
    }

    private void submitAnswer(String playerAnswer) {
        if (!quizStarted) {
            Toast.makeText(this, "Quiz has not started yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (hasSubmitted || remainingTimeSeconds <= 0 || currentQuestionId == null || currentQuestionId.isEmpty()) {
            Toast.makeText(this, "Cannot submit answer at this time", Toast.LENGTH_SHORT).show();
            return;
        }
        disableOptions();
        hasSubmitted = true;
        currentQuestion++; // Increment question counter on submit

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
                    enableOptions();
                    hasSubmitted = false;
                    currentQuestion--; // Roll back if submission fails
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 409) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Answer already submitted.", Toast.LENGTH_SHORT).show());
                } else {
                    String responseBody = response.body().string();
                    runOnUiThread(() -> {
                        resultText.setText(responseBody);
                        Toast.makeText(MainActivity.this, "Answer submitted", Toast.LENGTH_SHORT).show();
                        if (currentQuestion > TOTAL_QUESTIONS) { // Fallback check
                            isSessionEnded = true;
                            sessionStatusText.setText("Session Ended");
                            questionText.setText("Session ended.");
                            disableOptions();
                            resultText.setText("Quiz session has ended. Redirecting...");
                            Intent intent = new Intent(MainActivity.this, JoinSessionActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                }
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
                    if (responseBody.equals("No scores yet")) {
                        resultText.setText("No scores yet");
                    } else {
                        resultText.setText(responseBody); // Display leaderboard text temporarily
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
    }
}