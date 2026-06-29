package com.example.expensetracker_app;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.expensetracker_manager.model.response.TransactionResponse;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.OfflineCacheManager;
import com.expensetracker_manager.utils.TokenManager;
import com.google.firebase.auth.FirebaseAuth;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {

    private static final String SETTINGS_PREFS = "settings";
    private static final String KEY_BUDGET_ALERT = "budget_alert";
    private static final String KEY_PIN_LOCK = "pin_lock";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 501;

    private SharedPreferences prefs;
    private boolean isUpdatingPinSwitch = false;

    private LinearLayout rowThemeMode;
    private LinearLayout rowLanguage;
    private LinearLayout rowReminder;
    private LinearLayout rowPinLock;
    private LinearLayout rowChangePin;

    private TextView tvThemeValue;
    private TextView tvLanguageValue;
    private TextView tvReminderValue;
    private TextView tvPinStatus;

    private Switch switchBudgetAlert;
    private Switch switchPinLock;

    private Button btnSync;
    private Button btnExport;
    private Button btnClearCache;
    private Button btnAbout;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE);
        applySavedTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        bindViews();
        loadSettingsToUi();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePinUi();
        updateReminderUi();
    }

    private void bindViews() {
        rowThemeMode = findViewById(R.id.rowThemeMode);
        rowLanguage = findViewById(R.id.rowLanguage);
        rowReminder = findViewById(R.id.rowReminder);
        rowPinLock = findViewById(R.id.rowPinLock);
        rowChangePin = findViewById(R.id.rowChangePin);

        tvThemeValue = findViewById(R.id.tvThemeValue);
        tvLanguageValue = findViewById(R.id.tvLanguageValue);
        tvReminderValue = findViewById(R.id.tvReminderValue);
        tvPinStatus = findViewById(R.id.tvPinStatus);

        switchBudgetAlert = findViewById(R.id.switchBudgetAlert);
        switchPinLock = findViewById(R.id.switchPinLock);

        btnSync = findViewById(R.id.btnSync);
        btnExport = findViewById(R.id.btnExport);
        btnClearCache = findViewById(R.id.btnClearCache);
        btnAbout = findViewById(R.id.btnAbout);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void loadSettingsToUi() {
        switchBudgetAlert.setChecked(prefs.getBoolean(KEY_BUDGET_ALERT, true));
        updateThemeUi();
        updateReminderUi();
        updatePinUi();
        tvLanguageValue.setText("Tiếng Việt");
    }

    private void setupListeners() {
        switchBudgetAlert.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_BUDGET_ALERT, isChecked).apply();
            Toast.makeText(
                    this,
                    isChecked ? "Đã bật cảnh báo ngân sách." : "Đã tắt cảnh báo ngân sách.",
                    Toast.LENGTH_SHORT
            ).show();
        });

        rowThemeMode.setOnClickListener(v -> showThemeDialog());
        rowLanguage.setOnClickListener(v -> showLanguageDialog());
        rowReminder.setOnClickListener(v -> showReminderOptions());

        rowPinLock.setOnClickListener(v -> switchPinLock.performClick());
        switchPinLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingPinSwitch) {
                return;
            }

            if (isChecked) {
                if (hasPin()) {
                    prefs.edit().putBoolean(KEY_PIN_LOCK, true).apply();
                    updatePinUi();
                    Toast.makeText(this, "Đã bật khóa ứng dụng bằng PIN.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Hãy thiết lập mã PIN để bật khóa ứng dụng.", Toast.LENGTH_SHORT).show();
                    openPinSetup();
                    updatePinUi();
                }
            } else {
                if (hasPin()) {
                    confirmDisablePin();
                } else {
                    prefs.edit().putBoolean(KEY_PIN_LOCK, false).apply();
                    updatePinUi();
                }
            }
        });

        rowChangePin.setOnClickListener(v -> openPinSetup());
        btnSync.setOnClickListener(v -> syncData());
        btnExport.setOnClickListener(v -> exportTransactionsToCSV());
        btnClearCache.setOnClickListener(v -> confirmClearCache());
        btnAbout.setOnClickListener(v -> showAboutDialog());
        btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void applySavedTheme() {
        int mode = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    private void showThemeDialog() {
        String[] options = {"Theo hệ thống", "Sáng", "Tối"};
        int currentMode = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        int checkedIndex = getThemeIndex(currentMode);

        new AlertDialog.Builder(this)
                .setTitle("Chế độ giao diện")
                .setSingleChoiceItems(options, checkedIndex, (dialog, which) -> {
                    int selectedMode;
                    if (which == 1) {
                        selectedMode = AppCompatDelegate.MODE_NIGHT_NO;
                    } else if (which == 2) {
                        selectedMode = AppCompatDelegate.MODE_NIGHT_YES;
                    } else {
                        selectedMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                    }

                    prefs.edit().putInt(KEY_THEME_MODE, selectedMode).apply();
                    updateThemeUi();
                    dialog.dismiss();
                    AppCompatDelegate.setDefaultNightMode(selectedMode);
                    getDelegate().applyDayNight();
                })
                .show();
    }

    private int getThemeIndex(int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            return 1;
        }
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            return 2;
        }
        return 0;
    }

    private void updateThemeUi() {
        int mode = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            tvThemeValue.setText("Tối");
        } else if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            tvThemeValue.setText("Sáng");
        } else {
            tvThemeValue.setText("Theo hệ thống");
        }
    }

    private void showLanguageDialog() {
        String[] options = {"Tiếng Việt", "English - sắp hỗ trợ"};
        new AlertDialog.Builder(this)
                .setTitle("Ngôn ngữ ứng dụng")
                .setSingleChoiceItems(options, 0, (dialog, which) -> {
                    if (which == 0) {
                        tvLanguageValue.setText("Tiếng Việt");
                        Toast.makeText(this, "Ứng dụng đang sử dụng Tiếng Việt.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Tiếng Anh sẽ được hỗ trợ ở phiên bản sau.", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                })
                .show();
    }

    private void showReminderOptions() {
        boolean enabled = prefs.getBoolean(KEY_REMINDER_ENABLED, false);
        String[] options = enabled
                ? new String[]{"Đặt / đổi giờ nhắc nhở", "Tắt nhắc nhở"}
                : new String[]{"Đặt giờ nhắc nhở"};

        new AlertDialog.Builder(this)
                .setTitle("Nhắc nhở ghi chép")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showReminderTimePicker();
                    } else {
                        cancelDailyReminder();
                    }
                })
                .show();
    }

    private void showReminderTimePicker() {
        int hour = prefs.getInt(KEY_REMINDER_HOUR, 21);
        int minute = prefs.getInt(KEY_REMINDER_MINUTE, 0);

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

        prefs.edit()
                .putBoolean(KEY_REMINDER_ENABLED, true)
                .putInt(KEY_REMINDER_HOUR, hour)
                .putInt(KEY_REMINDER_MINUTE, minute)
                .apply();

        ReminderReceiver.scheduleReminder(getApplicationContext(), hour, minute);

        updateReminderUi();
        Toast.makeText(this, "Đã đặt nhắc nhở lúc " + formatTime(hour, minute) + " hằng ngày.", Toast.LENGTH_LONG).show();
    }

    private void cancelDailyReminder() {
        ReminderReceiver.cancelReminder(getApplicationContext());

        prefs.edit()
                .putBoolean(KEY_REMINDER_ENABLED, false)
                .apply();

        updateReminderUi();
        Toast.makeText(this, "Đã tắt nhắc nhở ghi chép.", Toast.LENGTH_SHORT).show();
    }

    private void updateReminderUi() {
        boolean enabled = prefs.getBoolean(KEY_REMINDER_ENABLED, false);
        int hour = prefs.getInt(KEY_REMINDER_HOUR, 21);
        int minute = prefs.getInt(KEY_REMINDER_MINUTE, 0);

        if (enabled) {
            tvReminderValue.setText("Mỗi ngày lúc " + formatTime(hour, minute));
        } else {
            tvReminderValue.setText("Chưa bật");
        }
    }

    private void updatePinUi() {
        boolean hasPin = hasPin();
        boolean pinLockEnabled = prefs.getBoolean(KEY_PIN_LOCK, hasPin);

        if (!hasPin) {
            pinLockEnabled = false;
            prefs.edit().putBoolean(KEY_PIN_LOCK, false).apply();
        }

        isUpdatingPinSwitch = true;
        switchPinLock.setChecked(pinLockEnabled);
        isUpdatingPinSwitch = false;

        tvPinStatus.setText(hasPin ? "Đã thiết lập" : "Chưa thiết lập");
        rowChangePin.setAlpha(hasPin ? 1f : 0.65f);
    }

    private boolean hasPin() {
        String pin = TokenManager.getInstance(this).getPasscode();
        return pin != null && !pin.trim().isEmpty();
    }

    private void openPinSetup() {
        Intent intent = new Intent(SettingsActivity.this, PinLockActivity.class);
        intent.putExtra("setup_mode", true);
        startActivity(intent);
    }

    private void confirmDisablePin() {
        new AlertDialog.Builder(this)
                .setTitle("Tắt khóa PIN?")
                .setMessage("Sau khi tắt, app sẽ không yêu cầu nhập mã PIN khi mở lại.")
                .setNegativeButton("Hủy", (dialog, which) -> updatePinUi())
                .setPositiveButton("Tắt", (dialog, which) -> {
                    TokenManager.getInstance(this).savePasscode(null);
                    prefs.edit().putBoolean(KEY_PIN_LOCK, false).apply();
                    updatePinUi();
                    Toast.makeText(this, "Đã tắt khóa ứng dụng bằng PIN.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void syncData() {
        Long userId = TokenManager.getInstance(this).getUserId();
        if (userId == -1L) {
            Toast.makeText(this, "Không tìm thấy người dùng. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSync.setEnabled(false);
        Toast.makeText(this, "Đang đồng bộ dữ liệu...", Toast.LENGTH_SHORT).show();

        RetrofitClient.getInstance().getTransactionApi().getByUser(userId)
                .enqueue(new Callback<List<TransactionResponse>>() {
                    @Override
                    public void onResponse(Call<List<TransactionResponse>> call, Response<List<TransactionResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            OfflineCacheManager.getInstance(SettingsActivity.this).cacheTransactions(response.body());
                            syncBudgets(userId);
                        } else {
                            btnSync.setEnabled(true);
                            Toast.makeText(SettingsActivity.this, "Đồng bộ giao dịch chưa thành công.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<TransactionResponse>> call, Throwable t) {
                        btnSync.setEnabled(true);
                        Toast.makeText(SettingsActivity.this, "Không thể kết nối máy chủ để đồng bộ.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void syncBudgets(Long userId) {
        RetrofitClient.getInstance().getBudgetApi().getByUser(userId)
                .enqueue(new Callback<List<com.expensetracker_manager.model.response.BudgetResponse>>() {
                    @Override
                    public void onResponse(Call<List<com.expensetracker_manager.model.response.BudgetResponse>> call,
                                           Response<List<com.expensetracker_manager.model.response.BudgetResponse>> response) {
                        btnSync.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            OfflineCacheManager.getInstance(SettingsActivity.this).cacheBudgets(response.body());
                        }
                        Toast.makeText(SettingsActivity.this, "Đồng bộ dữ liệu thành công.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<List<com.expensetracker_manager.model.response.BudgetResponse>> call, Throwable t) {
                        btnSync.setEnabled(true);
                        Toast.makeText(SettingsActivity.this, "Đã đồng bộ giao dịch. Chưa đồng bộ được ngân sách.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void exportTransactionsToCSV() {
        Long userId = TokenManager.getInstance(this).getUserId();
        if (userId == -1L) {
            Toast.makeText(this, "Không tìm thấy người dùng. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnExport.setEnabled(false);
        Toast.makeText(this, "Đang xuất dữ liệu CSV...", Toast.LENGTH_SHORT).show();

        RetrofitClient.getInstance().getTransactionApi().getByUser(userId)
                .enqueue(new Callback<List<TransactionResponse>>() {
                    @Override
                    public void onResponse(Call<List<TransactionResponse>> call, Response<List<TransactionResponse>> response) {
                        btnExport.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            writeTransactionsCsv(response.body());
                        } else {
                            List<TransactionResponse> cached =
                                    OfflineCacheManager.getInstance(SettingsActivity.this).getCachedTransactions();
                            writeTransactionsCsv(cached);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<TransactionResponse>> call, Throwable t) {
                        btnExport.setEnabled(true);
                        List<TransactionResponse> cached =
                                OfflineCacheManager.getInstance(SettingsActivity.this).getCachedTransactions();
                        writeTransactionsCsv(cached);
                    }
                });
    }

    private void writeTransactionsCsv(List<TransactionResponse> transactions) {
        try {
            String fileName = "ExpenseTracker_Transactions_" + System.currentTimeMillis() + ".csv";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            android.net.Uri uri = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            }

            if (uri == null) {
                Toast.makeText(this, "Không thể tạo file CSV trong thư mục Tải về.", Toast.LENGTH_SHORT).show();
                return;
            }

            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream == null) {
                Toast.makeText(this, "Không thể ghi file CSV.", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder csv = new StringBuilder();
            csv.append("ID,Ngay,Mo ta,So tien,Loai,Danh muc\n");

            if (transactions != null) {
                for (TransactionResponse tr : transactions) {
                    csv.append(tr.getId()).append(",")
                            .append("\"").append(formatTransactionDate(tr.getTransactionDate())).append("\"").append(",")
                            .append("\"").append(escapeCsv(tr.getDescription())).append("\"").append(",")
                            .append(tr.getAmount()).append(",")
                            .append("\"").append(escapeCsv(tr.getType())).append("\"").append(",")
                            .append("\"").append(escapeCsv(tr.getCategoryName())).append("\"")
                            .append("\n");
                }
            }

            outputStream.write(csv.toString().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            outputStream.close();

            Toast.makeText(this, "Xuất CSV thành công. File nằm trong thư mục Tải về.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xuất CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmClearCache() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bộ nhớ đệm?")
                .setMessage("Dữ liệu tạm sẽ bị xóa. Tài khoản và dữ liệu đã lưu trên máy chủ không bị ảnh hưởng.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteCacheDir(getCacheDir());
                    Toast.makeText(this, "Đã xóa bộ nhớ đệm.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Expense Tracker")
                .setMessage("Phiên bản 1.0\nỨng dụng quản lý thu chi cá nhân, hỗ trợ ngân sách, báo cáo, phân tích AI và nhắc nhở ghi chép.")
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất?")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản hiện tại không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đăng xuất", (dialog, which) -> logout())
                .show();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        TokenManager.getInstance(this).clear();

        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void deleteCacheDir(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            java.io.File[] children = dir.listFiles();
            if (children != null) {
                for (java.io.File child : children) {
                    deleteCacheDir(child);
                }
            }
        }

        if (dir != null) {
            dir.delete();
        }
    }

    private String formatTime(int hour, int minute) {
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    private String formatTransactionDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return "";
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(rawDate);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return dateTime.format(formatter);
        } catch (Exception e) {
            return rawDate;
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"");
    }
}