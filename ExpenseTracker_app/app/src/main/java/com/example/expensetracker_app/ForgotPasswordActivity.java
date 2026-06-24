package com.example.expensetracker_app;

import android.os.Bundle;
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

    private EditText etEmail, etCode, etNewPassword;
    private Button btnSendCode, btnVerifyCode, btnResetPassword;
    private LinearLayout layoutResetForm;
    private TextView tvTimer, tvBackToLogin, tvPasswordRule;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        firebaseAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etCode = findViewById(R.id.etCode);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnSendCode = findViewById(R.id.btnSendCode);
        btnVerifyCode = findViewById(R.id.btnVerifyCode);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        layoutResetForm = findViewById(R.id.layoutResetForm);
        tvTimer = findViewById(R.id.tvTimer);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        tvPasswordRule = findViewById(R.id.tvPasswordRule);
        progressBar = findViewById(R.id.progressBar);

        btnSendCode.setOnClickListener(v -> sendFirebaseResetEmail());
        tvBackToLogin.setOnClickListener(v -> finish());

        // Theo hướng Firebase: không cần nhập mã OTP và mật khẩu mới trong app
        if (layoutResetForm != null) layoutResetForm.setVisibility(View.GONE);
        if (etCode != null) etCode.setVisibility(View.GONE);
        if (btnVerifyCode != null) btnVerifyCode.setVisibility(View.GONE);
        if (etNewPassword != null) etNewPassword.setVisibility(View.GONE);
        if (btnResetPassword != null) btnResetPassword.setVisibility(View.GONE);
        if (tvPasswordRule != null) tvPasswordRule.setVisibility(View.GONE);
        if (tvTimer != null) tvTimer.setVisibility(View.GONE);
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