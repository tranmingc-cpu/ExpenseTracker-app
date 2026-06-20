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

import com.expensetracker_manager.model.response.AuthResponse;
import com.expensetracker_manager.network.RetrofitClient;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.expensetracker_manager.model.request.ForgotPasswordRequest;
import com.expensetracker_manager.model.request.ResetPasswordRequest;
import com.expensetracker_manager.model.request.VerifyResetCodeRequest;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail, etCode, etNewPassword;
    private Button btnSendCode, btnVerifyCode, btnResetPassword;
    private LinearLayout layoutResetForm;
    private TextView tvTimer, tvBackToLogin, tvPasswordRule;
    private ProgressBar progressBar;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

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

        btnSendCode.setOnClickListener(v -> sendResetCode());
        btnVerifyCode.setOnClickListener(v -> verifyResetCode());
        btnResetPassword.setOnClickListener(v -> resetPassword());
        tvBackToLogin.setOnClickListener(v -> finish());

        etNewPassword.setVisibility(View.GONE);
        btnResetPassword.setVisibility(View.GONE);
        tvPasswordRule.setVisibility(View.GONE);
    }

    private void sendResetCode() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập Email", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        RetrofitClient.getInstance().getAuthApi().forgotPassword(new ForgotPasswordRequest(email))
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(ForgotPasswordActivity.this, "Mã xác thực đã được gửi!", Toast.LENGTH_SHORT).show();
                            layoutResetForm.setVisibility(View.VISIBLE);

                            etCode.setVisibility(View.VISIBLE);
                            btnVerifyCode.setVisibility(View.VISIBLE);

                            etNewPassword.setVisibility(View.GONE);
                            btnResetPassword.setVisibility(View.GONE);

                            btnSendCode.setEnabled(false);
                            etEmail.setEnabled(false);
                            startTimer();
                        } else {
                            Toast.makeText(ForgotPasswordActivity.this, "Gửi mã thất bại. Email không tồn tại.", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        showLoading(false);
                        Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void verifyResetCode() {
        String email = etEmail.getText().toString().trim();
        String code = etCode.getText().toString().trim();

        if (code.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mã xác nhận", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        RetrofitClient.getInstance().getAuthApi()
                .verifyResetCode(new VerifyResetCodeRequest(email, code))
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        showLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(ForgotPasswordActivity.this, "Mã xác nhận đúng", Toast.LENGTH_SHORT).show();

                            etCode.setEnabled(false);
                            btnVerifyCode.setVisibility(View.GONE);

                            etNewPassword.setVisibility(View.VISIBLE);
                            tvPasswordRule.setVisibility(View.VISIBLE);
                            btnResetPassword.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(ForgotPasswordActivity.this, "Mã xác nhận không đúng hoặc đã hết hạn", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        showLoading(false);
                        Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();
        String code = etCode.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        String passwordError = getPasswordError(newPassword);
        if (passwordError != null) {
            Toast.makeText(this, passwordError, Toast.LENGTH_LONG).show();
            return;
        }

        if (code.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mã xác nhận", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        RetrofitClient.getInstance().getAuthApi().resetPassword(new ResetPasswordRequest(email, code, newPassword))
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(ForgotPasswordActivity.this, "Đổi mật khẩu thành công! Hãy đăng nhập lại.", Toast.LENGTH_LONG).show();
                            if (countDownTimer != null) {
                                countDownTimer.cancel();
                            }
                            finish();
                        } else {
                            Toast.makeText(ForgotPasswordActivity.this, "Đặt lại thất bại. Mã xác nhận không đúng hoặc đã hết hạn.", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        showLoading(false);
                        Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String getPasswordError(String password) {
        if (password == null || password.trim().isEmpty()) {
            return "Vui lòng nhập mật khẩu mới";
        }

        if (password.length() < 8) {
            return "Mật khẩu phải có ít nhất 8 ký tự";
        }

        if (!password.matches(".*[a-z].*")) {
            return "Mật khẩu phải có ít nhất 1 chữ thường";
        }

        if (!password.matches(".*[A-Z].*")) {
            return "Mật khẩu phải có ít nhất 1 chữ hoa";
        }

        if (!password.matches(".*\\d.*")) {
            return "Mật khẩu phải có ít nhất 1 số";
        }

        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            return "Mật khẩu phải có ít nhất 1 ký tự đặc biệt";
        }

        String lower = password.toLowerCase();
        if (lower.equals("12345678")
                || lower.equals("123456789")
                || lower.equals("password")
                || lower.equals("password123")
                || lower.equals("admin123")
                || lower.equals("qwerty123")) {
            return "Mật khẩu quá phổ biến, vui lòng chọn mật khẩu khác";
        }

        return null;
    }

    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // 10 minutes count down
        countDownTimer = new CountDownTimer(600000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format(Locale.getDefault(), "Thời gian còn lại: %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("Mã xác nhận đã hết hạn!");
                btnResetPassword.setEnabled(false);
            }
        }.start();
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }
}
