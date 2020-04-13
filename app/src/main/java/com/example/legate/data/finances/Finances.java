package com.example.legate.data.finances;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.legate.R;
import com.example.legate.utils.CacheManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class Finances {
    private static final String TAG = "Finances";
    private static final String OS_API_KEY = "ca1f46ceab269d0fe761675e39bdbd09";
    // Format(method, cid, cycle, key)
    private static final String FINANCES_URL = "https://www.opensecrets.org/api/?method=%s&cid=%s&cycle=%s&apikey=%s&output=json";
    // Format(method, cid, key)
    // private static final String FINANCES_URL = "https://www.opensecrets.org/api/?method=%s&cid=%s&apikey=%s&output=json";

    private CacheManager cacheManager;
    private File summaryFile;
    private File topContributorsFile;
    private File topIndustriesFile;

    private JSONObject financesJSON = new JSONObject();

    public Finances(CacheManager manager) {
        cacheManager = manager;
    }

    public void downloadFinances(File localCache, String openSecretsId, String cycle) {
        // http://www.opensecrets.org/api/?method=candSummary&cid=N00007360&apikey=ca1f46ceab269d0fe761675e39bdbd09&output=json
        String summaryUrl = String.format(FINANCES_URL, "candSummary", openSecretsId, cycle, OS_API_KEY);
        if (null == summaryFile) summaryFile = new File(localCache, "finances-summary.json");
        String summaryPath = summaryFile.getAbsolutePath();

        // https://www.opensecrets.org/api/?method=candContrib&cid=N00007360&cycle=2020&apikey=ca1f46ceab269d0fe761675e39bdbd09&output=json
        String topContributorsUrl = String.format(FINANCES_URL, "candContrib", openSecretsId, cycle, OS_API_KEY);
        if (null == topContributorsFile) topContributorsFile = new File(localCache, "finances-contributors.json");
        String topContributorsPath = topContributorsFile.getAbsolutePath();

        // https://www.opensecrets.org/api/?method=candIndustry&cid=N00007360&cycle=2020&apikey=ca1f46ceab269d0fe761675e39bdbd09&output=json
        String topIndustriesUrl = String.format(FINANCES_URL, "candIndustry", openSecretsId, cycle, OS_API_KEY);
        if (null == topIndustriesFile) topIndustriesFile = new File(localCache, "finances-industries.json");
        String topIndustriesPath = topIndustriesFile.getAbsolutePath();

        cacheManager.downloadFile(summaryUrl, summaryPath);
        cacheManager.downloadFile(topContributorsUrl, topContributorsPath);
        cacheManager.downloadFile(topIndustriesUrl, topIndustriesPath);
    }

    public void fillFinances(ViewGroup summary, RecyclerView topContributors, RecyclerView topIndustries) {
        // do the files exist?
        if (null == summaryFile || null == topIndustriesFile || null == topContributorsFile) {
            Log.d(TAG, "Files not downloaded, exiting");
            return;
        }
        if (!summaryFile.exists() || !topIndustriesFile.exists() || !topContributorsFile.exists()){
            Log.d(TAG, "Files do not exist, exiting");
            return;
        }

        Log.d(TAG, "Files found, populating views");
        fillSummary(summary);
        fillContributors(topContributors);
        fillIndustries(topIndustries);
    }

    private JSONObject getJsonFromFile(File file) {
        String string = cacheManager.readFile(file.getAbsolutePath());

        if (null == string) {
            Log.e(TAG, "Failed to read file, exiting: " + file.getAbsolutePath());
            return null;
        }

        return cacheManager.stringToJSON(string);
    }

    private JSONObject getValues(String name, File file, String[] depth, String[] values) throws JSONException {
        Log.d(TAG, String.format("\"%s\" value not found in financesJSON, reading files", name));
        JSONObject newJSON = new JSONObject();
        JSONObject responseJSON = getJsonFromFile(file);

        if (null == responseJSON) {
            Log.e(TAG, "Failed to get JSON, exiting: " + file.getAbsolutePath());
            return null;
        }

        for (String level: depth) responseJSON = responseJSON.getJSONObject(level);

        for (String value: values) newJSON.put(value, responseJSON.getString(value));

        financesJSON.put(name, newJSON);

        return newJSON;
    }

    private void fillSummary(ViewGroup summary) {
        Log.d(TAG, "Populating Summary View");

        try {
            JSONObject summaryJSON;

            if (financesJSON.has("summary")) {
                Log.d(TAG, "\"summary\" value found in financesJSON");
                summaryJSON = financesJSON.getJSONObject("summary");
            }
            else {
                String[] depth = {"response", "summary", "@attributes"};
                String[] values = {"total", "spent", "cash_on_hand", "debt"};
                summaryJSON = getValues("summary", summaryFile, depth, values);
                if (null == summaryJSON) return;
            }

            TextView totalView = summary.findViewById(R.id.total_contributions);
            totalView.setText(summaryJSON.getString("total"));
            TextView spentView = summary.findViewById(R.id.amount_spent);
            spentView.setText(summaryJSON.getString("spent"));
            TextView cashView = summary.findViewById(R.id.cash_on_hand);
            cashView.setText(summaryJSON.getString("cash_on_hand"));
            TextView debtView = summary.findViewById(R.id.reported_debt);
            debtView.setText(summaryJSON.getString("debt"));

        } catch (JSONException e) {
            Log.e(TAG, "fillSummary encountered JSON error: " + e.toString());
        }
    }

    private void fillContributors(RecyclerView contributors) {
        Log.d(TAG, "Populating Top Contributors RecyclerView");

    }

    private void fillIndustries(RecyclerView industries) {
        Log.d(TAG, "Populating Top Industries RecyclerView");

    }
}
