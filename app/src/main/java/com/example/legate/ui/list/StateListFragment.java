package com.example.legate.ui.list;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.legate.R;
import com.example.legate.utils.ConfigManager;
import com.example.legate.utils.StateHelper;

import java.io.File;
import java.io.FileFilter;




public class StateListFragment extends Fragment {

    private final static String TAG = "StateListFragment";

    private StateHelper stateHelper = new StateHelper();

    private View root;
    private TextView senatorsText;
    private TextView representativesText;
    private RecyclerView senatorsRecyclerView;
    private RecyclerView representativesRecyclerView;
    private RecyclerView.Adapter senatorsAdapter;
    private RecyclerView.Adapter representativesAdapter;
    private RecyclerView.LayoutManager senatorsManager;
    private RecyclerView.LayoutManager representativesManager;

    private FileFilter repFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().startsWith("R-");
        }
    };
    private FileFilter senFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            Log.d(TAG, pathname.getName());
            return pathname.getName().startsWith("S-");
        }
    };

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

        // Set-up collapsible property from text views
        senatorsText = root.findViewById(R.id.text_senators);
        senatorsText.setOnClickListener(new CollapseListener(senatorsRecyclerView));
        representativesText = root.findViewById(R.id.text_representatives);
        representativesText.setOnClickListener(new CollapseListener(representativesRecyclerView));

        return root;
    }

    private static class CollapseListener implements View.OnClickListener {

        View recyclerView;

        CollapseListener(View view) {
            recyclerView = view;
        }

        @Override
        public void onClick(View v) {
            if (View.VISIBLE == recyclerView.getVisibility()) recyclerView.setVisibility(View.GONE);
            else recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void populateFileArrays(Context context) {
        ConfigManager configManager = new ConfigManager(context);

        // Get the selected state
        String state;
        String configValue = configManager.getValue("state");
        if (null != configValue) {
            state = configValue;
            state = stateHelper.fullToShort(state);
            if (null == state) {
                Log.e(TAG, "Failed to convert state fullToShort");
                return;
            }
        }
        else {
            Log.e(TAG, "Failed to get state String from config file");
            return;
        }

        // Populate array from files in state directory
        File stateCacheFile = new File(context.getCacheDir(), state);
        senatorFilesArray = stateCacheFile.listFiles(senFilter);
        representativeFilesArray = stateCacheFile.listFiles(repFilter);
        if (null == senatorFilesArray || null == representativeFilesArray) {
            Log.e(TAG, "Failed to get contents of " + stateCacheFile.getAbsolutePath());
        }
    }
}
