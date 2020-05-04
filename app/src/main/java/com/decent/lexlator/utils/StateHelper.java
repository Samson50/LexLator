package com.decent.lexlator.utils;

import android.util.Log;

public class StateHelper {

    private static final String TAG = "StateHelper";

    private final String[] FULL_STATES = {
            "Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut",
            "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa",
            "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts","Michigan",
            "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada",
            "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina",
            "North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island",
            "South Carolina", "South Dakota", "Tennessee", "Texas", "Utah", "Vermont", "Virginia",
            "Washington", "West Virginia", "Wisconsin", "Wyoming"
    };
    private final String[] SHORT_STATES = {
            "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "HI", "ID", "IL", "IN",
            "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV",
            "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN",
            "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"
    };

    public StateHelper() {
        Log.d(TAG, "Created StateHelper instance");
    }

    public String fullToShort(String fullState) {

        for (int i = 0; i < 50; i++) {
            if (FULL_STATES[i].equals(fullState)) {
                return SHORT_STATES[i];
            }
        }
        Log.e(TAG, "Failed fullToShort: " + fullState);
        return null;
    }

    public String shortToFull(String shortState) {

        for (int i = 0; i < 50; i++) {
            if (SHORT_STATES[i].equals(shortState)) {
                return FULL_STATES[i];
            }
        }
        Log.e(TAG, "Failed shortToFull: " + shortState);
        return null;
    }
}
