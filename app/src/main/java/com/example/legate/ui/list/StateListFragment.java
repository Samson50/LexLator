package com.example.legate.ui.list;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.legate.R;
import com.example.legate.utils.ConfigManager;

import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;




public class StateListFragment extends Fragment {

    private final static String TAG = "StateListFragment";

    private final String[] FULL_STATES = {
            "Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut",
            "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa",
            "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts","Michigan",
            "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada",
            "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina",
            "North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island",
            "South Carolina", "South Dakota", "Tennessee", "Texas", "Utah", "Vermont", "Virginia",
            "Washington", "West Virginia", "Wisconsin", "Wyoming"
    };
    private final String[] SHORT_STATES = {
            "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "HI", "ID", "IL", "IN",
            "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV",
            "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN",
            "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"
    };

    private View root;
    private RecyclerView senatorsRecyclerView;
    private RecyclerView representativesRecyclerView;
    private RecyclerView.Adapter senatorsAdapter;
    private RecyclerView.Adapter representativesAdapter;
    private RecyclerView.LayoutManager senatorsManager;
    private RecyclerView.LayoutManager representativesManager;

    private File[] senatorFilesArray;
    private File[] representativeFilesArray;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_state_list, container, false);

        Context context = getActivity();
        if (null == context) {
            Log.e(TAG, "Context was null");
            return root;
        }
        senatorsRecyclerView = root.findViewById(R.id.senatorsRecyclerView);
        representativesRecyclerView = root.findViewById(R.id.representativesRecyclerView);

        senatorsManager = new LinearLayoutManager(context);
        senatorsRecyclerView.setLayoutManager(senatorsManager);
        representativesManager = new LinearLayoutManager(context);
        representativesRecyclerView.setLayoutManager(representativesManager);

        populateFileArrays(context);

        // Iterate over array, populating recycler view
        senatorsAdapter = new LegislatorListAdapter(senatorFilesArray);
        senatorsRecyclerView.setAdapter(senatorsAdapter);
        representativesAdapter = new LegislatorListAdapter(representativeFilesArray);
        representativesRecyclerView.setAdapter(representativesAdapter);

        return root;
    }

    private void populateFileArrays(Context context) {
        ConfigManager configManager = new ConfigManager(context);

        // Get the selected state
        String state = null;
        String configValue = configManager.getValue("state");
        if (null != configValue) {
            state = configValue;
            // TODO: Make this better/implement state map or something
            for (int i = 0; i < 50; i++) {
                if (FULL_STATES[i].equals(state)) {
                    state = SHORT_STATES[i];
                    break;
                }
            }
        }
        else {
            Log.e(TAG, "Failed to get state String from config file");
            return;
        }

        // Populate array from files in state directory
        File stateCacheFile = new File(context.getCacheDir(), state);
        FileFilter repFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().startsWith("R-");
            }
        };
        FileFilter senFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                Log.d(TAG, pathname.getName());
                return pathname.getName().startsWith("S-");
            }
        };
        senatorFilesArray = stateCacheFile.listFiles(senFilter);
        representativeFilesArray = stateCacheFile.listFiles(repFilter);
        if (null == senatorFilesArray || null == representativeFilesArray) {
            Log.e(TAG, "Failed to get contents of " + stateCacheFile.getAbsolutePath());
            return;
        }
    }
}
