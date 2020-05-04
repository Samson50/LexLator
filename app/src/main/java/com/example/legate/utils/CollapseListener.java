package com.example.legate.utils;

import android.view.View;

public class CollapseListener implements View.OnClickListener {

    private View collapsibleView;

    public CollapseListener(View view) {
        collapsibleView = view;
    }

    @Override
    public void onClick(View v) {
        if (View.VISIBLE == collapsibleView.getVisibility()) collapsibleView.setVisibility(View.GONE);
        else collapsibleView.setVisibility(View.VISIBLE);
    }
}

