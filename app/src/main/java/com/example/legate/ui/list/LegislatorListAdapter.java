package com.example.legate.ui.list;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.legate.R;
import com.example.legate.data.Legislator;

import java.util.List;

public class LegislatorListAdapter extends RecyclerView.Adapter<LegislatorListAdapter.LegislatorsListViewHolder> {

    private static final String TAG = "LegislatorListAdapter";

    private List<Legislator> legislatorsList;

    static class LegislatorsListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView legislatorImage;
        TextView legislatorTitle;
        TextView legislatorState;
        TextView legislatorParty;
        TextView legislatorDistrict;
        ViewGroup districtLayout;
        String legislatorPath;

        LegislatorsListViewHolder(@NonNull View itemView) {
            super(itemView);
            legislatorImage = itemView.findViewById(R.id.legislator_image);
            legislatorTitle = itemView.findViewById(R.id.legislator_title);
            legislatorState = itemView.findViewById(R.id.legislator_state);
            legislatorParty = itemView.findViewById(R.id.legislator_party);
            legislatorDistrict = itemView.findViewById(R.id.legislator_district);
            districtLayout = itemView.findViewById(R.id.district_layout);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (null != legislatorPath) {
                Bundle bundle = new Bundle();
                // TODO: Pass legislator object, not just path
                bundle.putString("path", legislatorPath);
                Navigation.findNavController(v).navigate(R.id.nav_legislator_main, bundle);
            }
        }
    }

    LegislatorListAdapter(List<Legislator> legislators) {
        legislatorsList = legislators;
    }

    void cancel() {
        for (Legislator legislator : legislatorsList) {
            legislator.cancelImageTask();
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
        if (0 != legislatorsList.get(position).fillLegislatorInfo(
                holder.legislatorImage,
                holder.legislatorTitle,
                holder.legislatorParty,
                holder.legislatorState,
                holder.legislatorDistrict,
                holder.districtLayout
        )) Log.e(TAG, "onBindViewHolder failed with fillLegislatorMain");
        holder.legislatorPath = legislatorsList.get(position).getPath();
    }

    @Override
    public int getItemCount() {
        if (null == legislatorsList) return 0;
        return legislatorsList.size();
    }

    void setLegislatorsList(List<Legislator> newList) {
        legislatorsList = newList;
        notifyDataSetChanged();
    }
}
