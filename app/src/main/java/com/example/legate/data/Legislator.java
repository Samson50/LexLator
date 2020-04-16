package com.example.legate.data;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.legate.R;
import com.example.legate.data.actions.SponsoredBillsAdapter;
import com.example.legate.data.actions.VotesListAdapter;
import com.example.legate.data.finances.Finances;
import com.example.legate.utils.CacheManager;
import com.example.legate.utils.ImageTask;
import com.example.legate.utils.StateHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;


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
    private JSONArray billsJSON = new JSONArray();
    private JSONObject socialMediaJSON = null;

    private String title;
    private String state;
    private String party;
    private String district;
    private String bioGuide;
    private String imageUrl;
    private String biography;


    public Legislator(String legislatorPath) {
        legislatorFile = new File(legislatorPath);
        if (0 != getLegislatorInfo()) Log.e(TAG, "Failed initial population");
        finances = new Finances(cacheManager, legislatorFile);
    }

    private class DownloadBio extends AsyncTask<String, Integer, String> {

        private TextView bioView;

        DownloadBio(TextView biographyView) {
            bioView = biographyView;
        }

        @Override
        protected String doInBackground(String... strings) {
            // download bio from bioguide: https://bioguideretro.congress.gov/Home/MemberDetails?memIndex=L000585
            String bioGuide = strings[0];
            String bioChar = bioGuide.substring(0, 1);
            String bioUrl = String.format("https://bioguideretro.congress.gov/Static_Files/data/%s/%s.xml", bioChar, bioGuide);
            // <uscongress-bio><biography>That sweet sweet data</biography></uscongress-bio>
            URL url = null;
            try {
                url = new URL(bioUrl);
            } catch (MalformedURLException e) {
                Log.e(TAG, e.toString());
                return null;
            }
            InputStream stream;
            try {
                stream = url.openConnection().getInputStream();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                return null;
            }

            // Get content from "<biography>" tag
            String bioString;
            try {
                bioString = parseXml(stream, "uscongress-bio", "biography");
                Log.d(TAG, "Biography: " + bioString);
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                return null;
            }

            // Write content to local file (bio.txt)
            String legislatorPath = strings[1];
            String bioPath = legislatorPath + "/bio.txt";
            cacheManager.writeFile(bioPath, bioString);

            return bioString;
        }

        @Override
        protected void onPostExecute(String bio) {
            super.onPostExecute(bio);
            biography = bio;
            bioView.setText(bio);
        }
    }

    private class FinanceTask extends AsyncTask<String, Void, Void> {

        private ViewGroup summaryView;
        private RecyclerView contributionsRecycler;
        private RecyclerView industriesRecycler;
        Finances financeHelper;

        FinanceTask(ViewGroup summary, RecyclerView contributes, RecyclerView industries, Finances helper) {
            summaryView = summary;
            contributionsRecycler = contributes;
            industriesRecycler = industries;
            financeHelper = helper;
        }

        @Override
        protected Void doInBackground(String... strings) {
            String openSecretsId = strings[0];

            financeHelper.downloadFinances(openSecretsId, CYCLE);
            return null;
        }

        @Override
        protected void onPostExecute(Void avoid) {
            financeHelper.fillFinances(summaryView, contributionsRecycler, industriesRecycler);
        }
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

    public void fillFinances(ViewGroup summaryView, RecyclerView contributionsRecycler, RecyclerView industriesRecycler) {
        FinanceTask task = new FinanceTask(summaryView, contributionsRecycler, industriesRecycler, finances);
        task.execute(getIdValue("opensecrets"));
    }

    private String parseXml(InputStream input, String firstTag, String textTag) throws IOException {
        Log.d(TAG, "Parsing xml tree from input stream...");
        try {
            // Setup the xml parser...
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(input, null);
            parser.nextTag();

            // Require first tag to be html? This could be an issue
            parser.require(XmlPullParser.START_TAG, null, firstTag);// outer tag
            // Iterate over the tags...
            while (parser.next() != XmlPullParser.END_TAG) {
                // If the parser encounters a start tag, ignore it
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                // Starts by looking for the entry tag
                if (parser.getName().equals("biography")) { // the tag we want
                    parser.require(XmlPullParser.START_TAG, null, textTag);
                    if (parser.next() == XmlPullParser.TEXT) {
                        return parser.getText();
                    }
                }
                // If the parser encounters a tag we don't want, skip it and all children
                else {
                    // skip tags that don't match...
                    skip(parser);
                }
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, e.toString());
        }
        finally {
            Log.d(TAG, "Reached finally, closing input stream");
            input.close();
        }
        Log.e(TAG, "Failed to get value from xml tree");
        return null;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    public void fillBiography(TextView bioView) {
        // Check if field already populated
        if (null != biography) {
            Log.d(TAG, "Populating biography from class variable");
            bioView.setText(biography);
            return;
        }
        // bio.txt exists?
        File bioFile = new File(legislatorFile, "bio.txt");
        if (bioFile.exists()) {
            Log.d(TAG, "Populating biography from cache file");
            String bioString = cacheManager.readFile(bioFile.getAbsolutePath());
            if (!bioString.isEmpty()) {
                biography = bioString;
                bioView.setText(biography);
                return;
            }
        }

        Log.d(TAG, "Biography not found, getting from url");
        DownloadBio bioTask = new DownloadBio(bioView);
        bioTask.execute(bioGuide, legislatorFile.getAbsolutePath());
    }

    private void getCommittees(File committeesFile) {
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

    public void fillCommittees(RecyclerView view) {

        Log.d(TAG, "Getting committees for " + title);

        if (0 == membershipJSON.length()) {
            Log.d(TAG, "membershipJSON empty, populating");
            File committeesFile = new File(legislatorFile, "committees.json");

            if (committeesFile.exists()) {
                String membershipString = cacheManager.readFile(committeesFile.getAbsolutePath());
                if (membershipString.isEmpty()) getCommittees(committeesFile);
                else membershipJSON = cacheManager.stringToJSONArray(membershipString);
            }
            else getCommittees(committeesFile);
        }

        CommitteesListAdapter adapter = new CommitteesListAdapter(membershipJSON);
        view.setAdapter(adapter);
    }

    public void fillActions(RecyclerView billsRecycler, RecyclerView votesRecycler) {
        fillVotes(votesRecycler);
        fillSponsoredBills(billsRecycler);
    }

    private void getVotes(File votesFile) {
        Log.d(TAG, "Getting votes for " + title);

        String chamber = title.substring(0, 1);
        if (chamber.equals("S")) chamber = "senate";
        else chamber = "house";

        votesJSON = cacheManager.getVotes(chamber, bioGuide);
        if (0 != cacheManager.writeFile(votesFile.getAbsolutePath(), votesJSON)) {
            Log.e(TAG, "Failed to write votes to file");
        }
    }

    private void fillVotes(RecyclerView votesView) {
        Log.d(TAG, "Getting votes...");
        if (0 == votesJSON.length()) {
            Log.d(TAG, "votesJSON empty, populating");
            File votesFile = new File(legislatorFile, "votes.json");

            if (!votesFile.exists()) getVotes(votesFile);
            else {
                String votesString = cacheManager.readFile(votesFile.getAbsolutePath());
                if (!votesString.isEmpty()) votesJSON = cacheManager.stringToJSONArray(votesString);
                else getVotes(votesFile);
            }
        }

        VotesListAdapter adapter = new VotesListAdapter(votesJSON);
        votesView.setAdapter(adapter);
    }

    private void getSponsoredBills(File billsFile) {
        Log.d(TAG, "Getting bills for " + title);

        String chamber = title.substring(0, 1);
        if (chamber.equals("S")) chamber = "senate";
        else chamber = "house";

        billsJSON = cacheManager.getSponsoredBills(chamber, bioGuide); //parseCommitteeMembership(currentMembership, votes, null);
        if (0 != cacheManager.writeFile(billsFile.getAbsolutePath(), billsJSON)) {
            Log.e(TAG, "Failed to write votes to file");
        }
    }

    private void fillSponsoredBills(RecyclerView billsRecycler) {
        Log.d(TAG, "Getting sponsored bills...");
        if (0 == billsJSON.length()) {
            Log.d(TAG, "billsJSON empty, populating");
            File billsFile = new File(legislatorFile, "bills.json");

            if (!billsFile.exists()) getSponsoredBills(billsFile);
            else {
                String billsString = cacheManager.readFile(billsFile.getAbsolutePath());
                if (!billsString.isEmpty()) billsJSON = cacheManager.stringToJSONArray(billsString);
                else getSponsoredBills(billsFile);
            }
        }

        SponsoredBillsAdapter adapter = new SponsoredBillsAdapter(billsJSON);
        billsRecycler.setAdapter(adapter);
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

    public String getUserName(String platform) {
        try {
            return socialMediaJSON.getString(platform);
        } catch (JSONException e) {
            Log.e(TAG, "Legislator " + bioGuide + " " + platform + " not found");
            return null;
        }
    }

    private void getSocialMedia(File socialFile) {
        JSONObject newSocial = cacheManager.getSocialMedia(getBioGuide());
        if (null != newSocial) {
            socialMediaJSON = newSocial;
            if (0 != cacheManager.writeFile(socialFile.getAbsolutePath(), socialMediaJSON))
                Log.e(TAG, "Failed to write social media to file");
        }

    }

    private void fillSocialMedia(ViewGroup mediaLayout) {
        // twitter - http://twitter.com/[username]
        // facebook - https://www.facebook.com/[username]
        // youtube - https://www.youtube.com/user/[username]
        // instagram - https://www.instagram.com/[username]
        Log.d(TAG, "Getting social media...");
        if (null == socialMediaJSON) {
            Log.d(TAG, "socialMediaJSON == null, populating");
            File socialFile = new File(legislatorFile, "social-media.json");
            if (!socialFile.exists()) getSocialMedia(socialFile);
            else {
                String socialString = cacheManager.readFile(socialFile.getAbsolutePath());
                if (!socialString.isEmpty()) socialMediaJSON = cacheManager.stringToJSON(socialString);
                else getSocialMedia(socialFile);
            }
        }

        // Connect data with views...
        if (socialMediaJSON.has("twitter")) {
            mediaLayout.findViewById(R.id.icon_twitter).setVisibility(View.VISIBLE);
        }
        if (socialMediaJSON.has("facebook")) {
            mediaLayout.findViewById(R.id.icon_facebook).setVisibility(View.VISIBLE);
        }
        if (socialMediaJSON.has("instagram")) {
            mediaLayout.findViewById(R.id.icon_instagram).setVisibility(View.VISIBLE);
        }
        if (socialMediaJSON.has("youtube")) {
            mediaLayout.findViewById(R.id.icon_youtube).setVisibility(View.VISIBLE);
        }
    }

    public void fillContactInformation(TextView addressView, TextView phoneNumberView,
                                       TextView websiteView, ViewGroup socialViewGroup) {
        String address = getTermValue("address");
        if (null == address) address = getTermValue("office");
        addressView.setText(address);

        phoneNumberView.setText(getTermValue("phone"));

        websiteView.setText(getTermValue("url"));

        fillSocialMedia(socialViewGroup);
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
