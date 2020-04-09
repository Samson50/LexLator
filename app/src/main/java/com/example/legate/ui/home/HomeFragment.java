package com.example.legate.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import com.example.legate.R;
import com.example.legate.utils.CacheManager;


public class HomeFragment extends Fragment implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    private HomeViewModel homeViewModel;
    private View root;
    private Spinner stateSpinner;
    private Button goStateButton;
    private String state;
    private boolean updated = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        Context context = getActivity();

        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        root = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize State selection drop-down menu
        stateSpinner = root.findViewById(R.id.stateSpinner);
        assert context != null;
        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(context,
                R.array.states_array, android.R.layout.simple_spinner_item);
        stateSpinner.setAdapter(stateAdapter);
        stateSpinner.setOnItemSelectedListener(this);

        // Initialize goStateButton
        goStateButton = root.findViewById(R.id.goStateButton);
        goStateButton.setVisibility(View.GONE);
        goStateButton.setOnClickListener(this);

        // Update Local Cache
        if (!updated) {
            updated = true;
            CacheManager cacheManager = new CacheManager(context, root);
            int result = cacheManager.updateLocalCache();
            //root.findViewById(R.id.progress_overlay).setVisibility(View.GONE);
        }
        return root;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position != 0) {
            state = parent.getItemAtPosition(position).toString();
            goStateButton.setVisibility(View.VISIBLE);
        }
        else {
            state = null;
            goStateButton.setVisibility(View.GONE);
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
}
