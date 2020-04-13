package com.example.legate.data.finances;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.legate.R;
import com.example.legate.utils.CacheManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

// TODO: Combine download and fill function so they don't need to be subsequent, separate buttons
public class Finances {
    private static final String TAG = "Finances";
    private static final String OS_API_KEY = "ca1f46ceab269d0fe761675e39bdbd09";
    // Format(method, cid, cycle, key)
    private static final String FINANCES_URL = "https://www.opensecrets.org/api/?method=%s&cid=%s&cycle=%s&apikey=%s&output=json";
    // Format(method, cid, key)
    // private static final String FINANCES_URL = "https://www.opensecrets.org/api/?method=%s&cid=%s&apikey=%s&output=json";

    private CacheManager cacheManager;
    private File localCache;
    private File summaryFile;
    private File topContributorsFile;
    private File topIndustriesFile;

    private JSONObject financesJSON = new JSONObject();

    public Finances(CacheManager manager, File cache) {
        cacheManager = manager;
        localCache = cache;
    }

    private void updateFinances(String openSecretsId, String cycle) {
        // http://www.opensecrets.org/api/?method=candSummary&cid=N00007360&apikey=ca1f46ceab269d0fe761675e39bdbd09&output=json
        String summaryUrl = String.format(FINANCES_URL, "candSummary", openSecretsId, cycle, OS_API_KEY);
        String summaryPath = summaryFile.getAbsolutePath();

        // https://www.opensecrets.org/api/?method=candContrib&cid=N00007360&cycle=2020&apikey=ca1f46ceab269d0fe761675e39bdbd09&output=json
        String topContributorsUrl = String.format(FINANCES_URL, "candContrib", openSecretsId, cycle, OS_API_KEY);
        String topContributorsPath = topContributorsFile.getAbsolutePath();

        // https://www.opensecrets.org/api/?method=candIndustry&cid=N00007360&cycle=2020&apikey=ca1f46ceab269d0fe761675e39bdbd09&output=json
        String topIndustriesUrl = String.format(FINANCES_URL, "candIndustry", openSecretsId, cycle, OS_API_KEY);
        String topIndustriesPath = topIndustriesFile.getAbsolutePath();

        if (!summaryFile.exists()) cacheManager.downloadFile(summaryUrl, summaryPath);
        if (!topContributorsFile.exists()) cacheManager.downloadFile(topContributorsUrl, topContributorsPath);
        if (!topIndustriesFile.exists()) cacheManager.downloadFile(topIndustriesUrl, topIndustriesPath);
    }

    public void downloadFinances(String openSecretsId, String cycle) {
        if (filesExist()) return;
        updateFinances(openSecretsId, cycle);
    }

    public void fillFinances(ViewGroup summary, RecyclerView topContributors, RecyclerView topIndustries) {
        // do the files exist?
        if (filesExist()) {
            Log.d(TAG, "Files found, populating views");
            fillSummary(summary);
            fillContributors(topContributors);
            fillIndustries(topIndustries);
        }
    }

    private boolean filesExist() {
        if (null == summaryFile) summaryFile = new File(localCache, "finances-summary.json");
        if (null == topContributorsFile) topContributorsFile = new File(localCache, "finances-contributors.json");
        if (null == topIndustriesFile) topIndustriesFile = new File(localCache, "finances-industries.json");

        return (summaryFile.exists() && topContributorsFile.exists() && topIndustriesFile.exists());
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
            totalView.setText(monetaryFormat(summaryJSON.getString("total")));
            TextView spentView = summary.findViewById(R.id.amount_spent);
            spentView.setText(monetaryFormat(summaryJSON.getString("spent")));
            TextView cashView = summary.findViewById(R.id.cash_on_hand);
            cashView.setText(monetaryFormat(summaryJSON.getString("cash_on_hand")));
            TextView debtView = summary.findViewById(R.id.reported_debt);
            debtView.setText(monetaryFormat(summaryJSON.getString("debt")));

        } catch (JSONException e) {
            Log.e(TAG, "fillSummary encountered JSON error: " + e.toString());
        }
    }

    private JSONArray getArray(String name, File file, String[] depth) throws JSONException {
        Log.d(TAG, String.format("Reading file for: \"%s\"", name));
        JSONArray newArray = new JSONArray();
        JSONObject responseJSON = getJsonFromFile(file);
        if (null == responseJSON) return null;

        for (String level: depth) responseJSON = responseJSON.getJSONObject(level);

        JSONArray responseArray = responseJSON.getJSONArray(name);

        Log.d(TAG, "Iterating over response array");
        for (int i = 0; i < responseArray.length(); i++) {
            newArray.put(responseArray.getJSONObject(i).getJSONObject("@attributes"));
        }

        financesJSON.put(name, newArray);

        return newArray;
    }

    private void fillContributors(RecyclerView contributors) {
        Log.d(TAG, "Populating Top Contributors RecyclerView");

        try {
            JSONArray contributorsJSON;

            if (financesJSON.has("contributor")) {
                Log.d(TAG, "\"contributor\" value found in financesJSON");
                contributorsJSON = financesJSON.getJSONArray("contributor");
            }
            else {
                Log.d(TAG, "\"contributor\" value not found in financesJSON, reading files");

                String[] depth = {"response", "contributors"};
                contributorsJSON = getArray("contributor", topContributorsFile, depth);
                if (null == contributorsJSON) return;
            }

            String[] keys = {"org_name", "total", "indivs", "pacs"};
            ContributionListAdapter adapter = new ContributionListAdapter(contributorsJSON, keys);
            contributors.setAdapter(adapter);

        } catch (JSONException e) {
            Log.e(TAG, "fillContributors encountered JSON error: " + e.toString());
        }
    }

    private void fillIndustries(RecyclerView industries) {
        Log.d(TAG, "Populating Top Industries RecyclerView");

        try {
            JSONArray industriesJSON;

            if (financesJSON.has("industry")) {
                Log.d(TAG, "\"industry\" value found in financesJSON");
                industriesJSON = financesJSON.getJSONArray("industry");
            }
            else {
                Log.d(TAG, "\"industry\" value not found in financesJSON, reading files");

                String[] depth = {"response", "industries"};
                industriesJSON = getArray("industry", topIndustriesFile, depth);
                if (null == industriesJSON) return;
            }

            String[] keys = {"industry_name", "total", "indivs", "pacs"};
            ContributionListAdapter adapter = new ContributionListAdapter(industriesJSON, keys);
            industries.setAdapter(adapter);

        } catch (JSONException e) {
            Log.e(TAG, "fillContributors encountered JSON error: " + e.toString());
        }

    }

    private String monetaryFormat(String amount) {
        Float floatAmount = Float.parseFloat(amount);
        return String.format(Locale.US, "$%,.2f", floatAmount);
    }
}
