package com.example.personalplanner.activity;

import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.personalplanner.R;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.fragment.CalendarMockupFragment;
import com.example.personalplanner.fragment.HomeMockupFragment;
import com.example.personalplanner.fragment.OverviewFragment;
import com.example.personalplanner.fragment.ProfileFragment;
import com.example.personalplanner.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_ITEM = "selected_item";

    private BottomNavigationView bottomNavigationView;
    private int currentSelectedItemId = R.id.nav_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()
                || new DatabaseHelper(this).getUser(sessionManager.getUserId()) == null) {
            sessionManager.logout();
            openLoginScreen();
            return;
        }

        setContentView(R.layout.activity_main);
        requestNotificationPermission();
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        FloatingActionButton fabAddTask = findViewById(R.id.fabAddTask);
        fabAddTask.setOnClickListener(v -> startActivity(new Intent(this, AddTaskActivity.class)));

        if (savedInstanceState == null) {
            loadFragment(new HomeMockupFragment());
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else {
            currentSelectedItemId = savedInstanceState.getInt(
                    STATE_SELECTED_ITEM,
                    R.id.nav_home
            );
            bottomNavigationView.setSelectedItemId(currentSelectedItemId);
        }

        setupBottomNavigation();
        setupBackNavigation();
    }

    private void openLoginScreen() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
            Fragment fragment;
            if (itemId == R.id.nav_calendar) {
                fragment = new CalendarMockupFragment();
            } else if (itemId == R.id.nav_overview) {
                fragment = new OverviewFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            } else {
                fragment = new HomeMockupFragment();
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

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    return;
                }
                if (currentSelectedItemId != R.id.nav_home) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_SELECTED_ITEM, currentSelectedItemId);
        super.onSaveInstanceState(outState);
    }
}
