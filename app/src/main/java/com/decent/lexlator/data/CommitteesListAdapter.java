package com.decent.lexlator.data;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.decent.lexlator.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CommitteesListAdapter extends RecyclerView.Adapter<CommitteesListAdapter.CommitteesListViewHolder> {

    private static final String TAG = "CommitteesListAdapter";

    private JSONArray committees;

    static class CommitteesListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // party, rank, title, start date
        ViewGroup membershipContentLayout;
        TextView committeeName;
        TextView membershipTitle;
        ViewGroup titleLayout;
        TextView membershipParty;
        TextView membershipRank;
        TextView membershipStart;

        CommitteesListViewHolder(@NonNull View itemView) {
            super(itemView);
            membershipContentLayout = itemView.findViewById(R.id.membership_content_layout);
            committeeName = itemView.findViewById(R.id.committee_name);
            membershipTitle = itemView.findViewById(R.id.membership_title);
            titleLayout = itemView.findViewById(R.id.title_layout);
            membershipParty = itemView.findViewById(R.id.membership_party);
            membershipRank = itemView.findViewById(R.id.membership_rank);
            membershipStart = itemView.findViewById(R.id.membership_start);

            committeeName.setOnClickListener(this);
            membershipContentLayout.setVisibility(View.GONE);
        }

        @Override
        public void onClick(View view) {
            if (View.VISIBLE == membershipContentLayout.getVisibility())
                membershipContentLayout.setVisibility(View.GONE);
            else
                membershipContentLayout.setVisibility(View.VISIBLE);
        }
    }

    CommitteesListAdapter(JSONArray committeesArray) {
        Log.d(TAG, "Populating committees array");
        committees = committeesArray;
    }

    @NonNull
    @Override
    public CommitteesListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View committeesView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_committees_list_item, parent, false);

        return new CommitteesListViewHolder(committeesView);
    }

    @Override
    public void onBindViewHolder(@NonNull CommitteesListViewHolder holder, int position) {

        try {
            JSONObject committeeEntry = committees.getJSONObject(position);
            JSONObject membership = committeeEntry.getJSONObject("membership");

            holder.committeeName.setText(committeeEntry.getString("name"));

            try {
                String title = membership.getString("title");
                holder.membershipTitle.setText(title);
                holder.titleLayout.setVisibility(View.VISIBLE);
            } catch (JSONException e) {
                holder.titleLayout.setVisibility(View.GONE);
            }

            holder.membershipRank.setText(membership.getString("rank"));
            holder.membershipParty.setText(membership.getString("party"));
            holder.membershipStart.setText(membership.getString("start_date"));
        } catch (JSONException e) {
            Log.e(TAG, "onBindViewHolder failed: " + e.toString());
        }

    }

    @Override
    public int getItemCount() {
        return committees.length();
    }
}
