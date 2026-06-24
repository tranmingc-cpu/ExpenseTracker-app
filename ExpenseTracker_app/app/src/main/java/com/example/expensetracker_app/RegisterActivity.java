package com.example.expensetracker_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText etFullName, etEmail, etPassword, etPhoneNumber;
    private Button btnRegister;
    private TextView tvLoginLink;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();

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

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ họ tên, email và mật khẩu.", Toast.LENGTH_SHORT).show();
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

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                        if (firebaseUser == null) {
                            showLoading(false);
                            Toast.makeText(this, "Đăng ký chưa thành công. Vui lòng thử lại sau.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        UserProfileChangeRequest profileUpdates =
                                new UserProfileChangeRequest.Builder()
                                        .setDisplayName(fullName)
                                        .build();

                        firebaseUser.updateProfile(profileUpdates)
                                .addOnCompleteListener(profileTask -> {
                                    showLoading(false);

                                    if (!profileTask.isSuccessful()) {
                                        Log.e(TAG, "Update Firebase profile failed", profileTask.getException());
                                    }

                                    showRegisterSuccessDialog(email);
                                });
                    } else {
                        showLoading(false);
                        Log.e(TAG, "Firebase register failed", task.getException());
                        Toast.makeText(
                                RegisterActivity.this,
                                getVietnameseRegisterError(task.getException()),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void showRegisterSuccessDialog(String email) {
        firebaseAuth.signOut();

        new androidx.appcompat.app.AlertDialog.Builder(RegisterActivity.this)
                .setTitle("Đăng ký thành công")
                .setMessage("Tài khoản đã được tạo thành công.\n\nVui lòng quay lại màn đăng nhập để đăng nhập bằng email và mật khẩu vừa đăng ký.")
                .setPositiveButton("Đăng nhập ngay", (dialog, which) -> {
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.putExtra("email", email);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private String getVietnameseRegisterError(Exception exception) {
        if (exception == null || exception.getMessage() == null) {
            return "Đăng ký thất bại. Vui lòng thử lại.";
        }

        String error = exception.getMessage().toLowerCase();

        if (error.contains("email address is already in use")
                || error.contains("already in use")
                || error.contains("email-already-in-use")) {
            return "Email này đã được đăng ký.";
        }

        if (error.contains("badly formatted")
                || error.contains("invalid email")) {
            return "Email không đúng định dạng.";
        }

        if (error.contains("password is invalid")
                || error.contains("weak password")
                || error.contains("password should be at least")) {
            return "Mật khẩu chưa đủ mạnh. Vui lòng nhập mật khẩu tối thiểu 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt.";
        }

        if (error.contains("network")
                || error.contains("timeout")
                || error.contains("connection")) {
            return "Lỗi mạng. Vui lòng kiểm tra kết nối Internet.";
        }

        if (error.contains("too many")
                || error.contains("blocked")) {
            return "Bạn thao tác quá nhiều lần. Vui lòng thử lại sau.";
        }

        return "Đăng ký thất bại. Vui lòng kiểm tra lại email hoặc mật khẩu.";
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