package com.decent.lexlator.ui.about;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.decent.lexlator.R;
import com.decent.lexlator.utils.CollapseListener;

public class AboutFragment extends Fragment {

    private static final String TAG = "AboutFragment";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_about, container, false);

        addCollapse(
                root.findViewById(R.id.text_data),
                root.findViewById(R.id.content_data)
        );
        addCollapse(
                root.findViewById(R.id.text_leg_info),
                root.findViewById(R.id.content_leg_info)
        );
        addCollapse(
                root.findViewById(R.id.text_bills_votes),
                root.findViewById(R.id.content_bills_votes)
        );
        addCollapse(
                root.findViewById(R.id.text_finance_info),
                root.findViewById(R.id.content_finance_info)
        );
        addCollapse(
                root.findViewById(R.id.text_other),
                root.findViewById(R.id.content_other)
        );

        return root;
    }

    private void addCollapse(View clicker, View collapse) {
        CollapseListener collapses = new CollapseListener(collapse);

        clicker.setOnClickListener(collapses);
    }
}
