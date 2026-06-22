package com.example.personalplanner;

import android.app.Application;

import com.example.personalplanner.utils.ThemeManager;

public class PersonalPlannerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.applySavedMode(this);
    }
}
