package com.example.legate.ui.legislator;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.legate.R;
import com.example.legate.utils.CacheManager;
import com.example.legate.data.Legislator;
import com.example.legate.utils.StateHelper;

public class LegislatorMain extends Fragment {

    private final static String TAG = "LegislatorMain";

    private StateHelper stateHelper = new StateHelper();
    private CacheManager cacheManager = new CacheManager();
    private Legislator legislator;

    private ViewGroup legislatorLayout;
    private ImageView legislatorImage;
    private TextView legislatorTitle;
    private TextView legislatorState;
    private TextView legislatorParty;
    private TextView legislatorDistrict;

    public View onCreateView(@NonNull LayoutInflater inflater,
                        ViewGroup container, Bundle savedInstanceState) {

        Context context = getActivity();
        //configManager = new ConfigManager(context);

        //homeViewModel =
        //        ViewModelProviders.of(this).get(HomeViewModel.class);
        final View root = inflater.inflate(R.layout.fragment_legislator_main, container, false);

        legislatorLayout = root.findViewById(R.id.legislator_banner);
        legislatorImage = legislatorLayout.findViewById(R.id.legislator_image);
        legislatorTitle = legislatorLayout.findViewById(R.id.legislator_title);
        legislatorState = legislatorLayout.findViewById(R.id.legislator_state);
        legislatorParty = legislatorLayout.findViewById(R.id.legislator_party);
        legislatorDistrict = legislatorLayout.findViewById(R.id.legislator_district);
        ViewGroup districtLayout = legislatorLayout.findViewById(R.id.district_layout);

        Bundle arguments = getArguments();
        String legislatorPath = null;
        if (null != arguments) legislatorPath = arguments.getString("path");
        if (null != legislatorPath) {
            legislator = new Legislator(legislatorPath);
            legislator.fillLegislatorInfo(legislatorImage, legislatorTitle, legislatorParty,
                    legislatorState, legislatorDistrict, districtLayout);
        }

        // Initialize financial views
        addCollapse((TextView) root.findViewById(R.id.finances_text), root.findViewById(R.id.financial_information));
        root.findViewById(R.id.finances_download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadFinances();
            }
        });

        final ViewGroup summaryView = root.findViewById(R.id.summary_constraint);
        addCollapse((TextView) root.findViewById(R.id.summary_text), summaryView);

        final RecyclerView contributorsRecycler = root.findViewById(R.id.top_contributors_recycler);
        RecyclerView.LayoutManager contributorsManager = new LinearLayoutManager(context);
        contributorsRecycler.setLayoutManager(contributorsManager);
        addCollapse((TextView) root.findViewById(R.id.top_contributors_text), contributorsRecycler);


        final RecyclerView industriesRecycler = root.findViewById(R.id.top_industries_recycler);
        RecyclerView.LayoutManager industriesManager = new LinearLayoutManager(context);
        industriesRecycler.setLayoutManager(industriesManager);
        addCollapse((TextView) root.findViewById(R.id.top_industries_text), industriesRecycler);

        root.findViewById(R.id.finances_fill).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                legislator.fillFinances(summaryView, contributorsRecycler, industriesRecycler);
            }
        });

        // Initialize Information views
        addCollapse((TextView) root.findViewById(R.id.information_text), root.findViewById(R.id.information_constraint));

        final TextView bioView = root.findViewById(R.id.legislator_bio);
        addCollapse((TextView) root.findViewById(R.id.bio_text), bioView);

        final RecyclerView committeesRecycler = root.findViewById(R.id.committees_recycler);
        RecyclerView.LayoutManager committeesManager = new LinearLayoutManager(context);
        committeesRecycler.setLayoutManager(committeesManager);
        addCollapse((TextView) root.findViewById(R.id.committees_text), committeesRecycler);

        root.findViewById(R.id.information_download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadInformation(bioView, committeesRecycler);
            }
        });

        // Initialize Action views
        addCollapse((TextView) root.findViewById(R.id.recent_actions_text), root.findViewById(R.id.activities_constraint));

        final RecyclerView billsRecycler = root.findViewById(R.id.sponsored_bills_recycler);
        RecyclerView.LayoutManager billsManager = new LinearLayoutManager(context);
        billsRecycler.setLayoutManager(billsManager);
        addCollapse((TextView) root.findViewById(R.id.sponsored_bills_text), billsRecycler);

        final RecyclerView votesRecycler = root.findViewById(R.id.votes_recycler);
        RecyclerView.LayoutManager votesManager = new LinearLayoutManager(context);
        votesRecycler.setLayoutManager(votesManager);
        addCollapse((TextView) root.findViewById(R.id.votes_text), votesRecycler);

        root.findViewById(R.id.activities_download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fillActions(billsRecycler, votesRecycler);
            }
        });

        // Initialize Contact Information views
        addCollapse((TextView) root.findViewById(R.id.contact_text), root.findViewById(R.id.contact_constraint));

        TextView addressView = root.findViewById(R.id.legislator_address);
        TextView phoneView = root.findViewById(R.id.legislator_phone);
        TextView websiteView = root.findViewById(R.id.legislator_website);
        legislator.fillContactInformation(addressView, phoneView, websiteView);

        return root;
    }

    private void addCollapse(TextView clicker, View hider) {
        hider.setVisibility(View.GONE);
        CollapseListener collapseListener = new CollapseListener(hider);
        clicker.setOnClickListener(collapseListener);
    }

    private static class CollapseListener implements View.OnClickListener {

        View collapsibleView;

        CollapseListener(View view) {
            collapsibleView = view;
        }

        @Override
        public void onClick(View v) {
            if (View.VISIBLE == collapsibleView.getVisibility()) collapsibleView.setVisibility(View.GONE);
            else collapsibleView.setVisibility(View.VISIBLE);
        }
    }

    private void downloadFinances() {
        legislator.downloadFinances();
    }

    private void fillActions(RecyclerView billsRecycler, RecyclerView votesRecycler) {
        legislator.fillActions(billsRecycler, votesRecycler);
    }

    private void downloadInformation(TextView bioView, RecyclerView committeesRecycler) {
        // Get bio
        legislator.fillBiography(bioView);
        // Get committees
        legislator.fillCommittees(committeesRecycler);
    }
}
