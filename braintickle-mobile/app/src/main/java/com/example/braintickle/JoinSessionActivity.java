package com.example.braintickle;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class JoinSessionActivity extends AppCompatActivity {
    private EditText sessionIdInput, playerNameInput;
    private Button joinSessionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_session);

        sessionIdInput = findViewById(R.id.sessionIdInput);
        playerNameInput = findViewById(R.id.playerNameInput);
        joinSessionButton = findViewById(R.id.joinSessionButton);

        joinSessionButton.setOnClickListener(v -> {
            String sessionIdStr = sessionIdInput.getText().toString().trim();
            String playerName = playerNameInput.getText().toString().trim();

            if (!sessionIdStr.isEmpty()) {
                try {
                    String sessionId = sessionIdStr; // no parsing needed
                    Log.d("DEBUG", "Joining with sessionId: " + sessionId);

                    Intent intent = new Intent(JoinSessionActivity.this, MainActivity.class);
                    intent.putExtra("sessionId", sessionId);
                    intent.putExtra("playerName", playerName);
                    startActivity(intent);
                    finish();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid session ID format", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter a session ID", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
