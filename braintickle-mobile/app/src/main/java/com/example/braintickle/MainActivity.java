package com.example.braintickle;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import android.widget.Toast;

import org.json.JSONObject;

// Add the R import
import com.example.braintickle.R;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private String playerName = "";
    private int questionId = 1; // Start with question 1
    private TextView questionText;
    private Button btnA, btnB, btnC, btnD, nextWordButton, startButton;
    private LinearLayout playerInputLayout, answerLayout;
    private TextView feedbackText;
    private String selectedCategory = "";
    private String correctAnswer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerInputLayout = findViewById(R.id.playerInputLayout);
        answerLayout = findViewById(R.id.answerLayout);
        questionText = findViewById(R.id.questionText);
        feedbackText = findViewById(R.id.feedbackText);
        nextWordButton = findViewById(R.id.nextWordButton);

        btnA = findViewById(R.id.btnA);
        btnB = findViewById(R.id.btnB);
        btnC = findViewById(R.id.btnC);
        btnD = findViewById(R.id.btnD);

        findViewById(R.id.btnGeneral).setOnClickListener(v -> setCategory("General Knowledge"));
        findViewById(R.id.btnScience).setOnClickListener(v -> setCategory("Science"));
        findViewById(R.id.btnHistory).setOnClickListener(v -> setCategory("History"));
        findViewById(R.id.btnGeography).setOnClickListener(v -> setCategory("Geography"));
        findViewById(R.id.btnMath).setOnClickListener(v -> setCategory("Math"));

        // Start Button (for player name input)
        startButton = findViewById(R.id.startButton);
        EditText playerNameInput = findViewById(R.id.playerNameInput);

        startButton.setOnClickListener(v -> {
            playerName = playerNameInput.getText().toString().trim();
            if (playerName.isEmpty()) {
                playerNameInput.setError("Please enter your name");
                return;
            }
            playerInputLayout.setVisibility(View.GONE);
            answerLayout.setVisibility(View.VISIBLE);
            fetchQuestion();
        });

        btnA.setOnClickListener(v -> submitAnswer("1"));
        btnB.setOnClickListener(v -> submitAnswer("2"));
        btnC.setOnClickListener(v -> submitAnswer("3"));
        btnD.setOnClickListener(v -> submitAnswer("4"));

        // Next word button click handler
        nextWordButton.setOnClickListener(v -> fetchQuestion());
    }

    // Fetch question from the server
    private void fetchQuestion() {
        Log.d("MainActivity", "Fetching question for category: " + selectedCategory); // Add this line
        new Thread(() -> {
            try {
                // URL for fetching the question
                String categoryParam = URLEncoder.encode(selectedCategory, "UTF-8");
                URL url = new URL("http://10.0.2.2:9999/braintickle/getQuestionByCategory?category=" + categoryParam);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                // Read response
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = reader.readLine();

                Log.d("MainActivity", "Server Response: " + response);
                reader.close();

                // Assuming the response is JSON like:
                // {"question":"To sparkle or shine with small flashes of light","option1":"scintillate","option2":"evanesce", ...}
                runOnUiThread(() -> {
                    try {
                        // Parse JSON response
                        JSONObject jsonResponse = new JSONObject(response);
                        String question = jsonResponse.getString("question");
                        String optionA = jsonResponse.getString("option1");
                        String optionB = jsonResponse.getString("option2");
                        String optionC = jsonResponse.getString("option3");
                        String optionD = jsonResponse.getString("option4");
                        String correctAnswer = jsonResponse.getString("answer"); // Retrieve the correct answer

                        // Update UI with question and options
                        questionText.setText(question);
                        btnA.setText(optionA);
                        btnB.setText(optionB);
                        btnC.setText(optionC);
                        btnD.setText(optionD);

                        this.correctAnswer = correctAnswer;

                        // Hide feedback and next word button initially
                        feedbackText.setVisibility(View.GONE);
                        nextWordButton.setVisibility(View.GONE);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                conn.disconnect();
            } catch (Exception e) {
                //e.printStackTrace();
                Log.e("MainActivity", "Error during network request: " + e.getMessage(), e); // Add this line
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error fetching question: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void submitAnswer(String answer) {
        new Thread(() -> {
            runOnUiThread(() -> {
                feedbackText.setVisibility(View.VISIBLE);
                nextWordButton.setVisibility(View.VISIBLE);

                if (answer.equals(this.correctAnswer)) {
                    feedbackText.setText("Correct!");
                    // Increment score here (you'll need to add a score variable)
                } else {
                    feedbackText.setText("Incorrect. The correct answer was option " + this.correctAnswer);
                }
            });
        }).start();
    }


    //    private void submitAnswer(String answer) {
//        new Thread(() -> {
//            try {
//                URL url = new URL("http://10.0.2.2:9999/braintickle/submitAnswer");
//                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//                conn.setRequestMethod("POST");
//                conn.setDoOutput(true);
//                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//
//                String postData = "player=" + URLEncoder.encode(playerName, "UTF-8") +
//                        "&questionId=" + questionId +
//                        "&playerAnswer=" + URLEncoder.encode(answer, "UTF-8");
//
//                OutputStream os = conn.getOutputStream();
//                os.write(postData.getBytes());
//                os.flush();
//                os.close();
//
//                // Read response
//                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                String response = reader.readLine();
//                reader.close();
//
//                conn.disconnect();
//
//                // Show response and move to next question
//                runOnUiThread(() -> {
//                    feedbackText.setVisibility(View.VISIBLE);
//                    nextWordButton.setVisibility(View.VISIBLE);
//                    feedbackText.setText(response);
//                    //Toast.makeText(MainActivity.this, response, Toast.LENGTH_SHORT).show();
//                });
//            } catch (Exception e) {
//                e.printStackTrace();
//                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
//            }
//        }).start();
//    }
    private void setCategory(String category) {
        selectedCategory = category;
        Log.d("MainActivity", "Category selected: " + category); // Add this line
        Toast.makeText(this, "Category selected: " + category, Toast.LENGTH_SHORT).show();
    }

}