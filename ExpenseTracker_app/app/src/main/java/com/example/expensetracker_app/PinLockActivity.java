package com.example.expensetracker_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.expensetracker_manager.utils.TokenManager;

public class PinLockActivity extends AppCompatActivity {

    private TextView tvPinTitle;
    private TextView tvPinStatus;
    private View dot1, dot2, dot3, dot4;

    private String pinInput = "";
    private boolean isSetupMode = false;
    private boolean isCheckingLock = false;
    private String firstPin = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_lock);

        tvPinTitle = findViewById(R.id.tvPinTitle);
        tvPinStatus = findViewById(R.id.tvPinStatus);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
        dot4 = findViewById(R.id.dot4);

        isSetupMode = getIntent().getBooleanExtra("setup_mode", false);
        isCheckingLock = getIntent().getBooleanExtra("check_lock", false);

        if (isSetupMode) {
            tvPinTitle.setText("Thiết lập mã PIN mới");
            tvPinStatus.setText("Nhập 4 số để bảo vệ ứng dụng");
        } else {
            tvPinTitle.setText("Nhập mã PIN của bạn");
            tvPinStatus.setText("Nhập mã PIN để tiếp tục");
        }

        setupKeyboard();
    }

    private void setupKeyboard() {
        int[] buttonIds = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };

        for (int id : buttonIds) {
            Button btn = findViewById(id);
            btn.setOnClickListener(v -> appendPin(btn.getText().toString()));
        }

        findViewById(R.id.btnDel).setOnClickListener(v -> deletePin());
        findViewById(R.id.btnCancel).setOnClickListener(v -> {
            if (isCheckingLock) {
                finishAffinity();
            } else {
                finish();
            }
        });
    }

    private void appendPin(String num) {
        if (pinInput.length() < 4) {
            pinInput += num;
            updateDots();

            if (pinInput.length() == 4) {
                handlePinComplete();
            }
        }
    }

    private void deletePin() {
        if (pinInput.length() > 0) {
            pinInput = pinInput.substring(0, pinInput.length() - 1);
            updateDots();
        }
    }

    private void updateDots() {
        int len = pinInput.length();
        int activeColor = ContextCompat.getColor(this, R.color.app_accent_income);
        int inactiveColor = ContextCompat.getColor(this, R.color.app_divider);

        dot1.setBackgroundTintList(android.content.res.ColorStateList.valueOf(len >= 1 ? activeColor : inactiveColor));
        dot2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(len >= 2 ? activeColor : inactiveColor));
        dot3.setBackgroundTintList(android.content.res.ColorStateList.valueOf(len >= 3 ? activeColor : inactiveColor));
        dot4.setBackgroundTintList(android.content.res.ColorStateList.valueOf(len >= 4 ? activeColor : inactiveColor));
    }

    private void handlePinComplete() {
        TokenManager tokenManager = TokenManager.getInstance(this);

        if (isSetupMode) {
            if (firstPin.isEmpty()) {
                firstPin = pinInput;
                pinInput = "";
                updateDots();
                tvPinTitle.setText("Xác nhận mã PIN");
                tvPinStatus.setText("Nhập lại mã PIN 4 số vừa đặt");
            } else {
                if (pinInput.equals(firstPin)) {
                    tokenManager.savePasscode(pinInput);
                    Toast.makeText(this, "Thiết lập mã PIN thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "Mã PIN không khớp. Hãy thử lại!", Toast.LENGTH_SHORT).show();
                    pinInput = "";
                    firstPin = "";
                    updateDots();
                    tvPinTitle.setText("Thiết lập mã PIN mới");
                    tvPinStatus.setText("Nhập 4 số để bảo vệ ứng dụng");
                }
            }
        } else {
            String savedPin = tokenManager.getPasscode();

            if (savedPin != null && pinInput.equals(savedPin)) {
                Toast.makeText(this, "Mở khóa thành công!", Toast.LENGTH_SHORT).show();

                if (isCheckingLock) {
                    Intent intent = new Intent(PinLockActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

                finish();
            } else {
                Toast.makeText(this, "Sai mã PIN. Hãy thử lại!", Toast.LENGTH_SHORT).show();
                pinInput = "";
                updateDots();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isCheckingLock) {
            finishAffinity();
        } else {
            super.onBackPressed();
        }
    }
}