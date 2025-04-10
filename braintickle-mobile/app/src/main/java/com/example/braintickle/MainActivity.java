package com.example.braintickle;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import android.widget.Toast;

// Add the R import
import com.example.braintickle.R;

public class MainActivity extends AppCompatActivity {
    private String playerName = "";
    private int questionId = 1; // Start with question 1

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout playerInputLayout = findViewById(R.id.playerInputLayout);
        LinearLayout answerLayout = findViewById(R.id.answerLayout);
        EditText playerNameInput = findViewById(R.id.playerNameInput);
        Button startButton = findViewById(R.id.startButton);

        Button btnA = findViewById(R.id.btnA);
        Button btnB = findViewById(R.id.btnB);
        Button btnC = findViewById(R.id.btnC);
        Button btnD = findViewById(R.id.btnD);

        startButton.setOnClickListener(v -> {
            playerName = playerNameInput.getText().toString().trim();
            if (playerName.isEmpty()) {
                playerNameInput.setError("Please enter your name");
                return;
            }
            playerInputLayout.setVisibility(View.GONE);
            answerLayout.setVisibility(View.VISIBLE);
        });

        btnA.setOnClickListener(v -> submitAnswer("A"));
        btnB.setOnClickListener(v -> submitAnswer("B"));
        btnC.setOnClickListener(v -> submitAnswer("C"));
        btnD.setOnClickListener(v -> submitAnswer("D"));
    }

    private void submitAnswer(String answer) {
        new Thread(() -> {
            try {
                URL url = new URL("http://10.0.2.2:8080/braintickle/submitAnswer");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                String data = "player=" + URLEncoder.encode(playerName, "UTF-8") +
                        "&questionId=" + questionId +
                        "&playerAnswer=" + answer;
                OutputStream os = conn.getOutputStream();
                os.write(data.getBytes());
                os.flush();

                // Read response
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = reader.readLine();
                reader.close();

                conn.disconnect();

                // Show response and move to next question
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, response, Toast.LENGTH_SHORT).show();
                    questionId++;
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}