package com.example.expensetracker_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.expensetracker_manager.utils.TokenManager;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchBudgetAlert;
    private Switch switchDarkMode;
    private Switch switchPinLock;

    private Button btnSync;
    private Button btnExport;
    private Button btnClearCache;
    private Button btnAbout;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        switchBudgetAlert = findViewById(R.id.switchBudgetAlert);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchPinLock = findViewById(R.id.switchPinLock);

        btnSync = findViewById(R.id.btnSync);
        btnExport = findViewById(R.id.btnExport);
        btnClearCache = findViewById(R.id.btnClearCache);
        btnAbout = findViewById(R.id.btnAbout);
        btnLogout = findViewById(R.id.btnLogout);

        SharedPreferences prefs =
                getSharedPreferences("settings", MODE_PRIVATE);

        switchBudgetAlert.setChecked(
                prefs.getBoolean("budget_alert", true));

        switchDarkMode.setChecked(
                prefs.getBoolean("dark_mode", false));

        switchPinLock.setChecked(
                prefs.getBoolean("pin_lock", false));

        switchBudgetAlert.setOnCheckedChangeListener(
                (buttonView, isChecked) ->
                        prefs.edit()
                                .putBoolean("budget_alert", isChecked)
                                .apply());

        switchPinLock.setOnCheckedChangeListener(
                (buttonView, isChecked) ->
                        prefs.edit()
                                .putBoolean("pin_lock", isChecked)
                                .apply());

        switchDarkMode.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    prefs.edit()
                            .putBoolean("dark_mode", isChecked)
                            .apply();

                    AppCompatDelegate.setDefaultNightMode(
                            isChecked
                                    ? AppCompatDelegate.MODE_NIGHT_YES
                                    : AppCompatDelegate.MODE_NIGHT_NO
                    );
                });

        btnSync.setOnClickListener(v ->
                Toast.makeText(this,
                        "Đồng bộ dữ liệu thành công",
                        Toast.LENGTH_SHORT).show());

        btnExport.setOnClickListener(v ->
                Toast.makeText(this,
                        "Vui lòng dùng nút Xuất CSV tại Dashboard",
                        Toast.LENGTH_SHORT).show());

        btnClearCache.setOnClickListener(v -> {

            deleteCacheDir(getCacheDir());

            Toast.makeText(this,
                    "Đã xóa cache",
                    Toast.LENGTH_SHORT).show();
        });

        btnAbout.setOnClickListener(v ->
                Toast.makeText(this,
                        "Expense Tracker v1.0",
                        Toast.LENGTH_LONG).show());

        btnLogout.setOnClickListener(v -> {

            FirebaseAuth.getInstance().signOut();

            TokenManager.getInstance(this).clear();

            Intent intent =
                    new Intent(
                            SettingsActivity.this,
                            LoginActivity.class);

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish();
        });
    }

    private void deleteCacheDir(java.io.File dir) {

        if (dir != null && dir.isDirectory()) {

            java.io.File[] children = dir.listFiles();

            if (children != null) {
                for (java.io.File child : children) {
                    deleteCacheDir(child);
                }
            }
        }

        if (dir != null) {
            dir.delete();
        }
    }
}
