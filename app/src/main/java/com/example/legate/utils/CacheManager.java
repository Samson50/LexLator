package com.example.legate.utils;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.legate.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class CacheManager {

    private static final String TAG = "CacheManager";
    private static final String CACHE_URL = "https://theunitedstates.io/congress-legislators/legislators-current.json";

    private int progress = 0;
    private boolean isCancelled = false;
    private TextView progressText;
    private File localCache;

    public CacheManager() {
        Log.d(TAG, "Creating cache class instance");
    }

    public int updateLocalCache(Context context, View parentView) {
        Log.d(TAG, "Update called");
        localCache = context.getCacheDir();
        final File threadCacheFile = new File(localCache.getAbsolutePath());

        // Set progress overlay to visible and get the view for text updates
        final View overlay = parentView.findViewById(R.id.progress_overlay);
        overlay.setVisibility(View.VISIBLE);
        progressText = parentView.findViewById(R.id.progress_text);

        // Thread to download, read, and populate cache file
        Thread updateCacheThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File cacheFile = new File(localCache, "current_legislators.json");
                    Date lastModified = null;
                    int result;

                    if (cacheFile.exists()) {
                        Log.d(TAG, "File found");
                        lastModified = new Date(cacheFile.lastModified());
                        Log.d(TAG, "Cache last modified: " + lastModified.toString());
                    }

                    Log.d(TAG, "Starting download");
                    result = downloadFileBlocking(CACHE_URL, cacheFile.getAbsolutePath(), lastModified);
                    if (result == 0) Log.d(TAG, "Download complete");
                    else {
                        Log.e(TAG, "Download failed");
                        return;
                    }

                    Log.d(TAG, "Checking if state files are already populated");
                    File[] dirList = threadCacheFile.listFiles();
                    if (null != dirList && dirList.length >= 50) {
                        Log.d(TAG, "State files found, returning");
                        return;
                    }
                    Log.d(TAG, "Reading cached file");
                    String legislatorsRaw = readFile(cacheFile.getAbsolutePath());
                    Log.d(TAG, "Finished reading cached file");

                    try {
                        JSONArray legislatorsJSON = new JSONArray(legislatorsRaw);
                        Log.d(TAG, "Populating legislators");
                        result = populateLocal(legislatorsJSON);
                        if (0 == result) Log.d(TAG, "Finished populating legislators");
                        else {
                            Log.e(TAG, "populateLocal(...) failed");
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Error: " + e.toString());
                    }
                }
                finally {
                    Log.d(TAG, "Removing progress overlay");
                    overlay.post(new Runnable() {
                        @Override
                        public void run() {
                            overlay.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
        updateCacheThread.start();

        return 0;
    }

    private int writeLegislatorFile(File legislatorDir, JSONObject legislator, JSONObject term) throws JSONException {
        String infoString = "info.json";
        String termsString = "terms.json";

        File infoFile = new File(legislatorDir, infoString);
        File termsFile = new File(legislatorDir, termsString);

        try {
            String terms = legislator.getJSONArray("terms").toString(4);
            legislator.remove("terms");
            FileWriter termWriter = new FileWriter(termsFile.getAbsoluteFile());
            BufferedWriter termBuffer = new BufferedWriter(termWriter);
            termBuffer.write(terms);
            termBuffer.close();

            String legislatorInfo = legislator.put("term", term).toString(4);
            FileWriter legislatorWriter = new FileWriter(infoFile.getAbsoluteFile());
            BufferedWriter legislatorBuffer = new BufferedWriter(legislatorWriter);
            legislatorBuffer.write(legislatorInfo);
            legislatorBuffer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
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

    private void updateProgress(String action) {
        final String update = action.concat(Integer.toString(progress));
        progressText.post(new Runnable() {
            @Override
            public void run() {
                progressText.setText(update);
            }
        });
    }

    private int populateLocal(JSONArray legislators) throws JSONException {
        int total = legislators.length();

        for (int i = 0; i < total; i++){
            JSONObject currentLegislator = legislators.getJSONObject(i);
            int result = createLegislator(currentLegislator);
            if (0 != result) {
                Log.e(TAG, "Failed to create legislator: " + i);
            }

            progress = i * 100 / total;
            updateProgress("Populating Cache: ");
        }
        return 0;
    }

    private int downloadFileBlocking(String fileUrl, String filePath, Date cacheLastModified) {
        InputStream input = null;
        OutputStream output = null;
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(fileUrl);

            connection = (HttpsURLConnection) url.openConnection();
            connection.connect();

            /*
                {null=[HTTP/1.1 200 OK], Accept-Ranges=[bytes], Access-Control-Allow-Origin=[*],
                Age=[331], Cache-Control=[max-age=600], Connection=[keep-alive],
                Content-Type=[application/json; charset=utf-8],
                Date=[Thu, 09 Apr 2020 19:58:20 GMT], ETag=[W/"5e85ca0f-154469"],
                Expires=[Thu, 09 Apr 2020 19:56:00 GMT],
                Last-Modified=[Thu, 02 Apr 2020 11:18:39 GMT], Server=[GitHub.com],
                Vary=[Accept-Encoding], Via=[1.1 varnish], X-Android-Received-Millis=[1586462291090],
                X-Android-Response-Source=[NETWORK 200], X-Android-Selected-Protocol=[http/1.1],
                X-Android-Sent-Millis=[1586462291061], X-Cache=[HIT], X-Cache-Hits=[1],
                X-Fastly-Request-ID=[71d03a863191d7f48032cbcb76207606fa34d72f],
                X-GitHub-Request-Id=[9AC4:4BE5:44AB26:568A37:5E8F7B78], X-Proxy-Cache=[MISS],
                X-Served-By=[cache-ewr18145-EWR], X-Timer=[S1586462301.625007,VS0,VE1]}
             */
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
                if (isCancelled) {
                    input.close();
                    return 1;
                }
                total += count;
                // publishing the progress....
                if (fileLength > 0) // only if total length is known
                    progress = (int) (total * 100 / fileLength);
                    updateProgress("Downloading: ");
                output.write(data, 0, count);
            }
            Log.d(TAG, "File length: " + fileLength);
            Log.d(TAG, "Bytes downloaded: " + total);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
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

    int downloadFile(String fileUrl, String filePath, Date cacheLastModified) {
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
                } else Log.d(TAG, "Failed to get last-modified form header, continuing");
            } else Log.d(TAG, "Cache last modified date == null, continuing");

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
                if (isCancelled) {
                    input.close();
                    return 1;
                }
                total += count;
                // publishing the progress....
                if (fileLength > 0) // only if total length is known
                    progress = (int) (total * 100 / fileLength);
                output.write(data, 0, count);
            }
            Log.d(TAG, "File length: " + fileLength);
            Log.d(TAG, "Bytes downloaded: " + total);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
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

    public JSONObject stringToJSON(String rawString) {
        try {
            Log.d(TAG, "Converting string to JSONObject");
            return new JSONObject(rawString);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }

    public String getJSONField(File inputFile, String key) {
        String outputValue;
        String infoString;

        infoString = readFile(inputFile.getAbsolutePath());
        if (null == infoString) {
            Log.e(TAG, "Failed to read file: " + inputFile.getPath());
            return null;
        }

        JSONObject inputJSON = stringToJSON(infoString);
        if (null == inputJSON) {
            Log.e(TAG, "Failed to convert string to JSONObject");
            return null;
        }

        try {
            Log.d(TAG, String.format("Getting key (%s) value from JSONObject", key));
            outputValue = inputJSON.getString(key);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }

        Log.d(TAG, "Value: " + outputValue);
        return outputValue;
    }

    public String readFile(String filePath) {
        String ret = "";

        try {
            FileInputStream fileInputStream = new FileInputStream(new File(filePath));

            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();

            String receiveString;
            while ( (receiveString = bufferedReader.readLine()) != null ) {
                stringBuilder.append("\n").append(receiveString);
            }

            fileInputStream.close();
            ret = stringBuilder.toString();
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + filePath + " - " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "Can not read file: " + filePath + " - " + e.toString());
        }

        return ret;
    }

    public void cancel() {
        isCancelled = true;
    }
}
