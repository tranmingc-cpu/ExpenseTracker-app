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

        // Khởi tạo RetrofitClient với context ứng dụng
        RetrofitClient.init(getApplicationContext());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            TokenManager tokenManager = TokenManager.getInstance(SplashActivity.this);
            String token = tokenManager.getToken();

            if (token != null && !token.isEmpty()) {
                // Đã đăng nhập, chuyển sang HomeActivity
                startActivity(new Intent(SplashActivity.this, HomeActivity.class));
            } else {
                // Chưa đăng nhập, chuyển sang LoginActivity
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        }, 1500); // Trì hoãn 1.5 giây để hiển thị màn hình chào mừng
    }
}
