package com.decent.lexlator.ui.list;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.decent.lexlator.R;
import com.decent.lexlator.data.Legislator;
import com.decent.lexlator.utils.CollapseListener;

import java.util.List;


public class StateListFragment extends Fragment {

    private final static String TAG = "StateListFragment";

    private String state;
    private String district;
    private String districtName;

    private LegislatorListAdapter senatorsAdapter;
    private LegislatorListAdapter representativesAdapter;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_state_list, container, false);

        Context context = getActivity();
        if (null == context) {
            Log.e(TAG, "Context was null");
            return root;
        }

        // Get bundle arguments
        Bundle arguments = getArguments();
        if (null != arguments) {
            state = arguments.getString("state");
            district = arguments.getString("district");
            districtName = arguments.getString("district-name");
        }
        TextView districtView = root.findViewById(R.id.state_district);
        if (districtName != null) {
            districtView.setText(districtName);
        }
        else {
            districtView.setVisibility(View.GONE);
        }

        StateListViewModel model = ViewModelProviders.of(this).get(StateListViewModel.class);
        model.setFilePath(context.getCacheDir());
        model.setState(state);
        model.setDistrict(district);

        // Initialize legislator recycler views
        RecyclerView senatorsRecyclerView = root.findViewById(R.id.senatorsRecyclerView);
        RecyclerView.LayoutManager senatorsManager = new LinearLayoutManager(context);
        senatorsRecyclerView.setLayoutManager(senatorsManager);

        RecyclerView representativesRecyclerView = root.findViewById(R.id.representativesRecyclerView);
        RecyclerView.LayoutManager representativesManager = new LinearLayoutManager(context);
        representativesRecyclerView.setLayoutManager(representativesManager);


        // Iterate over array, populating recycler view
        LiveData<List<Legislator>> senators = model.getSenators();

        senatorsAdapter = new LegislatorListAdapter(senators.getValue());
        senatorsRecyclerView.setAdapter(senatorsAdapter);

        senators.observe(this, new Observer<List<Legislator>>() {
            @Override
            public void onChanged(List<Legislator> legislators) {
                senatorsAdapter.setLegislatorsList(legislators);
            }
        });

        LiveData<List<Legislator>> representatives = model.getRepresentatives();

        representativesAdapter = new LegislatorListAdapter(representatives.getValue());
        representativesRecyclerView.setAdapter(representativesAdapter);

        representatives.observe(this, new Observer<List<Legislator>>() {
            @Override
            public void onChanged(List<Legislator> legislators) {
                representativesAdapter.setLegislatorsList(legislators);
            }
        });

        // Set-up collapsible property from text views
        TextView senatorsText = root.findViewById(R.id.text_senators);
        senatorsText.setOnClickListener(new CollapseListener(senatorsRecyclerView));
        TextView representativesText = root.findViewById(R.id.text_representatives);
        representativesText.setOnClickListener(new CollapseListener(representativesRecyclerView));

        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        representativesAdapter.cancel();
        senatorsAdapter.cancel();
    }
}
