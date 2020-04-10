package com.example.legate.ui.list;

import android.content.Context;
import android.media.Image;
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
import com.example.legate.utils.CacheManager;
import com.example.legate.utils.ImageTask;
import com.example.legate.utils.StateHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LegislatorListAdapter extends RecyclerView.Adapter<LegislatorListAdapter.LegislatorsListViewHolder> {

    private static final int TITLE = 0;
    private static final int STATE = 1;
    private static final int PARTY = 2;
    private static final int DISTRICT = 3;
    private static final int IMAGE_PATH = 4;
    private static final int IMAGE_URL = 5;

    private static final String TAG = "LegislatorListAdapter";

    private StateHelper stateHelper = new StateHelper();
    private CacheManager cacheManager = new CacheManager();

    private List<String[]> legislatorsList;
    private Context context;

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
                bundle.putString("path", legislatorPath);
                Navigation.findNavController(v).navigate(R.id.nav_legislator_main, bundle);
            }
        }
    }

    LegislatorListAdapter(File[] legislatorsFileArray) {
        Log.d(TAG, "Populating adapter data");
        legislatorsList = new ArrayList<>();

        for (File legislatorFile: legislatorsFileArray) {
            // ex: S-R-First Last, R-12-D-First Last
            String[] fileSplit = legislatorFile.getName().split("-");
            String[] newLegislator = {"", "", "", "", "", ""};

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

            String bioGuide = getBioGuide(legislatorFile);
            if (null == bioGuide) {
                Log.e(TAG, "bioGuide was null");
                return;
            }
            newLegislator[IMAGE_PATH] = legislatorFile.getPath();
            // https://bioguideretro.congress.gov/Static_Files/images/photos/D/D000563.jpg
            newLegislator[IMAGE_URL] = String.format("https://bioguideretro.congress.gov/Static_Files/images/photos/%c/%s.jpg", bioGuide.charAt(0), bioGuide);

            //[0], newLegislator[1], newLegislator[2], newLegislator[3], newLegislator[4]
            Log.d(TAG, String.format("newLeg: %s, %s, %s, %s, %s, %s", (Object[]) newLegislator));
            legislatorsList.add(newLegislator.clone());
        }
    }

    private String getBioGuide(File legislatorDirectory) {
        String bioGuide;
        String infoString;

        infoString = cacheManager.readFile(new File(legislatorDirectory, "info.json").getPath());
        if (null == infoString) return null;

        JSONObject legislatorJSON = cacheManager.stringToJSON(infoString);

        try {
            Log.d(TAG, "getBioGuide(): Getting bioguide from JSON format string");
            bioGuide = legislatorJSON.getJSONObject("id").getString("bioguide");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }

        Log.d(TAG, "getBioGuide(): result: " + bioGuide);
        return bioGuide;
    }

    @NonNull
    @Override
    public LegislatorsListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
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
        // TODO: This is dirty. Clean it up
        holder.legislatorPath = legislatorData[IMAGE_PATH];
        if (legislatorData[TITLE].contains("Rep")) {
            holder.legislatorDistrict.setText(legislatorData[DISTRICT]);
        }
        else {
            holder.districtLayout.setVisibility(View.GONE);
        }
        ImageTask imageTask = new ImageTask(holder.legislatorImage);
        imageTask.execute(legislatorData[IMAGE_PATH], legislatorData[IMAGE_URL]);
    }

    @Override
    public int getItemCount() {
        return legislatorsList.size();
    }
}
