package com.example.personalplanner.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeManager {
    public static final int MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES;
    public static final int MODE_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

    private static final String PREF_NAME = "APP_SETTINGS";
    private static final String KEY_THEME_MODE = "theme_mode";

    private ThemeManager() {
    }

    public static void applySavedMode(Context context) {
        AppCompatDelegate.setDefaultNightMode(getSavedMode(context));
    }

    public static int getSavedMode(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getInt(KEY_THEME_MODE, MODE_SYSTEM);
    }

    public static void saveMode(Context context, int mode) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_THEME_MODE, mode)
                .apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
