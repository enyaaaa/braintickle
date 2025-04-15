package com.example.braintickle;

import android.os.Bundle;
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
    private TextView sessionInfoText, questionTypeText, questionText, resultText;
    private Button optionA, optionB, optionC, optionD;
    private final String player = "player_" + System.currentTimeMillis(); // Made final
    private final int sessionId = 1; // Hardcoded for now
    private final String SERVER_URL = "http://localhost:9999/braintickle/submitAnswer";
    private final String DISPLAY_URL = "http://localhost:9999/braintickle/displayResults?sessionId=" + sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionInfoText = findViewById(R.id.sessionInfoText);
        questionTypeText = findViewById(R.id.questionTypeText);
        questionText = findViewById(R.id.questionText);
        resultText = findViewById(R.id.resultText);
        optionA = findViewById(R.id.optionA);
        optionB = findViewById(R.id.optionB);
        optionC = findViewById(R.id.optionC);
        optionD = findViewById(R.id.optionD);

        sessionInfoText.setText("Session ID: " + sessionId);
        loadQuestion();

        optionA.setOnClickListener(v -> submitAnswer("A"));
        optionB.setOnClickListener(v -> submitAnswer("B"));
        optionC.setOnClickListener(v -> submitAnswer("C"));
        optionD.setOnClickListener(v -> submitAnswer("D"));
    }

    private void loadQuestion() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(DISPLAY_URL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> resultText.setText("Error loading question: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                try {
                    JSONObject data = new JSONObject(responseBody);
                    runOnUiThread(() -> {
                        questionTypeText.setText(data.optString("questionType"));
                        questionText.setText(data.optString("question_text"));
                        optionA.setText("A: " + data.optString("option1"));
                        optionB.setText("B: " + data.optString("option2"));
                        optionC.setText("C: " + data.optString("option3"));
                        optionD.setText("D: " + data.optString("option4"));
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> resultText.setText("Error parsing question: " + e.getMessage()));
                }
            }
        });
    }

    private void submitAnswer(String playerAnswer) {
        OkHttpClient client = new OkHttpClient();
        FormBody formBody = new FormBody.Builder()
                .add("player", player)
                .add("questionId", "1") // Should be dynamic based on session
                .add("playerAnswer", playerAnswer)
                .add("sessionId", String.valueOf(sessionId))
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
                });
            }
        });
    }
}