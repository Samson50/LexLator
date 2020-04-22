package com.example.legate.utils;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;


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


// TODO: Fix code duplication among download tasks
public class CacheManager {

    private static final String TAG = "CacheManager";

    private static File localCache;


    public CacheManager() {
        Log.d(TAG, "Creating cache class instance");
    }

    public int updateLocalCache(Context context, ViewGroup progressOverlay, ViewGroup contentLayout) {
        Log.d(TAG, "Update called");
        localCache = context.getCacheDir();

        UpdateTask updateTask = new UpdateTask(context, progressOverlay, contentLayout, this);
        updateTask.execute(localCache.getAbsolutePath());

        return 0;
    }

    public int writeFile(String filePath, JSONArray content) {
        try {
            return writeFile(filePath, content.toString(4));
        } catch (JSONException e) {
            Log.e(TAG, "writeFile(String, JSONArray) failed...");
            return 1;
        }
    }

    public int writeFile(String filePath, JSONObject content) {
        try {
            return writeFile(filePath, content.toString(4));
        } catch (JSONException e) {
            Log.e(TAG, "writeFile(String, JSONArray) failed...");
            return 1;
        }
    }

    public int writeFile(String filePath, String content) {
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            BufferedWriter fileBuffer = new BufferedWriter(fileWriter);
            fileBuffer.write(content);
            fileBuffer.close();
        }
        catch (IOException e) {
            Log.e(TAG, "writeFile(String, String) failed: " + e.toString());
            return 1;
        }
        return 0;
    }

    public void downloadFile(String fileUrl, String filePath) {
        //DownloadTask downloadTask = new DownloadTask();
        //downloadTask.execute(fileUrl, filePath);
        //downloadTasks.add(downloadTask);
        File downloadFile = new File(filePath);
        Date lastModified = null;
        if (downloadFile.exists()) lastModified = new Date(downloadFile.lastModified());

        if (0 != downloadFile(fileUrl, filePath, lastModified)) {
            Log.e(TAG, "Download failed...");
        }
    }

    public int downloadFile(String fileUrl, String filePath, Date cacheLastModified) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.e(TAG, "Attempting to run downloadFile on main thread, exiting");
            return 1;
        }
        InputStream input = null;
        OutputStream output = null;
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(fileUrl);

            Log.d(TAG, "Establishing HTTPS connection");
            connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(1000);
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
                /*
                boolean isCancelled = false;
                if (isCancelled) {
                    input.close();
                    return 1;
                }
                */

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

    public JSONObject stringToJSON(String rawString) {
        try {
            Log.d(TAG, "Converting string to JSONObject");
            return new JSONObject(rawString);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }

    public JSONArray stringToJSONArray(String rawString) {
        try {
            Log.d(TAG, "Converting string to JSONObject");
            return new JSONArray(rawString);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }

    public JSONArray getCurrentCommittees() {
        String committeesPath = localCache.getAbsolutePath() + "/committees-current.json";

        String committeesString = readFile(committeesPath);

        return stringToJSONArray(committeesString);
    }

    public JSONObject getCommitteeMembership() {
        String membershipPath = localCache.getAbsolutePath() + "/committee-membership-current.json";

        String membershipString = readFile(membershipPath);

        return stringToJSON(membershipString);
    }

    public JSONArray getVotes(String chamber, String bioGuide) {
        File votesDir = new File(localCache, "votes/" + chamber);
        JSONArray votesArray = new JSONArray();

        if (!votesDir.exists()) {
            Log.e(TAG, "Votes directory does not exist: " + votesDir.getAbsolutePath());
            return votesArray;
        }

        File[] voteFiles = votesDir.listFiles();
        if (null == voteFiles) {
            Log.e(TAG, "getVotes(...): (null == voteFiles), exiting");
            return votesArray;
        }
        for (File voteFile: voteFiles) {
            try {
                String voteString = readFile(voteFile.getAbsolutePath());
                JSONObject vote = stringToJSON(voteString).getJSONObject("results").getJSONObject("votes").getJSONObject("vote");

                // roll_call: #, description, "", vote_type: "", date: "yyyy-MM-dd",
                // time: "HH:mm:ss"*, result: "", positions: JSONArray
                JSONObject voteInfo = new JSONObject();
                voteInfo.put("roll-call", vote.getInt("roll_call"));
                voteInfo.put("description", vote.getString("description"));
                voteInfo.put("vote-type", vote.getString("vote_type"));
                voteInfo.put("date", vote.getString("date"));
                voteInfo.put("time", vote.getString("time"));

                // Iterate over votes to find one matching bioGuide...
                JSONArray positions = vote.getJSONArray("positions");
                for (int i = 0; i < positions.length(); i++) {
                    JSONObject position = positions.getJSONObject(i);
                    if (position.getString("member_id").equals(bioGuide)) {
                        voteInfo.put("position", position.getString("vote_position"));
                    }
                }

                // If the legislator was absent...
                if (!voteInfo.has("position")) voteInfo.put("position", "absent");

                // Add vote to array
                votesArray.put(voteInfo);
            } catch (JSONException e) {
                Log.e(TAG, "getVotes(...): " + e.toString());
            }
        }

        return votesArray;
    }

    public JSONArray getSponsoredBills(String chamber, String bioGuide) {
        File billsDir = new File(localCache, "bills/" + chamber);
        JSONArray billsArray = new JSONArray();

        if (!billsDir.exists()) {
            Log.e(TAG, "Bills directory does not exist: " + billsDir.getAbsolutePath());
            return billsArray;
        }

        File[] billFiles = billsDir.listFiles();
        if (null == billFiles) {
            Log.e(TAG, "getSponsoredBills(...): (null == billFiles), exiting");
            return billsArray;
        }
        for (File billFile: billFiles) {
            try {
                String billString = readFile(billFile.getAbsolutePath());
                JSONObject bill = stringToJSON(billString).getJSONArray("results").getJSONObject(0);

                String sponsorId = bill.getString("sponsor_id");
                if (sponsorId.equals(bioGuide)) {
                    // bill: "", title: "", short_title: "", sponsor_id: "bioguide", congressdotgov_url: "",
                    // govtrack_url: "", introduced_date: "", last_major_action_date: "",
                    // last_major_action: "", summary: "probably empty"
                    JSONObject billInfo = new JSONObject();
                    billInfo.put("name", bill.getString("bill"));
                    billInfo.put("title", bill.getString("title"));
                    billInfo.put("short-title", bill.getString("short_title"));
                    billInfo.put("congress-url", bill.getString("congressdotgov_url"));
                    billInfo.put("introduced", bill.getString("introduced_date"));
                    billInfo.put("last-action-date", bill.getString("latest_major_action_date"));
                    billInfo.put("last-action", bill.getString("latest_major_action"));
                    billInfo.put("summary", bill.getString("summary"));

                    // Add bill to array
                    billsArray.put(billInfo);
                }
            } catch (JSONException e) {
                Log.e(TAG, "getSponsoredBills(...): " + e.toString());
            }
        }

        return billsArray;
    }

    public JSONObject getSocialMedia(String bioGuide) {
        Log.d(TAG, "Parsing cached social media file for " + bioGuide);
        String socialString = readFile(localCache.getAbsolutePath() + "/legislators-social-media.json");

        JSONArray socialJson = stringToJSONArray(socialString);

        try {
            for (int i = 0; i < socialJson.length(); i++) {
                JSONObject social = socialJson.getJSONObject(i);
                String socialId = social.getJSONObject("id").getString("bioguide");
                if (socialId.equals(bioGuide)) {
                    return social.getJSONObject("social");
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "getSocialMedia(...): " + e.toString());
        }

        Log.e(TAG, "Failed to get social media information for: " + bioGuide);
        return null;
    }

    public String readFile(String filePath) {
        Log.d(TAG, "Reading: " + filePath);
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
}
