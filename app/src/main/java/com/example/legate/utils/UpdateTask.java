package com.example.legate.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.legate.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

// TODO: Fix code duplication among download calls
// TODO: Add votes-last-updated to config.json
public class UpdateTask extends AsyncTask<String, Integer, Void> {

    private static final String TAG = "UpdateTask";
    //private static final int ANIMATION_DURATION = 200;
    private static final String CACHE_URL = "https://theunitedstates.io/congress-legislators/legislators-current.json";
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
    private ProgressBar progressBar;
    private CacheManager cacheManager;
    private File localCache;

    public UpdateTask(Context parentContext, ViewGroup overlay, ViewGroup content, CacheManager manager) {
        context = parentContext;
        progressOverlay = overlay;
        contentLayout = content;
        progressBar = progressOverlay.findViewById(R.id.update_progress_bar);
        progressTextView = progressOverlay.findViewById(R.id.progress_text);
        cacheManager = manager;
    }

    private void sleepTest() {
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

        File cacheFile = new File(localCache, "legislators-current.json");

        Log.d(TAG, "Downloading files");

        String membershipPath = localCache.getAbsolutePath() + "/committee-membership-current.json";
        if (0 != downloadFile(MEMBERSHIP_URL, membershipPath)) {
            Log.e(TAG, "Failed to download: " + MEMBERSHIP_URL);
        }

        // committees-current.json, committee-membership-current.json
        String committeesPath = localCache.getAbsolutePath() + "/committees-current.json";
        if (0 != downloadFile(COMMITTEES_URL, committeesPath)) {
            Log.e(TAG, "Failed to download: " + COMMITTEES_URL);
        }

        String legislatorsPath = localCache.getAbsolutePath() + "/legislators-current.json";
        if (0 != downloadFile(CACHE_URL, legislatorsPath)) {
            Log.e(TAG, "Failed to download: " + CACHE_URL);
        }
        else {
            populateStates(cacheFile);
        }


        if (0 != downloadVotes()) {
            Log.e(TAG, "Failed to download votes");
        }

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

    private int downloadFile(String fileUrl, String filePath, String apiArg, String apiKey) {
        File downloadFile = new File(filePath);
        progressText = "Downloading " + downloadFile.getName();
        Date lastModified;

        if (!downloadFile.exists()) lastModified = null;
        else lastModified = new Date(downloadFile.lastModified());

        return downloadFile(fileUrl, filePath, apiArg, apiKey, lastModified);
    }

    private int downloadFile(String fileUrl, String filePath, String apiArg, String apiKey, Date cacheLastModified) {
        InputStream input = null;
        OutputStream output = null;
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(fileUrl);

            connection = (HttpsURLConnection) url.openConnection();
            if (null != apiKey && null != apiArg) {
                Log.d(TAG, "Adding api key to request: " + apiArg + ": " + apiKey);
                // connection.setRequestProperty("X-API-Key", apiKey);
                connection.setRequestProperty(apiArg, apiKey);
            }
            connection.setConnectTimeout(5000);
            connection.connect();

            Log.d(TAG, "Headers: \n" + connection.getHeaderFields());

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage()
                );
                return 1;
            }
            Log.d(TAG, "Connection established");

            // Get the last-modified header and compare with last_modified date if not null
            if (null != cacheLastModified) {
                String urlModified = connection.getHeaderField("last-modified");
                if (null != urlModified) {
                    // Example date: Thu, 02 Apr 2020 11:18:39 GMT
                    Log.d(TAG, "Raw URL modified string: " + urlModified);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                    Date urlModifiedDate = dateFormat.parse(urlModified);
                    assert urlModifiedDate != null;
                    Log.d(TAG, "URL Updated: " + urlModifiedDate.toString());
                    if (urlModifiedDate.before(cacheLastModified)) {
                        Log.d(TAG, "Cache file up to date, exiting download");
                        return 0;
                    }
                }
                else Log.d(TAG, "Failed to get last-modified form header, continuing");
            }
            else Log.d(TAG, "Cache last modified date == null, continuing");

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int fileLength = connection.getContentLength();
            // If server did not report the length, try to get it from the header
            if (-1 == fileLength) {
                String headerFileLength = connection.getHeaderField("content-length");
                if (null != headerFileLength) {
                    Log.d(TAG, "Raw file length string from header: " + headerFileLength);
                    fileLength = Integer.parseInt(headerFileLength);
                }
            }

            // download the file
            input = connection.getInputStream();
            Log.d(TAG, "Downloading file to: " + filePath);
            output = new FileOutputStream(filePath);

            byte[] data = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                // allow canceling with back button
                if (isCancelled()) {
                    input.close();
                    return 1;
                }
                total += count;
                // publishing the progress....
                if (fileLength > 0) {
                    // only if total length is known
                    int progress = (int) (total * 100 / fileLength);
                    publishProgress(progress);
                }
                else {
                    publishProgress(-1);
                }
                output.write(data, 0, count);
            }
            Log.d(TAG, "File length: " + fileLength);
            Log.d(TAG, "Bytes downloaded: " + total);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return 1;
        } finally {
            Log.d(TAG, "Closing HTTPS connection and output stream");
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }

            if (connection != null)
                connection.disconnect();
        }
        return 0;
    }

    private int downloadFile(String fileUrl, String filePath) {
        return downloadFile(fileUrl, filePath, null, null);
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
        progressText = "Populating local cache...";
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
        if (!votesFile.exists()) {
            if (!votesFile.mkdir()) {
                Log.e(TAG, "Failed to create file: " + votesFile.getAbsolutePath());
                return 1;
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

        String houseVotesPath = votesFile.getAbsolutePath() + "/house-votes.json";
        String houseVotesUrl = String.format(
                VOTES_URL, "house"//, formatter.format(startDate), formatter.format(endDate)
        );
        String apiArg = "X-API-Key";
        if (0 != downloadFile(houseVotesUrl, houseVotesPath, apiArg, PRO_API_KEY)) {
            Log.e(TAG, "Failed to download: " + houseVotesUrl);
            return 1;
        }

        String senateVotesPath = votesFile.getAbsolutePath() + "/senate-votes.json";
        String senateVotesUrl = String.format(
                VOTES_URL, "senate"//, formatter.format(startDate), formatter.format(endDate)
        );
        if (0 != downloadFile(senateVotesUrl, senateVotesPath, apiArg, PRO_API_KEY)) {
            Log.e(TAG, "Failed to download: " + senateVotesUrl);
            return 1;
        }

        return populateVotes(votesFile, houseVotesPath, senateVotesPath);
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

                        if (0 != downloadFile(voteUrl, outputPath, "X-API-Key", PRO_API_KEY))
                            Log.e(TAG, "Failed to download: " + voteUrl);

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

    private int populateVotes(File votesFile, String houseVotesPath, String senateVotesPath) {

        int res = populateChamberVotes("house", votesFile, houseVotesPath);
        int ras = populateChamberVotes("senate", votesFile, senateVotesPath);

        return res + ras;
    }

    private int downloadBills() {
        Log.d(TAG, "Downloading bills...");
        File billsFile = new File(localCache, "bills");
        if (!billsFile.exists()) {
            if (!billsFile.mkdir()) {
                Log.e(TAG, "Failed to create file: " + billsFile.getAbsolutePath());
                return 1;
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

        String houseBillsPath = billsFile.getAbsolutePath() + "/house-bills.json";
        String houseBillsUrl = String.format(
                BILLS_URL, congress, "house", billType//, formatter.format(startDate), formatter.format(endDate)
        );
        String apiArg = "X-API-Key";
        if (0 != downloadFile(houseBillsUrl, houseBillsPath, apiArg, PRO_API_KEY)) {
            Log.e(TAG, "Failed to download: " + houseBillsUrl);
            return 1;
        }

        String senateBillsPath = billsFile.getAbsolutePath() + "/senate-bills.json";
        String senateBillsUrl = String.format(
                BILLS_URL, congress, "senate", billType//, formatter.format(startDate), formatter.format(endDate)
        );
        if (0 != downloadFile(senateBillsUrl, senateBillsPath, apiArg, PRO_API_KEY)) {
            Log.e(TAG, "Failed to download: " + senateBillsUrl);
            return 1;
        }

        return populateBills(billsFile, houseBillsPath, senateBillsPath);
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

                            if (0 != downloadFile(billUrl, outputPath, "X-API-Key", PRO_API_KEY))
                                Log.e(TAG, "Failed to download: " + billUrl);
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

    private int populateBills(File billsFile, String houseBillsString, String senateBillsString) {
        int res = populateChamberBills("house", billsFile, houseBillsString);
        int ras = populateChamberBills("senate", billsFile, senateBillsString);

        return res + ras;
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
