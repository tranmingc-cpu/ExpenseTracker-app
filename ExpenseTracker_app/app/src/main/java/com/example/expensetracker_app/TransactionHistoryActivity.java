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
    private int selectedDay;

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
        selectedDay = 0;

        btnBackHistory.setOnClickListener(v -> finish());
        btnHistoryMonthFilter.setOnClickListener(v -> showMonthFilterDialog());

        updateMonthText();
        loadTransactions();
    }

    private void updateMonthText() {
        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);
        int currentMonth = now.get(Calendar.MONTH) + 1;

        if (selectedDay > 0) {
            if (selectedYear == currentYear && selectedMonth == currentMonth && selectedDay == now.get(Calendar.DAY_OF_MONTH)) {
                btnHistoryMonthFilter.setText("Hôm nay");
            } else {
                btnHistoryMonthFilter.setText(String.format(Locale.US, "Ngày %02d/%02d/%d", selectedDay, selectedMonth, selectedYear));
            }
            tvHistoryTitle.setText(String.format(Locale.US, "Giao dịch %02d/%02d/%d", selectedDay, selectedMonth, selectedYear));
        } else {
            if (selectedYear == currentYear && selectedMonth == currentMonth) {
                btnHistoryMonthFilter.setText("Tháng hiện tại");
            } else {
                btnHistoryMonthFilter.setText(String.format(Locale.US, "Tháng %02d/%d", selectedMonth, selectedYear));
            }
            tvHistoryTitle.setText(String.format(Locale.US, "Giao dịch %02d/%d", selectedMonth, selectedYear));
        }
    }

    private void showMonthFilterDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(40, 20, 40, 20);

        NumberPicker dayPicker = new NumberPicker(this);
        NumberPicker monthPicker = new NumberPicker(this);
        NumberPicker yearPicker = new NumberPicker(this);

        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(selectedMonth);
        monthPicker.setDisplayedValues(new String[]{
                "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4",
                "Tháng 5", "Tháng 6", "Tháng 7", "Tháng 8",
                "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
        });

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(2020);
        yearPicker.setMaxValue(currentYear + 1);
        yearPicker.setValue(selectedYear);

        dayPicker.setMinValue(0);

        NumberPicker.OnValueChangeListener dateChangeListener = (picker, oldVal, newVal) -> {
            int m = monthPicker.getValue();
            int y = yearPicker.getValue();
            Calendar calc = Calendar.getInstance();
            calc.set(Calendar.YEAR, y);
            calc.set(Calendar.MONTH, m - 1);
            int maxDays = calc.getActualMaximum(Calendar.DAY_OF_MONTH);

            if (dayPicker.getValue() > maxDays) {
                dayPicker.setValue(maxDays);
            }
            dayPicker.setDisplayedValues(null);
            dayPicker.setMaxValue(maxDays);

            String[] dayDisplays = new String[maxDays + 1];
            dayDisplays[0] = "Tất cả";
            for (int i = 1; i <= maxDays; i++) {
                dayDisplays[i] = "Ngày " + i;
            }
            dayPicker.setDisplayedValues(dayDisplays);
        };

        monthPicker.setOnValueChangedListener(dateChangeListener);
        yearPicker.setOnValueChangedListener(dateChangeListener);

        dateChangeListener.onValueChange(null, 0, 0);
        dayPicker.setValue(selectedDay);

        layout.addView(dayPicker, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        layout.addView(monthPicker, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        layout.addView(yearPicker, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn thời gian")
                .setView(layout)
                .setPositiveButton("Áp dụng", (dialog, which) -> {
                    selectedDay = dayPicker.getValue();
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

        RetrofitClient.getInstance().getTransactionApi().getByUser(userId, null, null)
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
            empty.setTextColor(themeColor(R.color.app_text_secondary));
            empty.setTextSize(14);
            empty.setPadding(12, 24, 12, 24);
            layoutHistoryContainer.addView(empty);
            return;
        }

        for (TransactionResponse tr : filtered) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setPadding(12, 12, 12, 12);
            item.setBackground(roundedBg(R.color.app_surface));

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
            tvDesc.setTextColor(themeColor(R.color.app_text_primary));
            tvDesc.setTextSize(14);
            leftBox.addView(tvDesc);

            TextView tvDate = new TextView(this);
            tvDate.setText(formatTransactionDate(tr.getTransactionDate()));
            tvDate.setTextColor(themeColor(R.color.app_text_secondary));
            tvDate.setTextSize(12);
            leftBox.addView(tvDate);

            item.addView(leftBox);

            TextView tvValue = new TextView(this);
            boolean isIncome = "INCOME".equalsIgnoreCase(tr.getType());
            tvValue.setText((isIncome ? "+" : "-") + formatVND(tr.getAmount()));
            tvValue.setTextColor(isIncome ? themeColor(R.color.app_accent_income) : themeColor(R.color.app_accent_expense));
            tvValue.setTypeface(null, android.graphics.Typeface.BOLD);
            item.addView(tvValue);

            // Thêm trình lắng nghe Nhấn giữ để Xóa
            item.setOnLongClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(TransactionHistoryActivity.this)
                        .setTitle("Xóa giao dịch")
                        .setMessage("Bạn có chắc chắn muốn xóa giao dịch này?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            deleteTransaction(tr);
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
                return true;
            });

            layoutHistoryContainer.addView(item);
        }
    }

    private void deleteTransaction(TransactionResponse tr) {
        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            // Xóa cục bộ/ngoại tuyến
            com.expensetracker_manager.utils.OfflineCacheManager cache = com.expensetracker_manager.utils.OfflineCacheManager.getInstance(this);
            List<TransactionResponse> txs = cache.getCachedTransactions();
            for (int i = 0; i < txs.size(); i++) {
                if (txs.get(i).getId() == tr.getId()) {
                    txs.remove(i);
                    break;
                }
            }
            cache.cacheTransactions(txs);
            com.expensetracker_manager.service.FinancialAnalysisEngine.analyze(this);
            
            // Điều chỉnh tóm tắt báo cáo ngoại tuyến
            com.expensetracker_manager.model.response.ReportSummaryResponse summary = cache.getCachedReportSummary();
            boolean isIncome = "INCOME".equalsIgnoreCase(tr.getType());
            if (isIncome) {
                summary.setTotalIncome(Math.max(0, summary.getTotalIncome() - tr.getAmount()));
                summary.setCurrentBalance(summary.getCurrentBalance() - tr.getAmount());
            } else {
                summary.setTotalExpense(Math.max(0, summary.getTotalExpense() - tr.getAmount()));
                summary.setCurrentBalance(summary.getCurrentBalance() + tr.getAmount());
            }
            cache.cacheReportSummary(summary);

            allTransactions = txs;
            renderTransactions();
            Toast.makeText(this, "Đã xóa giao dịch (Offline)", Toast.LENGTH_SHORT).show();
            return;
        }

        RetrofitClient.getInstance().getTransactionApi().delete(tr.getId())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(TransactionHistoryActivity.this, "Đã xóa giao dịch thành công!", Toast.LENGTH_SHORT).show();
                            // Xóa khỏi danh sách hiện tại
                            allTransactions.remove(tr);
                            // Cập nhật bộ nhớ đệm ngoại tuyến
                            com.expensetracker_manager.utils.OfflineCacheManager.getInstance(TransactionHistoryActivity.this)
                                    .cacheTransactions(allTransactions);
                            // Kích hoạt tính toán lại
                            com.expensetracker_manager.service.FinancialAnalysisEngine.analyze(TransactionHistoryActivity.this);
                            renderTransactions();
                        } else {
                            Toast.makeText(TransactionHistoryActivity.this, "Không thể xóa giao dịch", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(TransactionHistoryActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private List<TransactionResponse> filterBySelectedMonth(List<TransactionResponse> source) {
        List<TransactionResponse> result = new ArrayList<>();
        String datePrefix;
        if (selectedDay > 0) {
            datePrefix = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay);
        } else {
            datePrefix = String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonth);
        }

        for (TransactionResponse tr : source) {
            if (tr.getTransactionDate() != null && tr.getTransactionDate().startsWith(datePrefix)) {
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

    private int themeColor(int colorResId) {
        return androidx.core.content.ContextCompat.getColor(this, colorResId);
    }

    private android.graphics.drawable.GradientDrawable roundedBg(int colorResId) {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(themeColor(colorResId));
        bg.setCornerRadius(dp(12));
        return bg;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

}