package com.example.legate.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConfigManager {

    private static final String TAG = "ConfigManager";

    private File configFile;
    private JSONObject configJSON;

    public ConfigManager(Context context) {
        configFile = new File(context.getFilesDir(), "config.json");

        if (!configFile.exists()) {
            try {
                if (!configFile.createNewFile()) Log.e(TAG, "Failed to create config file");
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
        loadConfig();
    }

    public int update(String name, String value) {

        // Read config file
        loadConfig();

        try {
            // Change/Add key:value
            configJSON.put(name, value);

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return 1;
        }

        // Write new config back to file
        if (0 != writeUpdate()) {
            Log.e(TAG, "Failed to write config");
            return 1;
        }

        return 0;
    }

    private void loadConfig() {
        // Read config file
        String configString = readConfig();

        try {
            // Convert string to JSON
            if (configString.equals("")) {
                configJSON = new JSONObject();
            }
            else {
                configJSON = new JSONObject(configString);
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
    }

    private String readConfig() {
        String ret = "";

        try {
            FileInputStream fileInputStream = new FileInputStream(configFile);

            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString;
            StringBuilder stringBuilder = new StringBuilder();

            while ( (receiveString = bufferedReader.readLine()) != null ) {
                stringBuilder.append("\n").append(receiveString);
            }

            fileInputStream.close();
            ret = stringBuilder.toString();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + configFile + " - " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "Cannot read file: " + configFile + " - " + e.toString());
        }

        return ret;
    }

    private int writeUpdate() {

        try {
            String configString = configJSON.toString(4);
            FileWriter configWriter = new FileWriter(configFile.getAbsoluteFile());
            BufferedWriter configBuffer = new BufferedWriter(configWriter);
            configBuffer.write(configString);
            configBuffer.close();
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
            return 1;
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return 1;
        }

        return 0;
    }

    public String getValue(String name) {
        String value = null;

        try {
            value = configJSON.getString(name);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to get JSON object: " + e.toString());
        }

        return value;
    }
}
