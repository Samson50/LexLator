package com.example.legate.utils;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;


// TODO: Fix code duplication among download tasks
public class DownloadTask extends AsyncTask<String, Integer, String> {

    private static final String TAG = "DownloadTask";

    @Override
    protected String doInBackground(String... sPaths) {
        if (sPaths.length < 2) {
            Log.e(TAG, "Implementation error: too few arguments in sPaths");
            return "";
        }
        InputStream input = null;
        OutputStream outputStream = null;
        File output;
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(sPaths[0]);
            output = new File(sPaths[1]);

            Log.d(TAG, "Establishing HTTPS connection");
            connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.connect();

            Log.d(TAG, "Headers: \n" + connection.getHeaderFields());

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                String message = "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage();
                Log.e(TAG, message);
                return message;
            }
            Log.d(TAG, "Connection established");

            if (output.exists()) {
                Date outputModified = new Date(output.lastModified());
                String urlModified = connection.getHeaderField("last-modified");
                if (null != urlModified) {
                    Log.d(TAG, "URL modified string: " + urlModified);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                    Date urlModifiedDate = dateFormat.parse(urlModified);
                    if (null != urlModifiedDate) {
                        Log.d(TAG, "URL Updated: " + urlModifiedDate.toString());
                        if (urlModifiedDate.before(outputModified)) {
                            Log.d(TAG, "Cache file up to date, exiting download");
                            return "";
                        }
                    } else Log.d(TAG, "Failed to parse URL Modified String: " + urlModified);
                } else Log.d(TAG, "Url last modified == null, continuing");
            }

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
            Log.d(TAG, "Downloading file to: " + output.getAbsolutePath());
            outputStream = new FileOutputStream(output);

            byte[] data = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                // allow canceling with back button
                if (isCancelled()) {
                    input.close();
                    return null;
                }
                total += count;

                outputStream.write(data, 0, count);
            }
            Log.d(TAG, "File length: " + fileLength);
            Log.d(TAG, "Bytes downloaded: " + total);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        } finally {
            Log.d(TAG, "Closing HTTPS connection and output stream");
            try {
                if (outputStream != null)
                    outputStream.close();
                if (input != null)
                    input.close();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }

            if (connection != null)
                connection.disconnect();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        //setProgressPercent(progress[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        //showDialog("Downloaded " + result + " bytes");
    }

}
