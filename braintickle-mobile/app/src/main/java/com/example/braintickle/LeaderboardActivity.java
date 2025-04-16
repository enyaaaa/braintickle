package com.example.braintickle;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class LeaderboardActivity extends AppCompatActivity {
    private RecyclerView leaderboardRecyclerView;
    private LeaderboardAdapter leaderboardAdapter;
    private OkHttpClient client = new OkHttpClient();
    private final String LEADERBOARD_URL = "http://10.0.2.2:9999/braintickle/getLeaderboard?sessionId=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        String sessionId = getIntent().getStringExtra("sessionId");

        leaderboardRecyclerView = findViewById(R.id.leaderboardRecyclerView);
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        leaderboardAdapter = new LeaderboardAdapter();
        leaderboardRecyclerView.setAdapter(leaderboardAdapter);

        fetchLeaderboard(sessionId);
    }

    private void fetchLeaderboard(String sessionId) {
        Request request = new Request.Builder()
                .url(LEADERBOARD_URL + sessionId)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                Log.d("LEADERBOARD", "Server Response: " + responseText);
                List<LeaderboardEntry> entries = new ArrayList<>();

                if (!responseText.equals("No scores yet")) {
                    String[] playerScores = responseText.split("\\|");
                    for (String entry : playerScores) {
                        String[] parts = entry.split(":");
                        if (parts.length == 2) {
                            entries.add(new LeaderboardEntry(parts[0], Integer.parseInt(parts[1])));
                        }
                    }
                }

                runOnUiThread(() -> leaderboardAdapter.updateLeaderboard(entries));
            }
        });
    }
}
