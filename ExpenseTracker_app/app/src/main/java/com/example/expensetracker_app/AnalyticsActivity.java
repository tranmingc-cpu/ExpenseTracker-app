package com.example.expensetracker_app;

import android.os.Bundle;
import android.widget.ImageView;

public class AnalyticsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        // ImageView btnBack = findViewById(R.id.btnBack);
        // btnBack.setOnClickListener(v -> finish());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AnalyticsFragment())
                    .commit();
        }
    }
}