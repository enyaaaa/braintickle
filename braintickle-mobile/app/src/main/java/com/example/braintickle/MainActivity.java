package com.example.braintickle;

import android.os.Bundle;
import android.util.Log;
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
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MainActivity extends AppCompatActivity {
    private TextView sessionInfoText, questionTypeText, questionText, resultText;
    private Button optionA, optionB, optionC, optionD;
    private String playerName;
    private String sessionId;
    private String currentQuestionId;
    private final String SERVER_URL = "http://10.0.2.2:9999/braintickle/submitAnswer";
    private final String WEBSOCKET_URL = "ws://10.0.2.2:9999/braintickle/quizSocket/";
    private WebSocket webSocket;
    private OkHttpClient client;

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
        questionTypeText = findViewById(R.id.questionTypeText);
        questionText = findViewById(R.id.questionText);
        resultText = findViewById(R.id.resultText);
        optionA = findViewById(R.id.optionA);
        optionB = findViewById(R.id.optionB);
        optionC = findViewById(R.id.optionC);
        optionD = findViewById(R.id.optionD);

        sessionInfoText.setText("Session ID: " + sessionId);

        // Initialize WebSocket
        client = new OkHttpClient();
        connectWebSocket();

        // Fallback to polling if WebSocket fails
        loadQuestion();

        optionA.setOnClickListener(v -> submitAnswer("A"));
        optionB.setOnClickListener(v -> submitAnswer("B"));
        optionC.setOnClickListener(v -> submitAnswer("C"));
        optionD.setOnClickListener(v -> submitAnswer("D"));
    }

    private void connectWebSocket() {
        String wsUrl = WEBSOCKET_URL + sessionId + "/" + playerName;
        Log.d("DEBUG", "Connecting to WebSocket: " + wsUrl);

        Request request = new Request.Builder()
                .url(wsUrl)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "WebSocket connected", Toast.LENGTH_SHORT).show());
                Log.d("DEBUG", "WebSocket connected");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d("DEBUG", "WebSocket message received: " + text);
                try {
                    JSONObject data = new JSONObject(text);
                    String event = data.optString("event");

                    switch (event) {
                        case "quizStarted":
                            runOnUiThread(() -> resultText.setText("Quiz has started!"));
                            break;

                        case "questionFetched":
                            currentQuestionId = data.optString("questionId");
                            runOnUiThread(() -> {
                                questionTypeText.setText(data.optString("questionType"));
                                questionText.setText(data.optString("question_text"));
                                optionA.setText("A: " + data.optString("option1"));
                                optionB.setText("B: " + data.optString("option2"));
                                optionC.setText("C: " + data.optString("option3"));
                                optionD.setText("D: " + data.optString("option4"));
                                resultText.setText("");
                            });
                            break;

                        case "nextQuestion":
                            runOnUiThread(() -> resultText.setText("Moving to next question..."));
                            break;

                        case "sessionEnded":
                            runOnUiThread(() -> {
                                questionText.setText("Session ended.");
                                questionTypeText.setText("");
                                optionA.setText("A:");
                                optionB.setText("B:");
                                optionC.setText("C:");
                                optionD.setText("D:");
                                resultText.setText("Quiz session has ended.");
                            });
                            break;

                        default:
                            Log.d("DEBUG", "Unknown WebSocket event: " + event);
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> resultText.setText("Error parsing WebSocket message: " + e.getMessage()));
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
                Log.d("DEBUG", "WebSocket closing: " + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "WebSocket failed: " + t.getMessage(), Toast.LENGTH_SHORT).show());
                Log.d("DEBUG", "WebSocket failure: " + t.getMessage());
                // Fallback to polling
                loadQuestion();
            }
        });
    }

    private void loadQuestion() {
        String DISPLAY_URL = "http://10.0.2.2:9999/braintickle/displayResults?sessionId=" + sessionId;
        Log.d("DEBUG", "Loading question from: " + DISPLAY_URL);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(DISPLAY_URL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> resultText.setText("Error loading question: " + e.getMessage()));
                // Retry after a delay even on failure
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                        MainActivity.this::loadQuestion, 3000
                );
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
                                questionTypeText.setText(""); // Clear old type
                                optionA.setText("A:");
                                optionB.setText("B:");
                                optionC.setText("C:");
                                optionD.setText("D:");
                            });

                            // 🔁 Retry after 3 seconds
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                                    MainActivity.this::loadQuestion, 3000
                            );
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
                        // Continue polling for the next question
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                                MainActivity.this::loadQuestion, 3000
                        );
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> resultText.setText("Error parsing question: " + e.getMessage()));
                    // Retry after a delay
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                            MainActivity.this::loadQuestion, 3000
                    );
                }
            }
        });
    }

    private void submitAnswer(String playerAnswer) {
        if (currentQuestionId == null || currentQuestionId.isEmpty()) {
            Toast.makeText(this, "No question loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        OkHttpClient client = new OkHttpClient();
        FormBody formBody = new FormBody.Builder()
                .add("player", playerName)
                .add("questionId", currentQuestionId)// Should be dynamic based on session
                .add("playerAnswer", playerAnswer)
                .add("sessionId", String.valueOf(sessionId))
                .build();            //    private final int sessionId = 1; // Hardcoded for now

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
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocket != null) {
            webSocket.close(1000, "Activity destroyed");
            Log.d("DEBUG", "WebSocket closed on activity destroy");
        }
    }
}