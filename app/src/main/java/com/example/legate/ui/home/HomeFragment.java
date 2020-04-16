package com.example.legate.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import com.example.legate.R;
import com.example.legate.utils.CacheManager;
import com.example.legate.utils.ConfigManager;


public class HomeFragment extends Fragment implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    private static final String TAG = "HomeFragment";
    private static final String CIVIC_API = "https://content.googleapis.com/civicinfo/v2/representatives?";
    private static final String CIVIC_KEY = "AIzaSyBeG0teS-ls3puepwzK89RmuLI_YVRLURQ";

    private ConfigManager configManager;

    private HomeViewModel homeViewModel;
    private Button goStateButton;
    private String state;
    private EditText address;
    private EditText city;
    private EditText zipCode;


    private static boolean updated = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        Context context = getActivity();
        assert context != null;
        configManager = new ConfigManager(context.getFilesDir());

        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize State selection drop-down menu
        Spinner stateSpinner = root.findViewById(R.id.stateSpinner);
        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(context,
                R.array.states_array, android.R.layout.simple_spinner_item);
        stateSpinner.setAdapter(stateAdapter);
        stateSpinner.setOnItemSelectedListener(this);

        // Initialize goStateButton
        goStateButton = root.findViewById(R.id.goStateButton);
        goStateButton.setVisibility(View.GONE);
        goStateButton.setOnClickListener(this);

        // Initialize address views
        address = root.findViewById(R.id.edit_address);
        city = root.findViewById(R.id.edit_city);
        zipCode = root.findViewById(R.id.edit_state);

        // Update Local Cache
        if (!updated) {
            updated = true;
            ViewGroup progressOverlay = root.findViewById(R.id.progress_overlay);
            ViewGroup contentLayout = root.findViewById(R.id.home_content_layout);
            CacheManager cacheManager = new CacheManager();
            if (0 != cacheManager.updateLocalCache(context, progressOverlay, contentLayout)) Log.e(TAG, "Failed to update cache.");
        }
        return root;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position != 0) {
            state = parent.getItemAtPosition(position).toString();
            goStateButton.setVisibility(View.VISIBLE);

            if (0 != configManager.update("state", state)) {
                Log.e(TAG, "Config update failed");
            }
        }
        else {
            state = null;
            goStateButton.setVisibility(View.GONE);

            if (0 != configManager.update("state", state)) {
                Log.e(TAG, "Config update failed");
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        state = null;
        goStateButton.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        Navigation.findNavController(v).navigate(R.id.nav_state_list);
    }

    private class DistrictButton implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            // https://developers.google.com/civic-information/docs/v2
            // https://developers.google.com/civic-information/

            String queryString = CIVIC_API;
            String addressString = String.format(
                    "%s, %s, %s, %s",
                    address.getText().toString(),
                    city.getText().toString(),
                    state,
                    zipCode.getText().toString()
            );
            queryString += "address=" + addressString;
            queryString += "&includeOffices=false";
            queryString += "&levels=country";
            queryString += "key=" + CIVIC_KEY;

            Log.d(TAG, queryString);

            // Hide home fragment and display progress overlay

            // Call query task

        }
    }
}
