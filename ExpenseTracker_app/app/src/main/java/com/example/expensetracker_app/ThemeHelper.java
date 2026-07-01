package com.example.expensetracker_app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeHelper {
    private static final String SETTINGS_PREFS = "settings";
    private static final String KEY_THEME_MODE = "theme_mode";

    private ThemeHelper() { }

    public static void applySavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
        int mode = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_YES);
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}