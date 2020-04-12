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
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

public class UpdateTask extends AsyncTask<String, Integer, Void> {

    private static final String TAG = "UpdateTask";
    //private static final int ANIMATION_DURATION = 200;
    private static final String CACHE_URL = "https://theunitedstates.io/congress-legislators/legislators-current.json";
    private static final String COMMITTEES_URL = "https://theunitedstates.io/congress-legislators/committees-current.json";
    private static final String MEMBERSHIP_URL = "https://theunitedstates.io/congress-legislators/committee-membership-current.json";

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

        Log.d(TAG, "Checking if state files are already populated");
        File[] dirList = localCache.listFiles();
        if (null != dirList && dirList.length >= 50) {
            Log.d(TAG, "State files found, returning");
            return null;
        }
        Log.d(TAG, "Reading cached file");
        String legislatorsRaw = cacheManager.readFile(cacheFile.getAbsolutePath());
        Log.d(TAG, "Finished reading cached file");

        try {
            JSONArray legislatorsJSON = new JSONArray(legislatorsRaw);
            Log.d(TAG, "Populating legislators");
            if (0 == populateLocal(legislatorsJSON)) Log.d(TAG, "Finished populating legislators");
            else Log.e(TAG, "populateLocal(...) failed");
        } catch (JSONException e) {
            Log.e(TAG, "JSON Error: " + e.toString());
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

    private int downloadFile(String fileUrl, String filePath) {
        File downloadFile = new File(filePath);
        progressText = "Downloading " + downloadFile.getName();
        Date lastModified;

        if (!downloadFile.exists()) lastModified = null;
        else lastModified = new Date(downloadFile.lastModified());

        return downloadFile(fileUrl, filePath, lastModified);
    }

    private int downloadFile(String fileUrl, String filePath, Date cacheLastModified) {
        InputStream input = null;
        OutputStream output = null;
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(fileUrl);

            connection = (HttpsURLConnection) url.openConnection();
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
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }
        return 0;
    }

    private int populateLocal(JSONArray legislators) throws JSONException {
        progressText = "Populating local cache...";
        int total = legislators.length();

        for (int i = 0; i < total; i++){
            JSONObject currentLegislator = legislators.getJSONObject(i);
            int result = createLegislator(currentLegislator);
            if (0 != result) {
                Log.e(TAG, "Failed to create legislator: " + i);
            }

            int progress = i * 100 / total;
            publishProgress(progress);
        }
        return 0;
    }

    private int createLegislator(JSONObject legislator) throws JSONException {
        // get terms
        JSONArray terms = legislator.getJSONArray("terms");
        int termsLength = terms.length();
        // Get current term
        JSONObject currentTerm = terms.getJSONObject(termsLength - 1);
        // get state
        String legislatorState = currentTerm.getString("state");
        // check/create state folder
        File statePath = new File(localCache, legislatorState);
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
