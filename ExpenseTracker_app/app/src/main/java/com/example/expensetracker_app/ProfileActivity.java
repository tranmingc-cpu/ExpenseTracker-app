package com.example.expensetracker_app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.Drawable;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.expensetracker_manager.model.response.UserModel;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.TokenManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends BaseActivity {

    private static final String SETTINGS_PREFS = "settings";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";
    private static final int REMINDER_REQUEST_CODE = 100;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 501;

    private EditText etProfileName, etProfilePhone;
    private TextView tvProfileEmail, tvReminderTime;
    private Button btnUpdateProfile, btnChangePassword, btnSetupPin, btnRemovePin,
            btnSetReminder, btnCancelReminder, btnSignOut;

    private TokenManager tokenManager;
    private SharedPreferences settingsPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tokenManager = TokenManager.getInstance(this);
        settingsPrefs = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE);

        etProfileName = findViewById(R.id.etProfileName);
        etProfilePhone = findViewById(R.id.etProfilePhone);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvReminderTime = findViewById(R.id.tvReminderTime);

        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnSetupPin = findViewById(R.id.btnSetupPin);
        btnRemovePin = findViewById(R.id.btnRemovePin);
        btnSetReminder = findViewById(R.id.btnSetReminder);
        btnCancelReminder = findViewById(R.id.btnCancelReminder);
        btnSignOut = findViewById(R.id.btnSignOut);

        etProfileName.setText(tokenManager.getUserName());
        etProfilePhone.setText(tokenManager.getUserPhone());

        String email = tokenManager.getUserEmail();
        if (email == null || email.trim().isEmpty()) {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            email = firebaseUser != null ? firebaseUser.getEmail() : "";
        }
        tvProfileEmail.setText(email == null || email.trim().isEmpty()
                ? "Email: Chưa có thông tin"
                : "Email: " + email);

        updateReminderLabel();
        loadProfileFromApi();

        btnUpdateProfile.setOnClickListener(v -> updateProfileInfo());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnSetupPin.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, PinLockActivity.class);
            intent.putExtra("setup_mode", true);
            startActivity(intent);
        });

        btnRemovePin.setOnClickListener(v -> confirmRemovePin());

        btnSetReminder.setOnClickListener(v -> showReminderTimePicker());
        btnCancelReminder.setOnClickListener(v -> cancelDailyReminder());

        btnSignOut.setOnClickListener(v -> signOut());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePinButtons();
        updateReminderLabel();
    }

    private void updatePinButtons() {
        boolean hasPin = tokenManager.getPasscode() != null && !tokenManager.getPasscode().trim().isEmpty();
        btnSetupPin.setText(hasPin ? "Đổi mã PIN mở khóa" : "Thiết lập mã PIN mở khóa");
        btnRemovePin.setEnabled(hasPin);
        btnRemovePin.setAlpha(hasPin ? 1f : 0.45f);
    }

    private void loadProfileFromApi() {
        Long userId = tokenManager.getUserId();
        if (userId == -1L) {
            return;
        }

        RetrofitClient.getInstance().getUserApi().getUserById(userId)
                .enqueue(new Callback<UserModel>() {
                    @Override
                    public void onResponse(Call<UserModel> call, Response<UserModel> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            UserModel user = response.body();

                            String fullName = safe(user.getFullName());
                            String phone = safe(user.getPhoneNumber());
                            String email = safe(user.getEmail());

                            if (!fullName.isEmpty()) {
                                etProfileName.setText(fullName);
                            }
                            if (!phone.isEmpty()) {
                                etProfilePhone.setText(phone);
                            }
                            if (!email.isEmpty()) {
                                tvProfileEmail.setText("Email: " + email);
                            }

                            tokenManager.saveUserInfo(user.getId(), email, fullName, user.getAvatarUrl());
                            tokenManager.saveUserPhone(phone);
                        }
                    }

                    @Override
                    public void onFailure(Call<UserModel> call, Throwable t) {
                        // Giữ dữ liệu local nếu không kết nối được API.
                    }
                });
    }

    private void updateProfileInfo() {
        Long userId = tokenManager.getUserId();
        if (userId == -1L) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etProfileName.getText().toString().trim();
        String phone = etProfilePhone.getText().toString().trim();

        if (name.isEmpty()) {
            etProfileName.setError("Vui lòng nhập họ tên");
            etProfileName.requestFocus();
            return;
        }

        if (!phone.isEmpty() && !phone.matches("^[0-9+\\-\\s]{8,15}$")) {
            etProfilePhone.setError("Số điện thoại không hợp lệ");
            etProfilePhone.requestFocus();
            return;
        }

        btnUpdateProfile.setEnabled(false);

        UserModel updateRequest = new UserModel();
        updateRequest.setId(userId);
        updateRequest.setFullName(name);
        updateRequest.setPhoneNumber(phone);
        updateRequest.setEmail(tokenManager.getUserEmail());

        RetrofitClient.getInstance().getUserApi().updateUser(userId, updateRequest)
                .enqueue(new Callback<UserModel>() {
                    @Override
                    public void onResponse(Call<UserModel> call, Response<UserModel> response) {
                        btnUpdateProfile.setEnabled(true);

                        if (response.isSuccessful()) {
                            UserModel user = response.body();

                            String savedEmail = tokenManager.getUserEmail();
                            String savedName = name;
                            String savedPhone = phone;
                            String savedAvatar = tokenManager.getUserAvatar();

                            if (user != null) {
                                savedEmail = safe(user.getEmail()).isEmpty() ? savedEmail : user.getEmail();
                                savedName = safe(user.getFullName()).isEmpty() ? name : user.getFullName();
                                savedPhone = safe(user.getPhoneNumber()).isEmpty() ? phone : user.getPhoneNumber();
                                savedAvatar = user.getAvatarUrl();
                            }

                            tokenManager.saveUserInfo(userId, savedEmail, savedName, savedAvatar);
                            tokenManager.saveUserPhone(savedPhone);

                            etProfileName.setText(savedName);
                            etProfilePhone.setText(savedPhone);
                            tvProfileEmail.setText("Email: " + savedEmail);

                            Toast.makeText(ProfileActivity.this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProfileActivity.this, "Cập nhật thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<UserModel> call, Throwable t) {
                        btnUpdateProfile.setEnabled(true);
                        Toast.makeText(ProfileActivity.this, "Không thể kết nối máy chủ. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showChangePasswordDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập lại để đổi mật khẩu.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasPasswordProvider(user)) {
            Toast.makeText(this, "Tài khoản Google không dùng mật khẩu trong app. Vui lòng đăng nhập bằng Google.", Toast.LENGTH_LONG).show();
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(16);
        layout.setPadding(padding, dp(12), padding, dp(2));

        EditText etCurrentPassword = createPasswordInput("Mật khẩu hiện tại");
        EditText etNewPassword = createPasswordInput("Mật khẩu mới tối thiểu 8 ký tự");
        EditText etConfirmPassword = createPasswordInput("Nhập lại mật khẩu mới");

        setupPasswordToggle(etCurrentPassword);
        setupPasswordToggle(etNewPassword);
        setupPasswordToggle(etConfirmPassword);

        layout.addView(etCurrentPassword);
        layout.addView(etNewPassword);
        layout.addView(etConfirmPassword);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Đổi mật khẩu")
                .setView(layout)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Cập nhật", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPassword = etCurrentPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (currentPassword.isEmpty()) {
                etCurrentPassword.setError("Nhập mật khẩu hiện tại");
                return;
            }

            if (newPassword.length() < 8) {
                etNewPassword.setError("Mật khẩu mới cần tối thiểu 8 ký tự");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                etConfirmPassword.setError("Mật khẩu nhập lại không khớp");
                return;
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

            user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), currentPassword))
                    .addOnCompleteListener(reauthTask -> {
                        if (!reauthTask.isSuccessful()) {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            Toast.makeText(ProfileActivity.this, "Mật khẩu hiện tại không đúng.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        user.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(ProfileActivity.this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    } else {
                                        Toast.makeText(ProfileActivity.this, "Không thể đổi mật khẩu. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    });
        }));

        dialog.show();
    }

    private void showReminderTimePicker() {
        int hour = settingsPrefs.getInt(KEY_REMINDER_HOUR, 21);
        int minute = settingsPrefs.getInt(KEY_REMINDER_MINUTE, 0);

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> scheduleDailyReminder(selectedHour, selectedMinute),
                hour,
                minute,
                true
        );

        dialog.setTitle("Chọn giờ nhắc ghi chép");
        dialog.show();
    }

    private void scheduleDailyReminder(int hour, int minute) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST
            );
            Toast.makeText(this, "Vui lòng cấp quyền thông báo để nhận nhắc nhở.", Toast.LENGTH_LONG).show();
        }

        settingsPrefs.edit()
                .putBoolean(KEY_REMINDER_ENABLED, true)
                .putInt(KEY_REMINDER_HOUR, hour)
                .putInt(KEY_REMINDER_MINUTE, minute)
                .apply();

        ReminderReceiver.scheduleReminder(getApplicationContext(), hour, minute);

        updateReminderLabel();
        Toast.makeText(this, "Đã đặt nhắc nhở lúc " + formatTime(hour, minute) + " hằng ngày.", Toast.LENGTH_LONG).show();
    }

    private void cancelDailyReminder() {
        ReminderReceiver.cancelReminder(getApplicationContext());

        settingsPrefs.edit()
                .putBoolean(KEY_REMINDER_ENABLED, false)
                .apply();

        updateReminderLabel();
        Toast.makeText(this, "Đã tắt nhắc nhở ghi chép.", Toast.LENGTH_SHORT).show();
    }

    private void updateReminderLabel() {
        boolean enabled = settingsPrefs.getBoolean(KEY_REMINDER_ENABLED, false);
        int hour = settingsPrefs.getInt(KEY_REMINDER_HOUR, 21);
        int minute = settingsPrefs.getInt(KEY_REMINDER_MINUTE, 0);

        if (enabled) {
            tvReminderTime.setText("Đang nhắc mỗi ngày lúc " + formatTime(hour, minute));
            btnSetReminder.setText("Đổi giờ nhắc nhở");
            btnCancelReminder.setEnabled(true);
            btnCancelReminder.setAlpha(1f);
        } else {
            tvReminderTime.setText("Chưa bật nhắc nhở ghi chép");
            btnSetReminder.setText("Đặt giờ nhắc nhở");
            btnCancelReminder.setEnabled(false);
            btnCancelReminder.setAlpha(0.45f);
        }
    }

    private void confirmRemovePin() {
        if (tokenManager.getPasscode() == null || tokenManager.getPasscode().trim().isEmpty()) {
            Toast.makeText(this, "Chưa thiết lập mã PIN.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xóa mã PIN?")
                .setMessage("Sau khi xóa, app sẽ không yêu cầu nhập mã PIN khi mở lại.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa PIN", (dialog, which) -> {
                    tokenManager.savePasscode(null);
                    updatePinButtons();
                    Toast.makeText(ProfileActivity.this, "Đã xóa mã PIN bảo mật.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();

        TokenManager.getInstance(ProfileActivity.this).clear();
        int resId = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
        String webClientId = resId != 0 ? getString(resId) : "";

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();

        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Toast.makeText(ProfileActivity.this, "Đăng xuất thành công!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupPasswordToggle(EditText editText) {
        final boolean[] isVisible = {false};

        editText.setMinHeight(0);
        editText.setGravity(android.view.Gravity.CENTER_VERTICAL);
        updatePasswordToggleIcon(editText, isVisible[0]);

        editText.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableEnd = editText.getCompoundDrawables()[2];

                if (drawableEnd != null) {
                    int iconStart = editText.getWidth()
                            - editText.getPaddingRight()
                            - drawableEnd.getBounds().width()
                            - dp(12);

                    if (event.getX() >= iconStart) {
                        isVisible[0] = !isVisible[0];

                        int cursorPosition = editText.getSelectionStart();
                        if (isVisible[0]) {
                            editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        } else {
                            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        }

                        updatePasswordToggleIcon(editText, isVisible[0]);
                        editText.setSelection(Math.max(0, Math.min(cursorPosition, editText.length())));
                        event.setAction(MotionEvent.ACTION_CANCEL);
                        return true;
                    }
                }
            }
            return false;
        });
    }



    private void updatePasswordToggleIcon(EditText editText, boolean isVisible) {
        int iconRes = isVisible ? R.drawable.ic_visibility_24 : R.drawable.ic_visibility_off_24;
        Drawable icon = ContextCompat.getDrawable(this, iconRes);
        if (icon != null) {
            int iconSize = dp(20);
            icon.setBounds(0, 0, iconSize, iconSize);
        }

        editText.setCompoundDrawables(null, null, icon, null);
        editText.setCompoundDrawablePadding(dp(6));
        editText.setPadding(dp(12), 0, dp(40), 0);
    }



    private EditText createPasswordInput(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setTextColor(color(R.color.app_text_primary));
        editText.setHintTextColor(color(R.color.app_input_hint));
        editText.setTextSize(14);
        editText.setSingleLine(true);
        editText.setMinHeight(0);
        editText.setIncludeFontPadding(false);
        editText.setGravity(android.view.Gravity.CENTER_VERTICAL);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        editText.setPadding(dp(12), 0, dp(40), 0);
        editText.setBackgroundColor(color(R.color.app_input_background));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
        );
        params.setMargins(0, 0, 0, dp(8));
        editText.setLayoutParams(params);

        return editText;
    }



    private boolean hasPasswordProvider(FirebaseUser user) {
        for (com.google.firebase.auth.UserInfo info : user.getProviderData()) {
            if ("password".equals(info.getProviderId())) {
                return true;
            }
        }
        return false;
    }

    private String formatTime(int hour, int minute) {
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int color(int colorResId) {
        return ContextCompat.getColor(this, colorResId);
    }

}