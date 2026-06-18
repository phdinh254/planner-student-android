package com.example.personalplanner.activity;

import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.personalplanner.R;
import com.example.personalplanner.fragment.CalendarFragment;
import com.example.personalplanner.fragment.HomeFragment;
import com.example.personalplanner.fragment.ProfileFragment;
import com.example.personalplanner.fragment.TaskFragment;
import com.example.personalplanner.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_ITEM = "selected_item";

    private BottomNavigationView bottomNavigationView;
    private int currentSelectedItemId = R.id.nav_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!new SessionManager(this).isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        requestNotificationPermission();
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else {
            currentSelectedItemId = savedInstanceState.getInt(
                    STATE_SELECTED_ITEM,
                    R.id.nav_home
            );
            bottomNavigationView.setSelectedItemId(currentSelectedItemId);
        }

        setupBottomNavigation();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    1001
            );
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_add) {
                startActivity(new Intent(this, AddTaskActivity.class));
                bottomNavigationView.post(
                        () -> bottomNavigationView.setSelectedItemId(currentSelectedItemId)
                );
                return false;
            }

            Fragment fragment;
            if (itemId == R.id.nav_task) {
                fragment = new TaskFragment();
            } else if (itemId == R.id.nav_calendar) {
                fragment = new CalendarFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            } else {
                fragment = new HomeFragment();
                itemId = R.id.nav_home;
            }

            currentSelectedItemId = itemId;
            loadFragment(fragment);
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_SELECTED_ITEM, currentSelectedItemId);
        super.onSaveInstanceState(outState);
    }
}
