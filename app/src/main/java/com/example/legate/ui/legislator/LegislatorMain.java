package com.example.legate.ui.legislator;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.legate.R;
import com.example.legate.data.Legislator;
import com.example.legate.utils.AlertFragment;
import com.example.legate.utils.CollapseListener;

public class LegislatorMain extends Fragment {

    private final static String TAG = "LegislatorMain";
    // twitter - http://twitter.com/[username]
    private final static String TWITTER_URL = "http://twitter.com/";
    // facebook - https://www.facebook.com/[username]
    private final static String FACEBOOK_URL = "https://www.facebook.com/";
    // youtube - https://www.youtube.com/user/[username]
    private final static String YOUTUBE_URL = "https://www.youtube.com/user/";
    // instagram - https://www.instagram.com/[username]
    private final static String INSTAGRAM_URL = "https://www.instagram.com/";

    private Legislator legislator;

    public View onCreateView(@NonNull LayoutInflater inflater,
                        ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView(...) called");

        Context context = getActivity();
        //configManager = new ConfigManager(context);

        //homeViewModel =
        //        ViewModelProviders.of(this).get(HomeViewModel.class);
        final View root = inflater.inflate(R.layout.fragment_legislator_main, container, false);

        ViewGroup legislatorLayout = root.findViewById(R.id.legislator_banner);
        ImageView legislatorImage = legislatorLayout.findViewById(R.id.legislator_image);
        TextView legislatorTitle = legislatorLayout.findViewById(R.id.legislator_title);
        TextView legislatorState = legislatorLayout.findViewById(R.id.legislator_state);
        TextView legislatorParty = legislatorLayout.findViewById(R.id.legislator_party);
        TextView legislatorDistrict = legislatorLayout.findViewById(R.id.legislator_district);
        ViewGroup districtLayout = legislatorLayout.findViewById(R.id.district_layout);

        Bundle arguments = getArguments();
        String legislatorPath = null;
        if (null != arguments) legislatorPath = arguments.getString("path");
        if (null != legislatorPath) {
            int updateInterval = 1;
            if (null != context) {
                String updateString = PreferenceManager.getDefaultSharedPreferences(context)
                        .getString("update_interval_preference", "1");
                updateInterval = Integer.parseInt(updateString);
            }
            legislator = new Legislator(legislatorPath, updateInterval);
            legislator.fillLegislatorInfo(legislatorImage, legislatorTitle, legislatorParty,
                    legislatorState, legislatorDistrict, districtLayout);
        }

        // Initialize financial views
        addCollapse((TextView) root.findViewById(R.id.finances_text), root.findViewById(R.id.financial_information));

        ImageView infoIcon = root.findViewById(R.id.finances_disclaimer);
        infoIcon.setOnClickListener(new AlertListener());

        View linkView = root.findViewById(R.id.open_secrets_text);
        linkView.setOnClickListener(new LinkListener(context, "https://www.opensecrets.org/"));
        View openIcon = root.findViewById(R.id.open_secrets_icon);
        openIcon.setOnClickListener(new LinkListener(context, "https://www.opensecrets.org/"));

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

        legislator.fillFinances(summaryView, contributorsRecycler, industriesRecycler);

        // Initialize Information views
        addCollapse((TextView) root.findViewById(R.id.information_text), root.findViewById(R.id.information_constraint));

        final TextView bioView = root.findViewById(R.id.legislator_bio);
        addCollapse((TextView) root.findViewById(R.id.bio_text), bioView);

        final RecyclerView committeesRecycler = root.findViewById(R.id.committees_recycler);
        RecyclerView.LayoutManager committeesManager = new LinearLayoutManager(context);
        committeesRecycler.setLayoutManager(committeesManager);
        addCollapse((TextView) root.findViewById(R.id.committees_text), committeesRecycler);

        legislator.fillBiography(bioView);
        legislator.fillCommittees(committeesRecycler);

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

        legislator.fillActions(billsRecycler, votesRecycler);

        // Initialize Contact Information views
        addCollapse((TextView) root.findViewById(R.id.contact_text), root.findViewById(R.id.contact_constraint));

        TextView addressView = root.findViewById(R.id.legislator_address);
        TextView phoneView = root.findViewById(R.id.legislator_phone);
        TextView websiteView = root.findViewById(R.id.legislator_website);
        ViewGroup socialViewGroup = root.findViewById(R.id.social_media_layout);
        legislator.fillContactInformation(addressView, phoneView, websiteView, socialViewGroup);

        ImageView twitterIcon = root.findViewById(R.id.icon_twitter);
        String twitterLink = TWITTER_URL + legislator.getUserName("twitter");
        LinkListener twitterListener = new LinkListener(context, twitterLink);
        twitterIcon.setOnClickListener(twitterListener);

        ImageView facebookIcon = root.findViewById(R.id.icon_facebook);
        String facebookLink = FACEBOOK_URL + legislator.getUserName("facebook");
        LinkListener facebookListener = new LinkListener(context, facebookLink);
        facebookIcon.setOnClickListener(facebookListener);

        ImageView instagramIcon = root.findViewById(R.id.icon_instagram);
        String instagramLink = INSTAGRAM_URL + legislator.getUserName("instagram");
        LinkListener instagramListener = new LinkListener(context, instagramLink);
        instagramIcon.setOnClickListener(instagramListener);

        ImageView youtubeIcon = root.findViewById(R.id.icon_youtube);
        String youtubeLink = YOUTUBE_URL + legislator.getUserName("youtube");
        LinkListener youtubeListener = new LinkListener(context, youtubeLink);
        youtubeIcon.setOnClickListener(youtubeListener);

        return root;
    }

    private class AlertListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            DialogFragment alert = new AlertFragment(
                    "The organizations themselves did not donate, rather the money " +
                            "came from the organizations' PACs, their individual members or " +
                            "employees or owners, and those individuals' immediate families. " +
                            "Organization totals include subsidiaries and affiliates. "
            );
            assert getFragmentManager() != null;
            alert.show(getFragmentManager(), "address-failure");
        }
    }

    private static class LinkListener implements View.OnClickListener {

        private String socialUrl;
        private Context context;

        LinkListener(Context c, String url) {
            context = c;
            socialUrl = url;
        }

        @Override
        public void onClick(View v) {
            Log.d(TAG, "Navigating to: " + socialUrl);
            Uri uri = Uri.parse(socialUrl);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(intent);
        }
    }

    private void addCollapse(TextView clicker, View hider) {
        hider.setVisibility(View.GONE);
        CollapseListener collapseListener = new CollapseListener(hider);
        clicker.setOnClickListener(collapseListener);
    }


}
