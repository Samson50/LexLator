package com.example.legate.ui.legislator;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.legate.R;
import com.example.legate.utils.CacheManager;
import com.example.legate.utils.ImageTask;
import com.example.legate.utils.StateHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class LegislatorMain extends Fragment {

    private final static String TAG = "LegislatorMain";

    private StateHelper stateHelper = new StateHelper();
    private CacheManager cacheManager = new CacheManager();

    private ViewGroup legislatorLayout;
    private ImageView legislatorImage;
    private TextView legislatorTitle;
    private TextView legislatorState;
    private TextView legislatorParty;
    private TextView legislatorDistrict;

    public View onCreateView(@NonNull LayoutInflater inflater,
                        ViewGroup container, Bundle savedInstanceState) {

        Context context = getActivity();
        //configManager = new ConfigManager(context);

        //homeViewModel =
        //        ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_legislator_main, container, false);

        legislatorLayout = root.findViewById(R.id.legislator_banner);
        legislatorImage = legislatorLayout.findViewById(R.id.legislator_image);
        legislatorTitle = legislatorLayout.findViewById(R.id.legislator_title);
        legislatorState = legislatorLayout.findViewById(R.id.legislator_state);
        legislatorParty = legislatorLayout.findViewById(R.id.legislator_party);
        legislatorDistrict = legislatorLayout.findViewById(R.id.legislator_district);

        Bundle arguments = getArguments();
        String legislatorPath = null;
        if (null != arguments) legislatorPath = arguments.getString("path");
        if (null != legislatorPath) {
            if (0 != fillLegislator(new File(legislatorPath))) {
                Log.d(TAG, "Failed to fill legislator from path: " + legislatorPath);
            }
        }

        return root;
    }

    private int fillLegislator(File legislatorFile) {
        // ex: S-R-First Last, R-12-D-First Last
        String[] fileSplit = legislatorFile.getName().split("-");

        String legislatorName = fileSplit[fileSplit.length - 2] + " " + fileSplit[fileSplit.length - 1];

        if (fileSplit[0].equals("R")) {
            legislatorTitle.setText(String.format("Rep. %s", legislatorName));
            legislatorDistrict.setText(fileSplit[1]);
            legislatorParty.setText(fileSplit[2]);
        }
        else {
            legislatorTitle.setText(String.format("Sen. %s", legislatorName));
            legislatorParty.setText(fileSplit[1]);
        }

        File parentFile = legislatorFile.getParentFile();
        if (parentFile == null) return 1;
        String shortState = parentFile.getName();
        String fullState = stateHelper.shortToFull(shortState);

        legislatorState.setText(fullState);

        String bioGuide = getBioGuide(legislatorFile);
        if (null == bioGuide) {
            Log.e(TAG, "bioGuide was null");
            return 1;
        }

        String imagePath = legislatorFile.getPath();
        String imageUrl = String.format("https://bioguideretro.congress.gov/Static_Files/images/photos/%c/%s.jpg", bioGuide.charAt(0), bioGuide);
        ImageTask imageTask = new ImageTask(legislatorImage);
        imageTask.execute(imagePath,imageUrl);

        Log.d(TAG, "Legislator populated, exiting");
        return 0;
    }

    private String getBioGuide(File legislatorDirectory) {
        String bioGuide;
        String infoString;

        infoString = cacheManager.readFile(new File(legislatorDirectory, "info.json").getPath());
        if (null == infoString) return null;

        JSONObject legislatorJSON = cacheManager.stringToJSON(infoString);

        try {
            Log.d(TAG, "getBioGuide(): Getting bioguide from JSON format string");
            bioGuide = legislatorJSON.getJSONObject("id").getString("bioguide");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }

        Log.d(TAG, "getBioGuide(): result: " + bioGuide);
        return bioGuide;
    }

}
