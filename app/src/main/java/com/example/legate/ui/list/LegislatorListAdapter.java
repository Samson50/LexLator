package com.example.legate.ui.list;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.legate.R;
import com.example.legate.utils.StateHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LegislatorListAdapter extends RecyclerView.Adapter<LegislatorListAdapter.LegislatorsListViewHolder> {

    private static final int TITLE = 0;
    private static final int STATE = 1;
    private static final int PARTY = 2;
    private static final int DISTRICT = 3;

    private static final String TAG = "LegislatorListAdapter";

    private StateHelper stateHelper = new StateHelper();

    private List<String[]> legislatorsList;

    static class LegislatorsListViewHolder extends RecyclerView.ViewHolder {
        TextView legislatorTitle;
        TextView legislatorState;
        TextView legislatorParty;
        TextView legislatorDistrict;
        ViewGroup districtLayout;

        LegislatorsListViewHolder(@NonNull View itemView) {
            super(itemView);
            legislatorTitle = itemView.findViewById(R.id.legislator_title);
            legislatorState = itemView.findViewById(R.id.legislator_state);
            legislatorParty = itemView.findViewById(R.id.legislator_party);
            legislatorDistrict = itemView.findViewById(R.id.legislator_district);
            districtLayout = itemView.findViewById(R.id.district_layout);
        }
    }

    LegislatorListAdapter(File[] legislatorsFileArray) {
        Log.d(TAG, "Populating adapter data");
        legislatorsList = new ArrayList<>();

        for (File legislatorFile: legislatorsFileArray) {
            // ex: S-R-First Last, R-12-D-First Last
            String[] fileSplit = legislatorFile.getName().split("-");
            String[] newLegislator = {"", "", "", ""};

            String legislatorName = fileSplit[fileSplit.length - 2] + " " + fileSplit[fileSplit.length - 1];

            if (fileSplit[0].equals("R")) {
                newLegislator[TITLE] = "Rep. " + legislatorName;
                newLegislator[DISTRICT] = fileSplit[1];
                newLegislator[PARTY] = fileSplit[2];
            }
            else {
                newLegislator[TITLE] = "Sen. " + legislatorName;
                newLegislator[PARTY] = fileSplit[1];
            }

            File parentFile = legislatorFile.getParentFile();
            if (parentFile == null) return;
            String shortState = parentFile.getName();
            String fullState = stateHelper.shortToFull(shortState);

            newLegislator[STATE] = fullState;

            Log.d(TAG,
                    String.format("newLeg: %s, %s, %s, %s",
                            newLegislator[0], newLegislator[1], newLegislator[2], newLegislator[3]
                    )
            );
            legislatorsList.add(newLegislator.clone());
        }
    }

    @NonNull
    @Override
    public LegislatorsListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View legislatorView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_legislator_list_item, parent, false);

        return new LegislatorsListViewHolder(legislatorView);
    }

    @Override
    public void onBindViewHolder(@NonNull LegislatorsListViewHolder holder, int position) {
        String[] legislatorData = legislatorsList.get(position);
        holder.legislatorTitle.setText(legislatorData[TITLE]);
        holder.legislatorState.setText(legislatorData[STATE]);
        holder.legislatorParty.setText(legislatorData[PARTY]);
        if (legislatorData[TITLE].contains("Rep")) {
            holder.legislatorDistrict.setText(legislatorData[DISTRICT]);
        }
        else {
            holder.districtLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return legislatorsList.size();
    }


}
