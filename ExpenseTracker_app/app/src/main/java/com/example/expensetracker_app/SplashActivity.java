package com.example.expensetracker_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.TokenManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        RetrofitClient.init(getApplicationContext());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            TokenManager tokenManager = TokenManager.getInstance(SplashActivity.this);
            String token = tokenManager.getToken();

            if (token != null && !token.isEmpty()) {
                String savedPin = tokenManager.getPasscode();

                if (savedPin != null && !savedPin.trim().isEmpty()) {
                    Intent intent = new Intent(SplashActivity.this, PinLockActivity.class);
                    intent.putExtra("check_lock", true);
                    startActivity(intent);
                } else {
                    startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                }
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }

            finish();
        }, 1500);
    }
}