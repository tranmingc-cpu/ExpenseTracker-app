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
            AnalyticsFragment fragment = new AnalyticsFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("selectedMonth", getIntent().getIntExtra("selectedMonth", -1));
            bundle.putInt("selectedYear", getIntent().getIntExtra("selectedYear", -1));
            fragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }
}