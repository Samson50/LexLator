package com.example.legate.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

public class UpdateLocalCache {

    private static final String TAG = "UpdateCache";

    private DownloadTask downloadTask = new DownloadTask();
    private final Object lock = new Object();
    private Handler uiHandler;

    private int progress = 0;
    private boolean isCancelled = false;
    private Context context;
    private View view;
    private TextView progressText;
    private File localCache;

    public UpdateLocalCache(Context mainContext, View mainView) {
        Log.d(TAG, "Creating cache class instance");
        view = mainView;
        context = mainContext;
        uiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {

            }
        };

        localCache = new File(context.getCacheDir(), "cache");
        if (!localCache.exists()){
            boolean result = localCache.mkdir();
        }
    }

    public int update() {
        Log.d(TAG, "Update called");
        final String[] legislatorsRaw = new String[1];

        // Check if the file already exists -> exit
        File file = new File(context.getCacheDir(),
                context.getResources().getString(R.string.current_legislators_path)
        );
        /*
        if (file.exists()) {
            // Check for update - last-modified header
            view.findViewById(R.id.progress_overlay).setVisibility(View.GONE);
            Log.d(TAG, "File found");
            return 0;
        }
        */
        // Start update process
        Log.d(TAG, "Starting update");

        // Set progress overlay to visible and get the view for text updates
        final View overlay = view.findViewById(R.id.progress_overlay);
        overlay.setVisibility(View.VISIBLE);
        progressText = view.findViewById(R.id.progress_text);

        final String cachePath = new File(
                context.getCacheDir(),
                context.getResources().getString(R.string.current_legislators_path)
        ).getAbsolutePath();

        // Thread to read an populate cache file, start first to ensure waiting
        Thread readCacheThread = new Thread(new Runnable() {
            @Override
            public void run() {
                File cacheFile = new File(cachePath);

                synchronized(lock) {
                    try {
                        Log.d(TAG, "Waiting for download to complete");
                        lock.wait();
                        Log.d(TAG, "Wait released");
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Wait failed: " + e.toString());
                    }
                }
                Log.d(TAG, "Reading cached file");
                String legislatorsRaw = readCache(cachePath);
                Log.d(TAG, "Finished reading cached file");

                try {
                    JSONArray legislatorsJSON = new JSONArray(legislatorsRaw);
                    Log.d(TAG, "Populating legislators");
                    int result = populateLocal(legislatorsJSON);
                    Log.d(TAG, "Finished populating legislators");

                } catch (JSONException e) {
                    Log.e(TAG, "JSON Error: " + e.toString());
                    //return 1;
                }
                overlay.post(new Runnable() {
                    @Override
                    public void run() {
                        overlay.setVisibility(View.GONE);
                    }
                });
            }
        });
        readCacheThread.start();



        // Thread to download cache file
        Thread downloadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    Log.d(TAG, "Starting download");
                    downloadFile(
                            context.getResources().getString(R.string.current_legislators_url),
                            cachePath
                    );
                    Log.d(TAG, "Download complete");
                    lock.notify();
                }
            }
        });
        downloadThread.start();

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

        fileName = fileName.concat(currentTerm.getString("party").substring(0, 0) + "-");
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
            boolean success = statePath.mkdir();
        }
        // Create directory for legislator ex: S-R-First Last, R-12-D-First Last
        String legislatorDirName = getLegislatorDir(legislator, currentTerm);
        File legislatorDir = new File(statePath, legislatorDirName);
        if (!legislatorDir.exists()) {
            boolean success = legislatorDir.mkdir();
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

            progress = i * 100 / total;
            updateProgress("Populating Cache: ");
        }
        return 0;
    }

    private String downloadFile(String fileUrl, String filePath) {
        InputStream input = null;
        OutputStream output = null;
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(fileUrl);

            connection = (HttpsURLConnection) url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage()
                );
            }

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int fileLength = connection.getContentLength();

            // download the file
            input = connection.getInputStream();
            Log.d(TAG, "Downloading file to: " + filePath);
            output = new FileOutputStream(filePath);

            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                // allow canceling with back button
                if (isCancelled) {
                    input.close();
                    return null;
                }
                total += count;
                // publishing the progress....
                if (fileLength > 0) // only if total length is known
                    progress = (int) (total * 100 / fileLength);
                    updateProgress("Downloading: ");
                output.write(data, 0, count);
            }
            Log.d(TAG, "File length: " + Integer.toString(fileLength));
            Log.d(TAG, "Bytes downloaded: " + Long.toString(total));
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        } finally {
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
        return null;
    }

    private String readCache(String cacheFilePath) {
        // https://bitbucket.org/asomov/snakeyaml/wiki/Documentation#markdown-header-tutorial
        String ret = "";

        try {
            FileInputStream fileInputStream = new FileInputStream(new File(cacheFilePath));

            if ( fileInputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }

                fileInputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "Can not read file: " + e.toString());
        }

        return ret;
    }
}
