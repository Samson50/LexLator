package com.decent.lexlator;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_about)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.open_settings);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void deleteFiles(File directory) {
        if (directory.isDirectory()) {
            File[] children = directory.listFiles();
            for (File child: children) {
                deleteFiles(child);
            }
        }
        else directory.delete();
        directory.delete();
    }

    private void clearCache() {
        Log.d(TAG, "Deleting local files...");

        File[] children = getCacheDir().listFiles();
        for (File child: children) {
            deleteFiles(child);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        boolean clearOnExit = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("clear_data_on_exit", false);

        if (clearOnExit) clearCache();
    }
}
