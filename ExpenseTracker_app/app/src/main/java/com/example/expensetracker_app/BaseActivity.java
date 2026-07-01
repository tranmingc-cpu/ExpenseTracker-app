package com.example.expensetracker_app;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.expensetracker_manager.utils.TokenManager;
import com.example.expensetracker_app.PinLockActivity;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
    }

    private static long lastBackgroundTime = 0;
    private static final long TIMEOUT_MS = 30000; // 30 seconds

    @Override
    protected void onResume() {
        super.onResume();
        TokenManager tokenManager = TokenManager.getInstance(this);
        String passcode = tokenManager.getPasscode();

        // If app has passcode enabled and has been in background for more than TIMEOUT_MS
        if (passcode != null && !passcode.isEmpty() && !this.getClass().equals(PinLockActivity.class)) {
            if (lastBackgroundTime != 0 && (System.currentTimeMillis() - lastBackgroundTime > TIMEOUT_MS)) {
                Intent intent = new Intent(this, PinLockActivity.class);
                intent.putExtra("check_lock", true);
                startActivity(intent);
            }
        }
        lastBackgroundTime = 0;
    }

    @Override
    protected void onPause() {
        super.onPause();
        lastBackgroundTime = System.currentTimeMillis();
    }

    public String formatVND(double amount) {
        try {
            java.text.DecimalFormat formatter = (java.text.DecimalFormat) java.text.NumberFormat.getInstance(java.util.Locale.US);
            java.text.DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
            symbols.setGroupingSeparator('.');
            formatter.setDecimalFormatSymbols(symbols);
            return formatter.format((long) amount) + " đ";
        } catch (Exception e) {
            return String.valueOf(amount) + " đ";
        }
    }
}