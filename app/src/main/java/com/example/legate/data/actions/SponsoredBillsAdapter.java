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

public class SponsoredBillsAdapter extends RecyclerView.Adapter<SponsoredBillsAdapter.SponsoredBillsViewHolder> {

    private static final String TAG = "sponsoredBillsAdapter";

    private JSONArray bills;

    static class SponsoredBillsViewHolder extends RecyclerView.ViewHolder {
        TextView billName;
        ViewGroup billContent;
        TextView actionDate;
        TextView titleText;
        TextView billTitle;
        TextView introducedDate;
        TextView lastAction;
        ViewGroup summaryLayout;
        TextView summaryText;
        TextView billSummary;

        SponsoredBillsViewHolder(@NonNull View itemView) {
            super(itemView);
            billName = itemView.findViewById(R.id.bill_name);
            billContent = itemView.findViewById(R.id.bill_content_layout);
            actionDate = itemView.findViewById(R.id.bill_action_date);
            titleText = itemView.findViewById(R.id.text_bill_title);
            billTitle = itemView.findViewById(R.id.bill_title);
            introducedDate = itemView.findViewById(R.id.bill_introduced_date);
            lastAction = itemView.findViewById(R.id.bill_last_action);
            summaryLayout = itemView.findViewById(R.id.bill_summary_layout);
            summaryText = itemView.findViewById(R.id.text_bill_summary);
            billSummary = itemView.findViewById(R.id.bill_summary);

            CollapseTitle titleCollapse = new CollapseTitle();
            titleText.setOnClickListener(titleCollapse);

            CollapseSummary collapseSummary = new CollapseSummary();
            summaryText.setOnClickListener(collapseSummary);

            CollapseContent collapseContent = new CollapseContent();
            billName.setOnClickListener(collapseContent);
        }

        private class CollapseSummary implements View.OnClickListener {
            @Override
            public void onClick(View v) {
                if (View.VISIBLE == billSummary.getVisibility()) billSummary.setVisibility(View.GONE);
                else billSummary.setVisibility(View.VISIBLE);
            }
        }

        private class CollapseTitle implements View.OnClickListener {
            @Override
            public void onClick(View v) {
                if (View.VISIBLE == billTitle.getVisibility()) billTitle.setVisibility(View.GONE);
                else billTitle.setVisibility(View.VISIBLE);
            }
        }

        private class CollapseContent implements View.OnClickListener {
            @Override
            public void onClick(View v) {
                if (View.VISIBLE == billContent.getVisibility()) billContent.setVisibility(View.GONE);
                else billContent.setVisibility(View.VISIBLE);
            }
        }
    }

    public SponsoredBillsAdapter(JSONArray billsArray) {
        Log.d(TAG, "Populating bills array");
        bills = billsArray;
    }

    @NonNull
    @Override
    public SponsoredBillsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View billsView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_bills_list_item, parent, false);

        return new SponsoredBillsViewHolder(billsView);
    }

    @Override
    public void onBindViewHolder(@NonNull SponsoredBillsViewHolder holder, int position) {
        try {
            JSONObject bill = bills.getJSONObject(position);
            holder.billName.setText(bill.getString("name"));
            holder.billTitle.setText(bill.getString("title"));
            holder.actionDate.setText(bill.getString("last-action-date"));
            holder.introducedDate.setText(bill.getString("introduced"));
            holder.lastAction.setText(bill.getString("last-action"));
            String summary = bill.getString("summary");
            if (summary.length() > 0) holder.billSummary.setText(summary);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public int getItemCount() {
        return bills.length();
    }
}
