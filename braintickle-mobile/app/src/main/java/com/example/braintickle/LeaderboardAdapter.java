package com.example.braintickle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {
    private List<LeaderboardEntry> leaderboardEntries = new ArrayList<>();

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.leaderboard_item, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        LeaderboardEntry entry = leaderboardEntries.get(position);
        holder.playerNameText.setText(entry.getPlayerName());
        holder.playerScoreText.setText(String.valueOf(entry.getScore()));
    }

    @Override
    public int getItemCount() {
        return leaderboardEntries.size();
    }

    public void updateLeaderboard(List<LeaderboardEntry> newEntries) {
        this.leaderboardEntries = newEntries;
        notifyDataSetChanged();
    }

    static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        TextView playerNameText, playerScoreText;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            playerNameText = itemView.findViewById(R.id.playerNameText);
            playerScoreText = itemView.findViewById(R.id.playerScoreText);
        }
    }
}