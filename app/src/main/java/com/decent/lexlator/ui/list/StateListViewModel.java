package com.decent.lexlator.ui.list;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.decent.lexlator.data.Legislator;
import com.decent.lexlator.utils.StateHelper;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class StateListViewModel extends ViewModel {

    private static final String TAG = "StateListViewModel";

    private MutableLiveData<List<Legislator>> senators;
    private MutableLiveData<List<Legislator>> representatives;
    private File statesDirectory;
    private String state;
    private String district;

    private FileFilter repFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().startsWith("R-");
        }
    };
    private FileFilter senFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().startsWith("S-");
        }
    };

    void setFilePath(File cacheFile) {
        statesDirectory = new File(cacheFile, "states");
    }

    void setState(String newState) {
        StateHelper helper = new StateHelper();
        String shortState = helper.fullToShort(newState);
        if (!shortState.equals(state)) {
            state = shortState;
            senators = null;
            representatives = null;
        }
    }

    void setDistrict(String newDistrict) {
        if (null == newDistrict) {
            if (null != district) {
                district = null;
                representatives = null;
            }
            return;
        }
        if (!newDistrict.equals(district)) {
            district = newDistrict;
            representatives = null;
        }
    }

    LiveData<List<Legislator>> getSenators() {
        if (senators == null) {
            senators = new MutableLiveData<>();
            loadSenators();
        }
        return senators;
    }

    LiveData<List<Legislator>> getRepresentatives() {
        if (representatives == null) {
            representatives = new MutableLiveData<>();
            loadRepresentatives();
        }
        return representatives;
    }

    private void loadSenators() {
        // Short state, ex: IL
        Log.d(TAG, "Loading Senators for state: " + state);
        File stateDirectory = new File(statesDirectory, state);
        Log.d(TAG, statesDirectory.getAbsolutePath());
        File[] senatorDirectories = stateDirectory.listFiles(senFilter);

        List<Legislator> senatorsList = new ArrayList<>();
        if (null != senatorDirectories) {
            for (File senatorDirectory : senatorDirectories) {
                senatorsList.add(new Legislator(senatorDirectory.getAbsolutePath()));
            }

            senators.postValue(senatorsList);
        } else Log.e(TAG, "Failed to list senators directory");
    }

    private void loadRepresentatives() {
        // Short state, ex: IL
        Log.d(TAG, "Loading Representatives for state: " + state);
        File stateDirectory = new File(statesDirectory, state);
        File[] repDirectories = stateDirectory.listFiles(repFilter);

        List<Legislator> repsList = new ArrayList<>();
        if (null != repDirectories) {
            for (File repDirectory : repDirectories) {
                if (null != district) {
                    String legislatorDistrict = repDirectory.getName().split("-")[1];
                    if (legislatorDistrict.equals(district)) {
                        repsList.add(new Legislator(repDirectory.getAbsolutePath()));
                    }
                }
                else {
                    repsList.add(new Legislator(repDirectory.getAbsolutePath()));
                }
            }

            representatives.postValue(repsList);
        } else Log.e(TAG, "Failed to list representatives directory");
    }
}
