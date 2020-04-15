package com.example.legate.data.actions;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.legate.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class VotesListAdapter extends RecyclerView.Adapter<VotesListAdapter.VotesListViewHolder> {

    private static final String TAG = "VotesListAdapter";

    private JSONArray votes;

    static class VotesListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // voteNumber, date-time, position, details
        ViewGroup generalLayout;
        TextView numberView;
        TextView dateView;
        TextView positionView;
        TextView descriptionView;

        VotesListViewHolder(@NonNull View itemView) {
            super(itemView);
            generalLayout = itemView.findViewById(R.id.layout_vote_main);
            numberView = itemView.findViewById(R.id.vote_number);
            dateView = itemView.findViewById(R.id.vote_date);
            positionView = itemView.findViewById(R.id.vote_position);
            descriptionView = itemView.findViewById(R.id.vote_details);

            generalLayout.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (View.VISIBLE == descriptionView.getVisibility())
                descriptionView.setVisibility(View.GONE);
            else
                descriptionView.setVisibility(View.VISIBLE);
        }
    }

    public VotesListAdapter(JSONArray votesArray) {
        Log.d(TAG, "Populating votes array");
        votes = votesArray;
    }

    @NonNull
    @Override
    public VotesListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View votesView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_votes_list_item, parent, false);

        return new VotesListViewHolder(votesView);
    }

    @Override
    public void onBindViewHolder(@NonNull VotesListViewHolder holder, int position) {
        try {
            JSONObject voteEntry = votes.getJSONObject(position);

            holder.numberView.setText(
                    String.format(Locale.US, "%d", voteEntry.getInt("roll-call"))
            );
            holder.dateView.setText(voteEntry.getString("date"));
            holder.positionView.setText(voteEntry.getString("position"));
            holder.descriptionView.setText(voteEntry.getString("description"));
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public int getItemCount() {
        return votes.length();
    }


}
