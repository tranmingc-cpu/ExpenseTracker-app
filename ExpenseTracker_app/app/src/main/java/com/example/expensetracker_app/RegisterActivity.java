package com.example.expensetracker_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.expensetracker_manager.model.request.RegisterRequest;
import com.expensetracker_manager.model.response.AuthResponse;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPassword, etPhoneNumber;
    private Button btnRegister;
    private TextView tvLoginLink;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        progressBar = findViewById(R.id.progressBar);

        btnRegister.setOnClickListener(v -> handleRegister());
        tvLoginLink.setOnClickListener(v -> finish());
    }

    private void handleRegister() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhoneNumber.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ họ tên, email và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }
        String passwordError = getPasswordError(password);
        if (passwordError != null) {
            etPassword.setError("Mật khẩu chưa đủ mạnh");
            etPassword.requestFocus();
            showPasswordRequirementDialog(passwordError);
            return;
        }
        showLoading(true);

        RegisterRequest request = new RegisterRequest();
        request.setFullName(fullName);
        request.setEmail(email);
        request.setPassword(password);
        request.setPhoneNumber(phone);

        RetrofitClient.getInstance().getAuthApi().register(request)
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            new androidx.appcompat.app.AlertDialog.Builder(RegisterActivity.this)
                                    .setTitle("Đăng ký thành công")
                                    .setMessage("Tài khoản đã được tạo thành công.\n\nVui lòng đăng nhập bằng email và mật khẩu vừa đăng ký.")
                                    .setPositiveButton("Đăng nhập ngay", (dialog, which) -> {
                                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                        intent.putExtra("email", email);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .setCancelable(false)
                                    .show();
                        } else {
                            String errorMessage = "Đăng ký thất bại. Vui lòng kiểm tra lại email, số điện thoại hoặc mật khẩu.";

                            try {
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();

                                    if (errorBody.contains("Email already exists")) {
                                        errorMessage = "Email này đã được đăng ký.";
                                    } else if (errorBody.toLowerCase().contains("phone")) {
                                        errorMessage = "Số điện thoại này đã được đăng ký.";
                                    } else if (errorBody.contains("Mật khẩu")) {
                                        errorMessage = "Mật khẩu chưa đạt yêu cầu bảo mật.";
                                    }
                                }
                            } catch (Exception e) {
                                errorMessage = "Đăng ký thất bại. Vui lòng thử lại.";
                            }

                            Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        showLoading(false);
                        Toast.makeText(RegisterActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
    private String getPasswordError(String password) {
        if (password == null || password.trim().isEmpty()) {
            return "Mật khẩu không được để trống.\n\n"
                    + "Mật khẩu hợp lệ cần có:\n"
                    + "• Tối thiểu 8 ký tự\n"
                    + "• Ít nhất 1 chữ hoa: A-Z\n"
                    + "• Ít nhất 1 chữ thường: a-z\n"
                    + "• Ít nhất 1 chữ số: 0-9\n"
                    + "• Ít nhất 1 ký tự đặc biệt: @ # $ % & * !\n"
                    + "• Không dùng mật khẩu phổ biến như 123456, password\n\n"
                    + "Ví dụ hợp lệ: Abc@12345";
        }

        boolean hasMinLength = password.length() >= 8;
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasNumber = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[^a-zA-Z0-9].*");

        String lower = password.toLowerCase();
        boolean isCommonPassword = lower.equals("123456")
                || lower.equals("12345678")
                || lower.equals("123456789")
                || lower.equals("password")
                || lower.equals("password123")
                || lower.equals("admin123")
                || lower.equals("qwerty123");

        if (hasMinLength && hasLowercase && hasUppercase && hasNumber && hasSpecial && !isCommonPassword) {
            return null;
        }

        StringBuilder message = new StringBuilder();
        message.append("Mật khẩu chưa đủ mạnh.\n\n");
        message.append("Mật khẩu hợp lệ cần có:\n");

        message.append(hasMinLength ? "✓ " : "✗ ");
        message.append("Tối thiểu 8 ký tự\n");

        message.append(hasUppercase ? "✓ " : "✗ ");
        message.append("Ít nhất 1 chữ hoa: A-Z\n");

        message.append(hasLowercase ? "✓ " : "✗ ");
        message.append("Ít nhất 1 chữ thường: a-z\n");

        message.append(hasNumber ? "✓ " : "✗ ");
        message.append("Ít nhất 1 chữ số: 0-9\n");

        message.append(hasSpecial ? "✓ " : "✗ ");
        message.append("Ít nhất 1 ký tự đặc biệt: @ # $ % & * !\n");

        message.append(!isCommonPassword ? "✓ " : "✗ ");
        message.append("Không dùng mật khẩu phổ biến như 123456, password\n\n");

        message.append("Ví dụ hợp lệ: Abc@12345");

        return message.toString();
    }
    private void showPasswordRequirementDialog(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Yêu cầu mật khẩu")
                .setMessage(message)
                .setPositiveButton("Đã hiểu", null)
                .show();
    }
    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }
}
