package com.example.legate.utils;

import android.content.Context;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;


// TODO: Fix code duplication among download tasks
// TODO: Refactor to use localCache when created, only sub-directories (utility class)
public class CacheManager {

    private static final String TAG = "CacheManager";

    private boolean isCancelled = false;
    private static File localCache;
    private List<DownloadTask> downloadTasks = new ArrayList<>();


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
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(fileUrl, filePath);
        downloadTasks.add(downloadTask);
        /*
        File downloadFile = new File(filePath);
        Date lastModified;

        if (!downloadFile.exists()) lastModified = null;
        else lastModified = new Date(downloadFile.lastModified());

        return downloadFile(fileUrl, filePath, lastModified);
        */
    }

    int downloadFile(String fileUrl, String filePath, Date cacheLastModified) {
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
                if (isCancelled) {
                    input.close();
                    return 1;
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

    public void cancel() {
        isCancelled = true;
    }
}
