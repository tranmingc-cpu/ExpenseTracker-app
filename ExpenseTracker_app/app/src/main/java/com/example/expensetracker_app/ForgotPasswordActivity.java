package com.example.expensetracker_app;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnSendCode;
    private TextView tvBackToLogin;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        firebaseAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        btnSendCode = findViewById(R.id.btnSendCode);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        progressBar = findViewById(R.id.progressBar);

        btnSendCode.setOnClickListener(v -> sendFirebaseResetEmail());
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void sendFirebaseResetEmail() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập Email", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        btnSendCode.setEnabled(false);

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    btnSendCode.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                ForgotPasswordActivity.this,
                                "Đã gửi email đặt lại mật khẩu. Vui lòng kiểm tra hộp thư.",
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                    } else {
                        String errorMessage = "Gửi email đặt lại mật khẩu thất bại.";

                        if (task.getException() != null && task.getException().getMessage() != null) {
                            errorMessage += " " + task.getException().getMessage();
                        }

                        Toast.makeText(
                                ForgotPasswordActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }



    private void showLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }
}