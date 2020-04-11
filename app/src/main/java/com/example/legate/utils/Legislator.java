package com.example.legate.utils;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class Legislator {
    private static final String TAG = "Legislator";

    private CacheManager cacheManager = new CacheManager();
    private StateHelper stateHelper = new StateHelper();

    private File localCache;
    private JSONObject infoJSON = null;

    private String title;
    private String state;
    private String party;
    private String district;
    private String bioGuide;
    private String imageUrl;

    public Legislator(String legislatorPath) {
        localCache = new File(legislatorPath);
    }

    private int fillLegislatorMain() {
        if (null == infoJSON) {
            if (0 != getInfoJSON()) {
                Log.e(TAG, "Failed to get info.json");
                return 1;
            }
        }

        String[] fileSplit = localCache.getName().split("-");

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

        File parentFile = localCache.getParentFile();
        if (parentFile == null) return 1;
        String shortState = parentFile.getName();
        state = stateHelper.shortToFull(shortState);
        
        bioGuide = getBioGuide();
        if (null == bioGuide) {
            Log.e(TAG, "bioGuide was null");
            return 1;
        }
        imageUrl = String.format("https://bioguideretro.congress.gov/Static_Files/images/photos/%c/%s.jpg", bioGuide.charAt(0), bioGuide);

        return 0;
    }

    private int getInfoJSON() {
        String infoString = cacheManager.readFile(new File(localCache, "info.json").getPath());
        if (null == infoString) return 1;

        infoJSON = cacheManager.stringToJSON(infoString);
        return 0;
    }

    public String getIdValue(String key) {
        if (null == infoJSON) {
            if (0 != getInfoJSON()) {
                Log.e(TAG, "Failed to get info.json");
                return null;
            }
        }

        try {
            Log.d(TAG, "getBioGuide(): Getting bioguide from JSON format string");
            return infoJSON.getJSONObject("id").getString(key);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }

    public String getBioGuide() {
        return getIdValue("bioguide");
    }
}
