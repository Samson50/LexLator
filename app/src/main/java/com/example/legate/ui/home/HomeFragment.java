package com.example.legate.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.example.legate.R;
import com.example.legate.utils.CacheManager;
import com.example.legate.utils.StateHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;


public class HomeFragment extends Fragment implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    private static final String TAG = "HomeFragment";
    private static final String CIVIC_API = "https://content.googleapis.com/civicinfo/v2/representatives?";
    private static final String CIVIC_KEY = "AIzaSyBeG0teS-ls3puepwzK89RmuLI_YVRLURQ";

    private Context context;
    private ViewGroup contentLayout;
    private ViewGroup progressOverlay;
    private ViewGroup actionLayout;
    private Spinner districtSpinner;
    private ArrayList<String> districtList = new ArrayList<>();
    private String district;
    private String districtPreference;
    private Button goStateButton;
    private String state;
    private EditText address;
    private EditText city;
    private EditText zipCode;


    private static boolean updated = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        context = getActivity();
        assert context != null;

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        progressOverlay = root.findViewById(R.id.progress_overlay);
        contentLayout = root.findViewById(R.id.home_content_layout);

        // Initialize State selection drop-down menu
        Spinner stateSpinner = root.findViewById(R.id.stateSpinner);
        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(context,
                R.array.states_array, android.R.layout.simple_spinner_item);
        stateSpinner.setAdapter(stateAdapter);
        stateSpinner.setOnItemSelectedListener(this);

        // Hide district & address information
        actionLayout = root.findViewById(R.id.spinner_hidden);
        actionLayout.setVisibility(View.GONE);

        // Initialize District selection spinner
        districtSpinner = root.findViewById(R.id.district_spinner);

        // Initialize goStateButton
        goStateButton = root.findViewById(R.id.goStateButton);
        goStateButton.setVisibility(View.GONE);
        goStateButton.setOnClickListener(this);

        // Initialize goDistrictButton
        Button districtButton = root.findViewById(R.id.district_button);
        districtButton.setOnClickListener(new DistrictButton());

        // Initialize address views
        address = root.findViewById(R.id.edit_address);
        city = root.findViewById(R.id.edit_city);
        zipCode = root.findViewById(R.id.edit_zip);

        // Populate values from preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        state = preferences.getString("state_preference", null);
        if (null != state) {
            int index = stateAdapter.getPosition(state);
            stateSpinner.setSelection(index);
        }
        String addressValue = preferences.getString("address_preference", null);
        if (null != addressValue && addressValue.length() != 0) address.setText(addressValue);
        String cityValue = preferences.getString("city_preference", null);
        if (null != cityValue && cityValue.length() != 0) city.setText(cityValue);
        String zipValue = preferences.getString("zip_preference", null);
        if (null != zipValue && zipValue.length() != 0) zipCode.setText(zipValue);
        districtPreference = preferences.getString("district_preference", null);


        // Update Local Cache
        if (!updated) {
            updated = true;
            CacheManager cacheManager = new CacheManager();
            if (0 != cacheManager.updateLocalCache(context, progressOverlay, contentLayout))
                Log.e(TAG, "Failed to update cache.");
        }
        return root;
    }

    private void listDistricts() {
        StateHelper helper = new StateHelper();
        String shortState = helper.fullToShort(state);
        File stateFile = new File(context.getCacheDir(), "states/" + shortState);
        File[] legislators = stateFile.listFiles();
        if (null == legislators) {
            Log.e(TAG, "listDistricts(): Failed to list legislators");
            return;
        }

        districtList.clear();
        for (File legislator: legislators) {
            if (legislator.getName().startsWith("R-")) {
                String district = legislator.getName().split("-")[1];
                districtList.add(district);
            }
        }

        Collections.sort(districtList, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return Integer.parseInt(o1) - Integer.parseInt(o2);
            }
        });

        districtList.add(0, "?");

        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, districtList);
        districtSpinner.setAdapter(districtAdapter);
        districtSpinner.setOnItemSelectedListener(new DistrictSpinnerListener());

        if (null != districtPreference && districtPreference.length() != 0) {
            int index = districtList.indexOf(districtPreference);
            if (-1 != index) districtSpinner.setSelection(districtList.indexOf(districtPreference));
        }
    }

    private class DistrictSpinnerListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            district = parent.getItemAtPosition(position).toString();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            district = null;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position != 0) {
            state = parent.getItemAtPosition(position).toString();
            listDistricts();
            goStateButton.setVisibility(View.VISIBLE);
            actionLayout.setVisibility(View.VISIBLE);
        }
        else {
            state = null;
            districtList.clear();
            goStateButton.setVisibility(View.GONE);
            actionLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        state = null;
        districtList.clear();
        goStateButton.setVisibility(View.GONE);
        actionLayout.setVisibility(View.GONE);
    }

    private String numberFlavor(String district) {
        int districtLength = district.length();
        if (2 == districtLength && district.substring(0, 1).equals("1")) return "th";
        switch (district.substring(districtLength - 1)) {
            case "1": return "st";
            case "2": return "nd";
            case "3": return "rd";
            default: return "th";
        }
    }

    @Override
    public void onClick(View v) {
        Bundle bundle = new Bundle();
        if (null != district && !district.equals("?")) {
            String districtName = String.format(
                    "%s's %s%s Congressional District",
                    state,
                    district,
                    numberFlavor(district)
            );
            // Navigate
            bundle.putString("district", district);
            bundle.putString("district-name", districtName);
        }
        bundle.putString("state", state);
        Navigation.findNavController(v).navigate(R.id.nav_state_list, bundle);
    }

    private boolean addressValid() {
        if (0 == address.getText().toString().length()) return false;
        if (0 == city.getText().toString().length()) return false;
        return 0 != zipCode.getText().toString().length();
    }

    private class DistrictButton implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            // https://developers.google.com/civic-information/docs/v2
            // https://developers.google.com/civic-information/
            if (null == district || district.length() == 0) {

                if (addressValid()) {

                    String queryString = CIVIC_API;
                    String addressString = String.format(
                            "%s, %s, %s, %s",
                            address.getText().toString(),
                            city.getText().toString(),
                            state,
                            zipCode.getText().toString()
                    );
                    queryString += "address=" + addressString;
                    queryString += "&includeOffices=false";
                    queryString += "&levels=country";
                    queryString += "&key=" + CIVIC_KEY;

                    Log.d(TAG, queryString);

                    QueryTask queryTask = new QueryTask();
                    queryTask.execute(queryString);
                }
                else {
                    DialogFragment alert = new AlertFragment(
                            "Street address and city required to find district."
                    );
                    alert.show(getFragmentManager(), "address-failure");
                }
            }
            else {
                String districtName = String.format(
                        "%s's %s%s Congressional District",
                        state,
                        district,
                        numberFlavor(district)
                );
                // Navigate
                Bundle bundle = new Bundle();
                bundle.putString("district", district);
                bundle.putString("district-name", districtName);
                bundle.putString("state", state);
                Navigation.findNavController(contentLayout).navigate(R.id.nav_state_list, bundle);
            }
        }
    }

    private class QueryTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            contentLayout.setVisibility(View.GONE);
            progressOverlay.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... strings) {
            if (strings.length < 1) {
                Log.e(TAG, "Implementation error: too few arguments in sPaths");
                return "";
            }
            InputStream input = null;
            HttpsURLConnection connection = null;
            String response = null;
            try {
                URL url = new URL(strings[0]);

                Log.d(TAG, "Establishing HTTPS connection");
                connection = (HttpsURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                    String message = "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                    Log.e(TAG, message);
                    return message;
                }
                Log.d(TAG, "Connection established");

                // download the file
                input = connection.getInputStream();
                BufferedReader r = new BufferedReader(new InputStreamReader(input));
                StringBuilder total = new StringBuilder();

                // Read the string form the input stream
                for (String line; (line = r.readLine()) != null; ) {
                    total.append(line).append('\n');
                }
                response = total.toString();
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            } finally {
                Log.d(TAG, "Closing HTTPS connection");
                try {
                    if (input != null)
                        input.close();
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }

                if (connection != null)
                    connection.disconnect();
            }
            Log.d(TAG, "Query response: " + response);
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            // Get district from response
            CacheManager manager = new CacheManager();
            String districtName = null;
            String districtString = null;
            try {
                JSONObject responseJson = manager.stringToJSON(response).getJSONObject("divisions");
                Iterator<String> keys = responseJson.keys();
                String longest = "";
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (key.length() > longest.length()) longest = key;
                }

                String[] split = longest.split(":");
                districtString = split[split.length - 1];
                districtName = responseJson.getJSONObject(longest).getString("name");
            } catch (JSONException e) {
                Log.e(TAG, "Failed to get district name: " + e.toString());
            }

            // Remove overlay
            progressOverlay.setVisibility(View.GONE);
            contentLayout.setVisibility(View.VISIBLE);

            Log.d(TAG, "District string: " + districtString);
            Log.d(TAG, "District name: " + districtName);

            if (state.equals(districtName)) {
                DialogFragment alert = new AlertFragment(
                        "We could not find your district with the address provided."
                );
                alert.show(getFragmentManager(), "address-failure");
                return;
            }

            // Navigate
            Bundle bundle = new Bundle();
            bundle.putString("district", districtString);
            bundle.putString("district-name", districtName);
            bundle.putString("state", state);
            Navigation.findNavController(contentLayout).navigate(R.id.nav_state_list, bundle);
        }
    }
}
