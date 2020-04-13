package com.example.legate.data.finances;

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

public class ContributionListAdapter extends RecyclerView.Adapter<ContributionListAdapter.ContributionListViewHolder> {

    private static final String TAG = "ContributionListAdapter";

    private JSONArray contributions;
    private String[] keys;

    static class ContributionListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView name;
        TextView total;
        TextView fromPACs;
        TextView fromIndividuals;
        View content;

        ContributionListViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.contributor_name);
            total = itemView.findViewById(R.id.contribution_total);
            fromPACs = itemView.findViewById(R.id.contribution_pac);
            fromIndividuals = itemView.findViewById(R.id.contribution_individual);

            content = itemView.findViewById(R.id.layout_contribution_content);
            name.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (View.VISIBLE == content.getVisibility()) content.setVisibility(View.GONE);
            else content.setVisibility(View.VISIBLE);
        }
    }

    ContributionListAdapter(JSONArray contributionsArray, String[] keysArray) {
        contributions = contributionsArray;
        keys = keysArray;
    }

    @NonNull
    @Override
    public ContributionListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View contributionView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_contribution_list_item, parent, false);

        return new ContributionListViewHolder(contributionView);
    }

    @Override
    public void onBindViewHolder(@NonNull ContributionListViewHolder holder, int position) {
        try {
            JSONObject contribution = contributions.getJSONObject(position);

            holder.name.setText(contribution.getString(keys[0]));
            holder.total.setText(monetaryFormat(contribution.getString(keys[1])));
            holder.fromIndividuals.setText(monetaryFormat(contribution.getString(keys[2])));
            holder.fromPACs.setText(monetaryFormat(contribution.getString(keys[3])));
        } catch (JSONException e) {
            Log.e(TAG, "onBindViewHolder failed: " + e.toString());
        }
    }

    @Override
    public int getItemCount() {
        return contributions.length();
    }

    private String monetaryFormat(String amount) {
        Float floatAmount = Float.parseFloat(amount);
        return String.format(Locale.US, "$%,.2f", floatAmount);
    }
}
