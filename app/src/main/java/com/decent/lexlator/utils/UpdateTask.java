package com.decent.lexlator.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.decent.lexlator.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

// TODO: Fix code duplication among download calls
// TODO: Update progress to be more accurate
public class UpdateTask extends AsyncTask<String, Integer, Void> {

    private static final String TAG = "UpdateTask";
    //private static final int ANIMATION_DURATION = 200;
    private static final String LEGISLATORS_URL = "https://theunitedstates.io/congress-legislators/legislators-current.json";
    private static final String SOCIAL_MEDIA_URL = "https://theunitedstates.io/congress-legislators/legislators-social-media.json";
    private static final String COMMITTEES_URL = "https://theunitedstates.io/congress-legislators/committees-current.json";
    private static final String MEMBERSHIP_URL = "https://theunitedstates.io/congress-legislators/committee-membership-current.json";
    // https://api.propublica.org/congress/v1/{chamber}/votes/{start-date}/{end-date}.json
    // private static final String VOTES_URL = "https://api.propublica.org/congress/v1/%s/votes/%s/%s.json";
    // https://api.propublica.org/congress/v1/{chamber}/votes/recent.json
    private static final String VOTES_URL = "https://api.propublica.org/congress/v1/%s/votes/recent.json";
    // https://api.propublica.org/congress/v1/{congress}/{chamber}/bills/{type}.json
    private static final String BILLS_URL = "https://api.propublica.org/congress/v1/%s/%s/bills/%s.json";
    private static final String PRO_API_KEY = "c2tpYc0rKnNfyf65N7p4lSBRgbzVHAYdgdrY2PGH ";


    private PowerManager.WakeLock mWakeLock;
    private Context context;
    private ViewGroup progressOverlay;
    private ViewGroup contentLayout;
    private TextView progressTextView;
    private String progressText = "Updating...";
    private int updateInterval;
    private ProgressBar progressBar;
    private CacheManager cacheManager;
    private File localCache;

    UpdateTask(Context parentContext, ViewGroup overlay, ViewGroup content, CacheManager manager) {
        context = parentContext;
        String updateString = PreferenceManager.getDefaultSharedPreferences(context).getString("update_interval_preference", "1");
        updateInterval = Integer.parseInt(updateString);
        progressOverlay = overlay;
        contentLayout = content;
        progressBar = progressOverlay.findViewById(R.id.update_progress_bar);
        progressTextView = progressOverlay.findViewById(R.id.progress_text);
        cacheManager = manager;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // take CPU lock to prevent CPU from going off if the user
        // presses the power button during download
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        assert pm != null;
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getClass().getName());
        mWakeLock.acquire(15000);

        contentLayout.setVisibility(View.GONE);

        //AlphaAnimation inAnimation = new AlphaAnimation(0f, 1f);
        //inAnimation.setDuration(ANIMATION_DURATION);
        //progressOverlay.setAnimation(inAnimation);

        progressOverlay.setVisibility(View.VISIBLE);
    }

    @Override
    protected Void doInBackground(String... strings) {

        localCache = new File(strings[0]);

        File legislatorsFile = new File(localCache, "legislators-current.json");

        Log.d(TAG, "Downloading files");

        progressText = "Downloading Legislator Information";

        // committee membership
        String membershipPath = localCache.getAbsolutePath() + "/committee-membership-current.json";
        if (0 != cacheManager.downloadFile(MEMBERSHIP_URL, membershipPath)) {
            Log.e(TAG, "Failed to download: " + MEMBERSHIP_URL);
        }

        // current committees
        // committees-current.json, committee-membership-current.json
        String committeesPath = localCache.getAbsolutePath() + "/committees-current.json";
        if (0 != cacheManager.downloadFile(COMMITTEES_URL, committeesPath)) {
            Log.e(TAG, "Failed to download: " + COMMITTEES_URL);
        }

        // social media
        String socialMediaPath = localCache.getAbsolutePath() + "/legislators-social-media.json";
        if (0 != cacheManager.downloadFile(SOCIAL_MEDIA_URL, socialMediaPath)) {
            Log.e(TAG, "Failed to download: " + SOCIAL_MEDIA_URL);
        }

        // current legislators
        String legislatorsPath = localCache.getAbsolutePath() + "/legislators-current.json";
        if (0 != cacheManager.downloadFile(LEGISLATORS_URL, legislatorsPath)) {
            Log.e(TAG, "Failed to download: " + LEGISLATORS_URL);
        }
        else {
            populateStates(legislatorsFile);
        }

        // download votes
        progressText = "Downloading Votes";
        if (0 != downloadVotes()) {
            Log.e(TAG, "Failed to download votes");
        }

        // download bills
        progressText = "Downloading Bills";
        if (0 != downloadBills()) {
            Log.e(TAG, "Failed to download bills");
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer...progress) {
        super.onProgressUpdate(progress);

        progressTextView.setText(progressText);

        if (progress[0] == -1) {
            progressBar.setIndeterminate(true);
        }
        else {
            // if we get here, length is known, now set indeterminate to false
            progressBar.setIndeterminate(false);
            progressBar.setMax(100);
            progressBar.setProgress(progress[0]);
        }
    }

    @Override
    protected void onPostExecute(Void avoid) {
        super.onPostExecute(avoid);

        //AlphaAnimation outAnimation = new AlphaAnimation(1f, 0f);
        //outAnimation.setDuration(ANIMATION_DURATION);
        //progressOverlay.setAnimation(outAnimation);

        progressOverlay.setVisibility(View.GONE);

        contentLayout.setVisibility(View.VISIBLE);

        mWakeLock.release();
    }

    private void populateStates(File legislatorsFile) {
        Log.d(TAG, "Checking if state files are already populated");
        File statesDir = new File(localCache, "states");
        if (statesDir.exists()) {
            File[] dirList = statesDir.listFiles();
            if (null != dirList && dirList.length >= 50) {
                Log.d(TAG, "State files found, returning");
                return;
            }
        }
        else {
            if (!statesDir.mkdir()) {
                Log.e(TAG, "Failed to create: " + statesDir.getAbsolutePath());
                return;
            }
        }

        Log.d(TAG, "Reading cached file");
        String legislatorsRaw = cacheManager.readFile(legislatorsFile.getAbsolutePath());
        Log.d(TAG, "Finished reading cached file");

        try {
            JSONArray legislatorsJSON = new JSONArray(legislatorsRaw);
            Log.d(TAG, "Populating legislators");
            if (0 == populateLegislators(statesDir, legislatorsJSON)) Log.d(TAG, "Finished populating legislators");
            else {
                Log.e(TAG, "populateLocal(...) failed");
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON Error: " + e.toString());
        }
    }

    private int populateLegislators(File statesDir, JSONArray legislators) throws JSONException {
        int total = legislators.length();

        for (int i = 0; i < total; i++){
            JSONObject currentLegislator = legislators.getJSONObject(i);
            int result = createLegislator(statesDir, currentLegislator);
            if (0 != result) {
                Log.e(TAG, "Failed to create legislator: " + i);
            }

            int progress = i * 100 / total;
            publishProgress(progress);
        }
        return 0;
    }

    private int createLegislator(File statesDir, JSONObject legislator) throws JSONException {
        // get terms
        JSONArray terms = legislator.getJSONArray("terms");
        int termsLength = terms.length();
        // Get current term
        JSONObject currentTerm = terms.getJSONObject(termsLength - 1);
        // get state
        String legislatorState = currentTerm.getString("state");
        // check/create state folder
        File statePath = new File(statesDir, legislatorState);
        if (!statePath.exists()) {
            if (!statePath.mkdir()) {
                Log.e(TAG, "Failed to create directory: " + statePath);
                return 1;
            }
        }
        // Create directory for legislator ex: S-R-First Last, R-12-D-First Last
        String legislatorDirName = getLegislatorDir(legislator, currentTerm);
        File legislatorDir = new File(statePath, legislatorDirName);
        if (!legislatorDir.exists()) {
            if (!legislatorDir.mkdir()) {
                Log.e(TAG, "Failed to create directory: " + legislatorDir);
                return 1;
            }
        }

        return writeLegislatorFile(legislatorDir, legislator, currentTerm);
    }

    private int writeLegislatorFile(File legislatorDir, JSONObject legislator, JSONObject term) throws JSONException {
        String infoString = "info.json";
        String termsString = "terms.json";

        File infoFile = new File(legislatorDir, infoString);
        File termsFile = new File(legislatorDir, termsString);

        String terms = legislator.getJSONArray("terms").toString(4);
        legislator.remove("terms");
        if (0 != cacheManager.writeFile(termsFile.getAbsolutePath(), terms)) {
            Log.e(TAG, "Failed to write terms: " + termsFile.getAbsolutePath());
        }

        String legislatorInfo = legislator.put("term", term).toString(4);
        if (0 != cacheManager.writeFile(infoFile.getAbsolutePath(), legislatorInfo)) {
            Log.e(TAG, "Failed to write info: " + infoFile.getAbsolutePath());
        }

        return 0;
    }

    private int downloadVotes() {
        Log.d(TAG, "Downloading votes...");
        File votesFile = new File(localCache, "votes");
        String houseVotesPath = votesFile.getAbsolutePath() + "/house-votes.json";
        String senateVotesPath = votesFile.getAbsolutePath() + "/senate-votes.json";

        if (!votesFile.exists()) {
            if (!votesFile.mkdir()) {
                Log.e(TAG, "Failed to create file: " + votesFile.getAbsolutePath());
                return 1;
            }
        }
        else {
            // Get the last modified date from local file
            File senateVotesFile = new File(senateVotesPath);
            if (senateVotesFile.exists()) {
                Date lastModified = new Date(senateVotesFile.lastModified());

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, - updateInterval);
                Date oldestDate = calendar.getTime();
                if (lastModified.after(oldestDate)) {
                    Log.d(TAG, "Vote files within date range, no update required, exiting");
                    return 0;
                }
            }
        }

        /*
        // Get two week date range
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();
        calendar.add(Calendar.DATE, -14);
        Date startDate = calendar.getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        */

        String houseVotesUrl = String.format(
                VOTES_URL, "house"//, formatter.format(startDate), formatter.format(endDate)
        );
        String apiArg = "X-API-Key";
        if (0 != cacheManager.downloadFile(houseVotesUrl, houseVotesPath, apiArg, PRO_API_KEY)) {
            Log.e(TAG, "Failed to download: " + houseVotesUrl);
            return 1;
        }

        String senateVotesUrl = String.format(
                VOTES_URL, "senate"//, formatter.format(startDate), formatter.format(endDate)
        );
        if (0 != cacheManager.downloadFile(senateVotesUrl, senateVotesPath, apiArg, PRO_API_KEY)) {
            Log.e(TAG, "Failed to download: " + senateVotesUrl);
            return 1;
        }

        return populateVotes(votesFile, houseVotesPath, senateVotesPath);
    }

    private int populateVotes(File votesFile, String houseVotesPath, String senateVotesPath) {

        String old = progressText;
        progressText = progressText + " - House";
        int res = populateChamberVotes("house", votesFile, houseVotesPath);
        progressText = old + " - Senate";
        int ras = populateChamberVotes("senate", votesFile, senateVotesPath);

        return res + ras;
    }

    private int populateChamberVotes(String chamber, File votesFile, String chamberVotesPath) {
        File chamberVotesDir = new File(votesFile, chamber);
        Log.d(TAG, "Populating chamber votes from file: " + chamberVotesPath);
        if (!chamberVotesDir.exists()) {
            if (!chamberVotesDir.mkdir()) {
                Log.e(TAG, "Failed to create: " + chamberVotesDir.getAbsolutePath());
                return 1;
            }
        }
        String chamberVotes = cacheManager.readFile(chamberVotesPath);
        if (null != chamberVotes) {
            JSONObject chamberJson = cacheManager.stringToJSON(chamberVotes);
            if (null != chamberJson) {
                // File format: {chamber}-{congress #}-{session #}-{roll #} = C-123-1-12345
                try {
                    JSONArray votesArray = chamberJson.getJSONObject("results").getJSONArray("votes");
                    int totalVotes = votesArray.length();
                    for (int i = 0; i < votesArray.length(); i++) {
                        Log.d(TAG, String.format(Locale.US, "Populating vote %d for %s", i, chamber));

                        JSONObject vote = votesArray.getJSONObject(i);
                        String voteName = chamber.substring(0,1) + "-";
                        voteName += vote.getInt("congress") + "-";
                        voteName += vote.getInt("session") + "-";
                        voteName += String.format(Locale.US, "%05d", vote.getInt("roll_call")) + ".json";
                        Log.d(TAG, "Vote file: " + voteName);

                        String voteUrl = vote.getString("vote_uri");

                        String outputPath = chamberVotesDir.getAbsolutePath() + "/" + voteName;

                        if (0 != cacheManager.downloadFile(voteUrl, outputPath, "X-API-Key", PRO_API_KEY))
                            Log.e(TAG, "Failed to download: " + voteUrl);

                        publishProgress(i * 100 / totalVotes);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "populateChamberVotes(...) JSON error: " + e.toString());
                    return 1;
                }
            }
            else {
                Log.e(TAG, "chamberJson == null, exiting");
                return 1;
            }
        }
        else {
            Log.e(TAG, "(String) chamberVotes == null, exiting");
            return 1;
        }

        return 0;
    }

    private int downloadBills() {
        Log.d(TAG, "Downloading bills...");
        File billsFile = new File(localCache, "bills");
        String houseBillsPath = billsFile.getAbsolutePath() + "/house-bills.json";
        String senateBillsPath = billsFile.getAbsolutePath() + "/senate-bills.json";

        if (!billsFile.exists()) {
            if (!billsFile.mkdir()) {
                Log.e(TAG, "Failed to create file: " + billsFile.getAbsolutePath());
                return 1;
            }
        }
        else {
            // Get the last modified date from local file
            File senateBillsFile = new File(senateBillsPath);

            if (senateBillsFile.exists()) {
                Date senateLastModified = new Date(senateBillsFile.lastModified());

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, - updateInterval);
                Date oldestDate = calendar.getTime();
                if (senateLastModified.after(oldestDate)) {
                    Log.d(TAG, "Bill files within date range, no update required, exiting");
                    return 0;
                }
            }
        }

        String congress = "116";
        String billType = "introduced";

        /*
        // Get two week date range
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();
        calendar.add(Calendar.DATE, -14);
        Date startDate = calendar.getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        */

        String houseBillsUrl = String.format(
                BILLS_URL, congress, "house", billType//, formatter.format(startDate), formatter.format(endDate)
        );
        String apiArg = "X-API-Key";
        if (0 != cacheManager.downloadFile(houseBillsUrl, houseBillsPath, apiArg, PRO_API_KEY)) {
            Log.e(TAG, "Failed to download: " + houseBillsUrl);
            return 1;
        }

        String senateBillsUrl = String.format(
                BILLS_URL, congress, "senate", billType//, formatter.format(startDate), formatter.format(endDate)
        );
        if (0 != cacheManager.downloadFile(senateBillsUrl, senateBillsPath, apiArg, PRO_API_KEY)) {
            Log.e(TAG, "Failed to download: " + senateBillsUrl);
            return 1;
        }

        return populateBills(billsFile, houseBillsPath, senateBillsPath);
    }

    private int populateBills(File billsFile, String houseBillsString, String senateBillsString) {

        String old = progressText;
        progressText = old + " - House";
        int res = populateChamberBills("house", billsFile, houseBillsString);
        progressText = old + " - Senate";
        int ras = populateChamberBills("senate", billsFile, senateBillsString);

        return res + ras;
    }

    private int populateChamberBills(String chamber, File billsFile, String chamberBillsPath) {
        File chamberBillDir = new File(billsFile, chamber);
        Log.d(TAG, "Populating chamber bills from file: " + chamberBillsPath);
        if (!chamberBillDir.exists()) {
            if (!chamberBillDir.mkdir()) {
                Log.e(TAG, "Failed to create: " + chamberBillDir.getAbsolutePath());
                return 1;
            }
        }
        String chamberBills = cacheManager.readFile(chamberBillsPath);
        if (null != chamberBills) {
            JSONObject chamberJson = cacheManager.stringToJSON(chamberBills);
            if (null != chamberJson) {
                // File format: {chamber}{bill #}-{congress} = ch1234-123
                try {
                    JSONArray resultsArray = chamberJson.getJSONArray("results");
                    int totalBills = resultsArray.length();
                    for (int j = 0; j < resultsArray.length(); j++) {
                        JSONObject chamberCongressJson = resultsArray.getJSONObject(j);
                        JSONArray billsArray = chamberCongressJson.getJSONArray("bills");

                        for (int i = 0; i < billsArray.length(); i++) {
                            Log.d(TAG, String.format(Locale.US, "Populating vote %d for %s", i, chamber));

                            JSONObject bill = billsArray.getJSONObject(i);

                            String billName = bill.getString("bill_id") + ".json";

                            Log.d(TAG, "Bill file: " + billName);

                            String billUrl = bill.getString("bill_uri");

                            String outputPath = chamberBillDir.getAbsolutePath() + "/" + billName;

                            if (0 != cacheManager.downloadFile(billUrl, outputPath, "X-API-Key", PRO_API_KEY))
                                Log.e(TAG, "Failed to download: " + billUrl);

                            publishProgress(i * 100 / totalBills);
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "populateChamberBills(...) JSON error: " + e.toString());
                    return 1;
                }
            }
            else {
                Log.e(TAG, "chamberJson == null, exiting");
                return 1;
            }
        }
        else {
            Log.e(TAG, "(String) chamberBills == null, exiting");
            return 1;
        }

        return 0;
    }

    private String getLegislatorDir(JSONObject legislator, JSONObject currentTerm) throws JSONException {
        // ex: S-R-First Last, R-12-D-First Last
        String fileName = "";

        String type = currentTerm.getString("type");
        if (type.equals("rep")) {
            fileName = fileName.concat("R-");
            fileName = fileName.concat(currentTerm.getString("district") + "-");
        }
        else {
            fileName = fileName.concat("S-");
        }

        fileName = fileName.concat(currentTerm.getString("party").substring(0, 1) + "-");
        JSONObject legislatorName = legislator.getJSONObject("name");
        fileName = fileName.concat(legislatorName.getString("first") + "-");
        fileName = fileName.concat(legislatorName.getString("last"));

        return fileName;
    }
}
