package com.example.expensetracker_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.TokenManager;

import java.util.concurrent.Executors;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Executors.newSingleThreadExecutor().execute(() -> {

            RetrofitClient.init(getApplicationContext());

            //  Đọc dữ liệu Token ngầm
            TokenManager tokenManager = TokenManager.getInstance(SplashActivity.this);
            String token = tokenManager.getToken();
            String savedPin = tokenManager.getPasscode();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isFinishing() || isDestroyed()) return;

                Intent intent;
                if (token != null && !token.isEmpty()) {
                    if (savedPin != null && !savedPin.trim().isEmpty()) {
                        intent = new Intent(SplashActivity.this, PinLockActivity.class);
                        intent.putExtra("check_lock", true);
                    } else {
                        intent = new Intent(SplashActivity.this, HomeActivity.class);
                    }
                } else {
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }

                startActivity(intent);
                finish();
            }, 1500);

        });
    }
}