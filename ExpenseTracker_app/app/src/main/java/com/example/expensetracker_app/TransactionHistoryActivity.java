package com.example.expensetracker_app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.expensetracker_manager.model.response.TransactionResponse;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.TokenManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransactionHistoryActivity extends BaseActivity {

    private Button btnBackHistory, btnHistoryMonthFilter;
    private TextView tvHistoryTitle, tvHistoryIncome, tvHistoryExpense;
    private LinearLayout layoutHistoryContainer;

    private int selectedYear;
    private int selectedMonth;

    private List<TransactionResponse> allTransactions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        btnBackHistory = findViewById(R.id.btnBackHistory);
        btnHistoryMonthFilter = findViewById(R.id.btnHistoryMonthFilter);
        tvHistoryTitle = findViewById(R.id.tvHistoryTitle);
        tvHistoryIncome = findViewById(R.id.tvHistoryIncome);
        tvHistoryExpense = findViewById(R.id.tvHistoryExpense);
        layoutHistoryContainer = findViewById(R.id.layoutHistoryContainer);

        Calendar now = Calendar.getInstance();
        selectedYear = getIntent().getIntExtra("selectedYear", now.get(Calendar.YEAR));
        selectedMonth = getIntent().getIntExtra("selectedMonth", now.get(Calendar.MONTH) + 1);

        btnBackHistory.setOnClickListener(v -> finish());
        btnHistoryMonthFilter.setOnClickListener(v -> showMonthFilterDialog());

        updateMonthText();
        loadTransactions();
    }

    private void updateMonthText() {
        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);
        int currentMonth = now.get(Calendar.MONTH) + 1;

        if (selectedYear == currentYear && selectedMonth == currentMonth) {
            btnHistoryMonthFilter.setText("Tháng hiện tại");
        } else {
            btnHistoryMonthFilter.setText(String.format(Locale.US, "Tháng %02d/%d", selectedMonth, selectedYear));
        }

        tvHistoryTitle.setText(String.format(Locale.US, "Giao dịch %02d/%d", selectedMonth, selectedYear));
    }

    private void showMonthFilterDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(40, 20, 40, 20);

        NumberPicker monthPicker = new NumberPicker(this);
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(selectedMonth);
        monthPicker.setDisplayedValues(new String[]{
                "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4",
                "Tháng 5", "Tháng 6", "Tháng 7", "Tháng 8",
                "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
        });

        NumberPicker yearPicker = new NumberPicker(this);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(2020);
        yearPicker.setMaxValue(currentYear + 1);
        yearPicker.setValue(selectedYear);

        layout.addView(monthPicker, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        layout.addView(yearPicker, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn tháng")
                .setView(layout)
                .setPositiveButton("Áp dụng", (dialog, which) -> {
                    selectedMonth = monthPicker.getValue();
                    selectedYear = yearPicker.getValue();
                    updateMonthText();
                    renderTransactions();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void loadTransactions() {
        Long userId = TokenManager.getInstance(this).getUserId();

        if (userId == -1L) {
            Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        RetrofitClient.getInstance().getTransactionApi().getByUser(userId)
                .enqueue(new Callback<List<TransactionResponse>>() {
                    @Override
                    public void onResponse(Call<List<TransactionResponse>> call, Response<List<TransactionResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            allTransactions = response.body();
                            renderTransactions();
                        } else {
                            Toast.makeText(TransactionHistoryActivity.this, "Không thể tải giao dịch", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<TransactionResponse>> call, Throwable t) {
                        Toast.makeText(TransactionHistoryActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void renderTransactions() {
        layoutHistoryContainer.removeAllViews();

        List<TransactionResponse> filtered = filterBySelectedMonth(allTransactions);

        double income = 0;
        double expense = 0;

        for (TransactionResponse tr : filtered) {
            if ("INCOME".equalsIgnoreCase(tr.getType())) {
                income += tr.getAmount();
            } else {
                expense += tr.getAmount();
            }
        }

        tvHistoryIncome.setText(formatVND(income));
        tvHistoryExpense.setText(formatVND(expense));

        if (filtered.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Không có giao dịch trong tháng này");
            empty.setTextColor(0xFFAAAAAA);
            empty.setTextSize(14);
            empty.setPadding(12, 24, 12, 24);
            layoutHistoryContainer.addView(empty);
            return;
        }

        for (TransactionResponse tr : filtered) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setPadding(12, 12, 12, 12);
            item.setBackgroundColor(0xFF1F1F35);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 8);
            item.setLayoutParams(params);

            LinearLayout leftBox = new LinearLayout(this);
            leftBox.setOrientation(LinearLayout.VERTICAL);
            leftBox.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            ));

            TextView tvDesc = new TextView(this);
            tvDesc.setText(tr.getDescription());
            tvDesc.setTextColor(0xFFFFFFFF);
            tvDesc.setTextSize(14);
            leftBox.addView(tvDesc);

            TextView tvDate = new TextView(this);
            tvDate.setText(formatTransactionDate(tr.getTransactionDate()));
            tvDate.setTextColor(0xFFAAAAAA);
            tvDate.setTextSize(12);
            leftBox.addView(tvDate);

            item.addView(leftBox);

            TextView tvValue = new TextView(this);
            boolean isIncome = "INCOME".equalsIgnoreCase(tr.getType());
            tvValue.setText((isIncome ? "+" : "-") + formatVND(tr.getAmount()));
            tvValue.setTextColor(isIncome ? 0xFF00FF66 : 0xFFFF3366);
            tvValue.setTypeface(null, android.graphics.Typeface.BOLD);
            item.addView(tvValue);

            layoutHistoryContainer.addView(item);
        }
    }

    private List<TransactionResponse> filterBySelectedMonth(List<TransactionResponse> source) {
        List<TransactionResponse> result = new ArrayList<>();
        String monthPrefix = String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonth);

        for (TransactionResponse tr : source) {
            if (tr.getTransactionDate() != null && tr.getTransactionDate().startsWith(monthPrefix)) {
                result.add(tr);
            }
        }

        return result;
    }

    private String formatTransactionDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return "Chưa có ngày giờ";
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(rawDate);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return dateTime.format(formatter);
        } catch (Exception e) {
            return rawDate;
        }
    }
}