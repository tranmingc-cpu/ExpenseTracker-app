package com.example.expensetracker_app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.expensetracker_manager.model.response.UserModel;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.TokenManager;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends BaseActivity {

    private EditText etProfileName, etProfilePhone;
    private Button btnUpdateProfile, btnSetupPin, btnRemovePin, btnSetReminder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etProfileName = findViewById(R.id.etProfileName);
        etProfilePhone = findViewById(R.id.etProfilePhone);
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
        btnSetupPin = findViewById(R.id.btnSetupPin);
        btnRemovePin = findViewById(R.id.btnRemovePin);
        btnSetReminder = findViewById(R.id.btnSetReminder);

        TokenManager tokenManager = TokenManager.getInstance(this);
        etProfileName.setText(tokenManager.getUserName());

        loadProfileFromApi();

        btnUpdateProfile.setOnClickListener(v -> updateProfileInfo());
        btnSetupPin.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, PinLockActivity.class);
            intent.putExtra("setup_mode", true);
            startActivity(intent);
        });

        btnRemovePin.setOnClickListener(v -> {
            tokenManager.savePasscode(null);
            Toast.makeText(ProfileActivity.this, "Đã xóa mã PIN bảo mật thành công!", Toast.LENGTH_SHORT).show();
        });

        btnSetReminder.setOnClickListener(v -> setDailyReminder());

        Button btnSignOut = findViewById(R.id.btnSignOut);
        btnSignOut.setOnClickListener(v -> {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
            TokenManager.getInstance(ProfileActivity.this).clear();
            Toast.makeText(ProfileActivity.this, "Đăng xuất thành công!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadProfileFromApi() {
        Long userId = TokenManager.getInstance(this).getUserId();
        if (userId == -1L) return;

        RetrofitClient.getInstance().getUserApi().getUserById(userId)
                .enqueue(new Callback<UserModel>() {
                    @Override
                    public void onResponse(Call<UserModel> call, Response<UserModel> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            UserModel user = response.body();
                            etProfileName.setText(user.getFullName());
                            etProfilePhone.setText(user.getPhoneNumber());
                        }
                    }

                    @Override
                    public void onFailure(Call<UserModel> call, Throwable t) {
                        // Silent failure / offline fallback
                    }
                });
    }

    private void updateProfileInfo() {
        Long userId = TokenManager.getInstance(this).getUserId();
        if (userId == -1L) return;

        String name = etProfileName.getText().toString().trim();
        String phone = etProfilePhone.getText().toString().trim();

        UserModel updateRequest = new UserModel();
        updateRequest.setId(userId);
        updateRequest.setFullName(name);
        updateRequest.setPhoneNumber(phone);
        updateRequest.setEmail(TokenManager.getInstance(this).getUserEmail());

        RetrofitClient.getInstance().getUserApi().updateUser(userId, updateRequest)
                .enqueue(new Callback<UserModel>() {
                    @Override
                    public void onResponse(Call<UserModel> call, Response<UserModel> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            UserModel user = response.body();
                            TokenManager.getInstance(ProfileActivity.this).saveUserInfo(
                                    user.getId(),
                                    user.getEmail(),
                                    user.getFullName(),
                                    user.getAvatarUrl()
                            );
                            Toast.makeText(ProfileActivity.this, "Cập nhật profile thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProfileActivity.this, "Cập nhật thất bại.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<UserModel> call, Throwable t) {
                        Toast.makeText(ProfileActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setDailyReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 21); // 9:00 PM
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1);
        }

        if (alarmManager != null) {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent
            );
            Toast.makeText(this, "Đã thiết lập nhắc nhở ghi chép lúc 21h00 hàng ngày!", Toast.LENGTH_LONG).show();
        }
    }
}
