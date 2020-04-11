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
import com.example.legate.utils.Legislator;
import com.example.legate.utils.StateHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class LegislatorMain extends Fragment {

    private final static String TAG = "LegislatorMain";

    private StateHelper stateHelper = new StateHelper();
    private CacheManager cacheManager = new CacheManager();
    private Legislator legislator;

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
        ViewGroup districtLayout = legislatorLayout.findViewById(R.id.district_layout);

        Bundle arguments = getArguments();
        String legislatorPath = null;
        if (null != arguments) legislatorPath = arguments.getString("path");
        if (null != legislatorPath) {
            legislator = new Legislator(legislatorPath);
            legislator.fillLegislatorMain(legislatorImage, legislatorTitle, legislatorParty,
                    legislatorState, legislatorDistrict, districtLayout);
        }

        return root;
    }
}
