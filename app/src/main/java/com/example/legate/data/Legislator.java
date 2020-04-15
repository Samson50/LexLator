package com.example.legate.data;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.legate.data.actions.VotesListAdapter;
import com.example.legate.data.finances.Finances;
import com.example.legate.utils.CacheManager;
import com.example.legate.utils.ImageTask;
import com.example.legate.utils.StateHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;


public class Legislator {
    private static final String TAG = "Legislator";
    private static final String CYCLE = "2020";

    private CacheManager cacheManager = new CacheManager();
    private StateHelper stateHelper = new StateHelper();
    private Finances finances;
    private ImageTask imageTask;

    private File legislatorFile;
    private JSONObject infoJSON = null;
    private JSONArray membershipJSON = new JSONArray();
    private JSONArray votesJSON = new JSONArray();

    private String title;
    private String state;
    private String party;
    private String district;
    private String bioGuide;
    private String imageUrl;


    public Legislator(String legislatorPath) {
        legislatorFile = new File(legislatorPath);
        if (0 != getLegislatorInfo()) Log.e(TAG, "Failed initial population");
        finances = new Finances(cacheManager, legislatorFile);
    }

    public void cancelImageTask() {
        if (null != imageTask) imageTask.cancel(false);
    }

    public int fillLegislatorInfo(ImageView imageView, TextView titleView, TextView partyView,
                                 TextView stateView, TextView districtView, ViewGroup districtLayout) {
        if (null == title || null == state || null == party || null == imageUrl) {
            if (0 != getLegislatorInfo()) {
                Log.e(TAG, "fillLegislatorMain(...) failed");
                return 1;
            }
        }

        titleView.setText(title);
        partyView.setText(party);
        stateView.setText(state);
        if (title.contains("Rep")) {
            districtView.setText(district);
        }
        else {
            districtLayout.setVisibility(View.GONE);
        }
        imageTask = new ImageTask(imageView);
        imageTask.execute(legislatorFile.getAbsolutePath(), imageUrl);

        return 0;
    }

    /**
     * Called from LegislatorMain to fill all fields from cached files
     * @return int indicating success or error code
     */
    public int fillLegislatorMain() {
        return 0;
    }

    private int getLegislatorInfo() {
        if (null == infoJSON) {
            if (0 != getInfoJSON()) {
                Log.e(TAG, "Failed to get info.json");
                return 1;
            }
        }

        String[] fileSplit = legislatorFile.getName().split("-");

        String legislatorName = fileSplit[fileSplit.length - 2] + " " + fileSplit[fileSplit.length - 1];

        if (fileSplit[0].equals("R")) {
            title = "Rep. " + legislatorName;
            district = fileSplit[1];
            party = fileSplit[2];
        }
        else {
            title = "Sen. " + legislatorName;
            party = fileSplit[1];
        }

        File parentFile = legislatorFile.getParentFile();
        if (parentFile == null) return 1;
        String shortState = parentFile.getName();
        state = stateHelper.shortToFull(shortState);

        bioGuide = getBioGuide();
        if (null == bioGuide) {
            Log.e(TAG, "bioGuide was null");
            return 1;
        }
        imageUrl = String.format("https://bioguideretro.congress.gov/Static_Files/images/photos/%c/%s.jpg", bioGuide.charAt(0), bioGuide);
        Log.d(TAG, "imageURL: " + imageUrl);

        return 0;
    }

    public void downloadFinances() {
        String openSecretsId = getIdValue("opensecrets");

        finances.downloadFinances(openSecretsId, CYCLE);
    }

    public void fillFinances(ViewGroup summaryView, RecyclerView contributionsRecycler, RecyclerView industriesRecycler) {
        finances.fillFinances(summaryView, contributionsRecycler, industriesRecycler);
    }

    public void fillContactInformation(TextView addressView, TextView phoneNumberView, TextView websiteView) {
        String address = getTermValue("address");
        if (null == address) address = getTermValue("office");
        addressView.setText(address);

        phoneNumberView.setText(getTermValue("phone"));

        websiteView.setText(getTermValue("url"));
        // Social Media: https://theunitedstates.io/congress-legislators/legislators-social-media.json
    }

    public void fillBiography(TextView bioView) {

    }

    public void fillCommittees(RecyclerView view) {

        Log.d(TAG, "Getting committees for " + title);

        if (0 == membershipJSON.length()) {
            Log.d(TAG, "membershipJSON empty, populating");
            File committeesFile = new File(legislatorFile, "committees.json");

            if (!committeesFile.exists()) {
                Log.d(TAG, "Getting committees for " + title);

                JSONArray currentCommittees = cacheManager.getCurrentCommittees();
                JSONObject currentMembership = cacheManager.getCommitteeMembership();

                if (null != currentCommittees && null != currentMembership) {
                    parseCommitteeMembership(currentMembership, currentCommittees, null);

                    if (0 != cacheManager.writeFile(committeesFile.getAbsolutePath(), membershipJSON)) {
                        Log.e(TAG, "Failed to write committee membership to file");
                    }
                } else Log.e(TAG, "fillCommittees(...): (null == currentCommittees || null == currentMembership)");
            }
            else {
                String membershipString = cacheManager.readFile(committeesFile.getAbsolutePath());
                membershipJSON = cacheManager.stringToJSONArray(membershipString);
            }
        }

        CommitteesListAdapter adapter = new CommitteesListAdapter(membershipJSON);
        view.setAdapter(adapter);

    }

    public void fillActions(RecyclerView billsRecycler, RecyclerView votesRecycler) {
        fillVotes(votesRecycler);
        fillSponsoredBills(billsRecycler);
    }

    private void fillVotes(RecyclerView votesView) {
        Log.d(TAG, "Getting votes...");
        if (0 == votesJSON.length()) {
            Log.d(TAG, "votesJSON empty, populating");
            File votesFile = new File(legislatorFile, "votes.json");

            if (!votesFile.exists()) {
                Log.d(TAG, "Getting votes for " + title);

                JSONArray votes = cacheManager.getCurrentCommittees();

                if (null != votes) {
                    String chamber = title.substring(0, 1);
                    if (chamber.equals("S")) chamber = "senate";
                    else chamber = "house";

                    votesJSON = cacheManager.getVotes(chamber, bioGuide); //parseCommitteeMembership(currentMembership, votes, null);
                    if (0 != cacheManager.writeFile(votesFile.getAbsolutePath(), votesJSON)) {
                        Log.e(TAG, "Failed to write votes to file");
                    }
                } else Log.e(TAG, "fillVotes(): (null == votes)");
            }
            else {
                String votesString = cacheManager.readFile(votesFile.getAbsolutePath());
                votesJSON = cacheManager.stringToJSONArray(votesString);
            }
        }

        VotesListAdapter adapter = new VotesListAdapter(votesJSON);
        votesView.setAdapter(adapter);
    }

    private void fillSponsoredBills(RecyclerView billsRecycler) {
        Log.d(TAG, "Getting sponsored bills...");

    }

    private void parseCommitteeMembership(JSONObject membership, JSONArray committees, String prefix) {

        // For each committee in the array...
        for (int i = 0; i < committees.length(); i++) {

            // Check if being called on subcommittee...
            String committeeId = "";
            if (null != prefix) committeeId = prefix;

            try {
                JSONObject committee = committees.getJSONObject(i);
                // Get the thomas_id used to identify that committee...
                committeeId = committeeId + committee.getString("thomas_id");
                Log.d(TAG, "Getting membership for " + committeeId);

                // Get the members of that committee using the unique ID
                JSONArray committeeMembership = membership.getJSONArray(committeeId);

                // For each membership entry...
                for (int j = 0; j < committeeMembership.length(); j++) {

                    // Get the bioguide identifier and compare w/ supplied guide
                    JSONObject member = committeeMembership.getJSONObject(j);
                    if (member.getString("bioguide").equals(bioGuide)) {

                        // Add relevant information to membership JSONArray
                        JSONObject membershipInfo = new JSONObject();
                        membershipInfo.put("name", committee.getString("name"));
                        membershipInfo.put("membership", member);
                        membershipJSON.put(membershipInfo);
                    }
                }

                // Check if there are any subcommittees
                if (null == prefix) {
                    JSONArray subcommittees = committee.getJSONArray("subcommittees");
                    if (subcommittees.length() > 0) {
                        Log.d(TAG, "Getting subcommittees for " + committeeId);
                        parseCommitteeMembership(membership, subcommittees, committeeId);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "parseCommitteeMembership(...) failed: " + e.toString());
            }
        }

    }

    public String getPath() {
        if (null != legislatorFile) return legislatorFile.getAbsolutePath();
        else return null;
    }

    private int getInfoJSON() {
        String infoString = cacheManager.readFile(new File(legislatorFile, "info.json").getPath());
        if (null == infoString) return 1;

        infoJSON = cacheManager.stringToJSON(infoString);
        if (null == infoJSON) {
            Log.e(TAG, "getInfoJSON failed to convert string to JSON");
            return 1;
        }
        return 0;
    }

    private String getValue(String category, String key) {
        if (null == infoJSON) {
            if (0 != getInfoJSON()) {
                Log.e(TAG, "Failed to get info.json");
                return null;
            }
        }

        try {
            Log.d(TAG,
                    String.format(
                            "getValue(): Getting value (%s) from category (%s): ", key, category
                    )
            );
            return infoJSON.getJSONObject(category).getString(key);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }

    private String getTermValue(String key) {
        return getValue("term", key);
    }

    private String getIdValue(String key) {
        return getValue("id", key);
    }

    private String getBioGuide() {
        return getIdValue("bioguide");
    }
}
