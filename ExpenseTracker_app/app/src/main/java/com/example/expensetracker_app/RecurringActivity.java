package com.example.expensetracker_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.expensetracker_manager.model.request.TransactionRequest;
import com.expensetracker_manager.model.request.RecurringTransactionRequest;
import com.expensetracker_manager.model.response.CategoryResponse;
import com.expensetracker_manager.model.response.TransactionResponse;
import com.expensetracker_manager.model.response.RecurringTransactionResponse;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.TokenManager;
import com.expensetracker_manager.utils.NetworkUtils;
import com.expensetracker_manager.utils.OfflineCacheManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecurringActivity extends BaseActivity {

    private Button btnOpenAddRecurringDialog;
    private LinearLayout layoutRecurringContainer;
    private List<RecurringItem> recurringList = new ArrayList<>();
    private Gson gson = new Gson();

    public static class RecurringItem {
        public Long id;
        public String name;
        public double amount;
        public int day;

        public RecurringItem(Long id, String name, double amount, int day) {
            this.id = id;
            this.name = name;
            this.amount = amount;
            this.day = day;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recurring);

        btnOpenAddRecurringDialog = findViewById(R.id.btnOpenAddRecurringDialog);
        layoutRecurringContainer = findViewById(R.id.layoutRecurringContainer);

        loadRecurringItems();

        btnOpenAddRecurringDialog.setOnClickListener(v -> showAddRecurringDialog());
    }

    private void showAddRecurringDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        final android.widget.EditText etName = new android.widget.EditText(this);
        etName.setHint("Tên dịch vụ (Ví dụ: Tiền mạng, Netflix)");
        etName.setHintTextColor(0xFF808090);
        etName.setTextColor(0xFFFFFFFF);
        layout.addView(etName);

        final android.widget.EditText etAmount = new android.widget.EditText(this);
        etAmount.setHint("Số tiền thanh toán (đ)");
        etAmount.setHintTextColor(0xFF808090);
        etAmount.setTextColor(0xFFFFFFFF);
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        android.widget.LinearLayout.LayoutParams amtParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        amtParams.setMargins(0, 24, 0, 0);
        etAmount.setLayoutParams(amtParams);
        layout.addView(etAmount);

        final android.widget.EditText etDay = new android.widget.EditText(this);
        etDay.setHint("Ngày nhắc hàng tháng (1-31)");
        etDay.setHintTextColor(0xFF808090);
        etDay.setTextColor(0xFFFFFFFF);
        etDay.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        android.widget.LinearLayout.LayoutParams dayParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dayParams.setMargins(0, 24, 0, 0);
        etDay.setLayoutParams(dayParams);
        layout.addView(etDay);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Thêm Tiền Hàng Tháng")
                .setView(layout)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button button = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                String amountStr = etAmount.getText().toString().trim();
                String dayStr = etDay.getText().toString().trim();

                if (name.isEmpty() || amountStr.isEmpty() || dayStr.isEmpty()) {
                    Toast.makeText(RecurringActivity.this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount;
                int day;
                try {
                    amount = Double.parseDouble(amountStr);
                    day = Integer.parseInt(dayStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(RecurringActivity.this, "Số tiền hoặc ngày không hợp lệ", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (day < 1 || day > 31) {
                    Toast.makeText(RecurringActivity.this, "Ngày thanh toán phải từ 1 đến 31", Toast.LENGTH_SHORT).show();
                    return;
                }

                performSaveRecurring(name, amountStr, day);
                dialog.dismiss();
            });
        });

        // Style the window background to fit the dark theme
        if (dialog.getWindow() != null) {
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(0xFF1E1E2C);
            gd.setCornerRadius(24f);
            dialog.getWindow().setBackgroundDrawable(gd);
        }

        dialog.show();
    }

    private void loadRecurringItems() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            recurringList = OfflineCacheManager.getInstance(this).getCachedRecurringItems();
            renderRecurringList();
            return;
        }

        Long userId = TokenManager.getInstance(this).getUserId();
        RetrofitClient.getInstance().getRecurringTransactionApi().getByUser(userId)
                .enqueue(new Callback<List<RecurringTransactionResponse>>() {
                    @Override
                    public void onResponse(Call<List<RecurringTransactionResponse>> call, Response<List<RecurringTransactionResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            recurringList.clear();
                            for (RecurringTransactionResponse res : response.body()) {
                                int day = 15;
                                try {
                                    if (res.getNextExecutionDate() != null) {
                                        String datePart = res.getNextExecutionDate().split("T")[0];
                                        String[] parts = datePart.split("-");
                                        day = Integer.parseInt(parts[2]);
                                    }
                                } catch (Exception e) {}
                                recurringList.add(new RecurringItem(res.getId(), res.getDescription(), res.getAmount().doubleValue(), day));
                            }
                            OfflineCacheManager.getInstance(RecurringActivity.this).cacheRecurringItems(recurringList);
                            renderRecurringList();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<RecurringTransactionResponse>> call, Throwable t) {
                        recurringList = OfflineCacheManager.getInstance(RecurringActivity.this).getCachedRecurringItems();
                        renderRecurringList();
                    }
                });
    }

    private void performSaveRecurring(String name, String amountStr, int day) {
        double amount = Double.parseDouble(amountStr);
        Long userId = TokenManager.getInstance(this).getUserId();

        if (!NetworkUtils.isNetworkAvailable(this)) {
            RecurringItem item = new RecurringItem(System.currentTimeMillis(), name, amount, day);
            recurringList.add(item);
            OfflineCacheManager.getInstance(this).cacheRecurringItems(recurringList);

            renderRecurringList();
            Toast.makeText(this, "Đã lưu khoản định kỳ (Offline)!", Toast.LENGTH_SHORT).show();
            return;
        }

        RecurringTransactionRequest request = new RecurringTransactionRequest();
        request.setAmount(new BigDecimal(amountStr));
        request.setDescription(name);
        request.setType("EXPENSE");
        request.setFrequency("MONTHLY");
        request.setUserId(userId);

        java.time.LocalDateTime nextExecution = java.time.LocalDateTime.now();
        int currentDay = nextExecution.getDayOfMonth();
        if (day < currentDay) {
            nextExecution = nextExecution.plusMonths(1);
        }
        try {
            nextExecution = nextExecution.withDayOfMonth(day).withHour(0).withMinute(0).withSecond(0);
        } catch (Exception e) {
            nextExecution = nextExecution.withDayOfMonth(nextExecution.toLocalDate().lengthOfMonth());
        }
        request.setNextExecutionDate(nextExecution.toString());

        RetrofitClient.getInstance().getRecurringTransactionApi().create(request)
                .enqueue(new Callback<RecurringTransactionResponse>() {
                    @Override
                    public void onResponse(Call<RecurringTransactionResponse> call, Response<RecurringTransactionResponse> response) {
                        if (response.isSuccessful()) {
                            loadRecurringItems();
                            Toast.makeText(RecurringActivity.this, "Đã lưu khoản định kỳ thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(RecurringActivity.this, "Không thể lưu khoản định kỳ.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<RecurringTransactionResponse> call, Throwable t) {
                        Toast.makeText(RecurringActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void renderRecurringList() {
        layoutRecurringContainer.removeAllViews();
        for (int i = 0; i < recurringList.size(); i++) {
            final int index = i;
            RecurringItem item = recurringList.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(16, 16, 16, 16);
            row.setBackgroundColor(0xFF2A2A3E);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            row.setLayoutParams(params);

            TextView tvName = new TextView(this);
            tvName.setText(item.name);
            tvName.setTextColor(0xFFFFFFFF);
            tvName.setTextSize(16);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(tvName);

            TextView tvInfo = new TextView(this);
            tvInfo.setText("Số tiền: " + formatVND(item.amount) + " | Ngày thanh toán: " + item.day + " hàng tháng");
            tvInfo.setTextColor(0xFF8A8A9E);
            tvInfo.setTextSize(12);
            row.addView(tvInfo);

            LinearLayout actionRow = new LinearLayout(this);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            actionRow.setPadding(0, 8, 0, 0);

            Button btnPay = new Button(this);
            btnPay.setText("Thanh toán ngay");
            btnPay.setTextSize(11);
            btnPay.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF00FF66));
            btnPay.setTextColor(0xFFFFFFFF);
            btnPay.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 80
            ));
            btnPay.setOnClickListener(v -> payRecurringItem(item));
            actionRow.addView(btnPay);

            Button btnDelete = new Button(this);
            btnDelete.setText("Xóa");
            btnDelete.setTextSize(11);
            btnDelete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF3366));
            btnDelete.setTextColor(0xFFFFFFFF);
            LinearLayout.LayoutParams delParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 80
            );
            delParams.setMargins(16, 0, 0, 0);
            btnDelete.setLayoutParams(delParams);
            btnDelete.setOnClickListener(v -> deleteRecurringItem(index));
            actionRow.addView(btnDelete);

            row.addView(actionRow);
            layoutRecurringContainer.addView(row);
        }
    }

    private void deleteRecurringItem(int index) {
        RecurringItem item = recurringList.get(index);
        if (!NetworkUtils.isNetworkAvailable(this) || item.id == null) {
            recurringList.remove(index);
            OfflineCacheManager.getInstance(this).cacheRecurringItems(recurringList);
            renderRecurringList();
            Toast.makeText(this, "Đã xóa khoản định kỳ (Offline)!", Toast.LENGTH_SHORT).show();
            return;
        }

        RetrofitClient.getInstance().getRecurringTransactionApi().delete(item.id)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        loadRecurringItems();
                        Toast.makeText(RecurringActivity.this, "Đã xóa thành công!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(RecurringActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void payRecurringItem(RecurringItem item) {
        Long userId = TokenManager.getInstance(this).getUserId();
        if (userId == -1L) return;

        if (!NetworkUtils.isNetworkAvailable(this)) {
            // Offline payment: deduct locally
            OfflineCacheManager cache = OfflineCacheManager.getInstance(this);
            com.expensetracker_manager.model.response.ReportSummaryResponse summary = cache.getCachedReportSummary();
            summary.setTotalExpense(summary.getTotalExpense() + item.amount);
            summary.setCurrentBalance(summary.getCurrentBalance() - item.amount);
            cache.cacheReportSummary(summary);

            // Add fake offline transaction response to local cache
            List<TransactionResponse> txs = cache.getCachedTransactions();
            TransactionResponse newTx = new TransactionResponse();
            newTx.setId(System.currentTimeMillis());
            newTx.setDescription("Thanh toán định kỳ: " + item.name);
            newTx.setAmount(item.amount);
            newTx.setType("EXPENSE");
            newTx.setCategoryName("Khác");
            newTx.setTransactionDate(java.time.LocalDateTime.now().toString());
            txs.add(0, newTx);
            cache.cacheTransactions(txs);

            Toast.makeText(this, "Đã thanh toán (Offline) " + formatVND(item.amount) + " thành công!", Toast.LENGTH_LONG).show();
            return;
        }

        RetrofitClient.getInstance().getCategoryApi().getAll()
                .enqueue(new Callback<List<CategoryResponse>>() {
                    @Override
                    public void onResponse(Call<List<CategoryResponse>> call, Response<List<CategoryResponse>> response) {
                        long categoryId = 1;
                        if (response.isSuccessful() && response.body() != null) {
                            for (CategoryResponse cat : response.body()) {
                                if ("Khác".equalsIgnoreCase(cat.getName())) {
                                    categoryId = cat.getId();
                                    break;
                                }
                            }
                        }

                        TransactionRequest request = new TransactionRequest();
                        request.setAmount(item.amount);
                        request.setDescription("Thanh toán định kỳ: " + item.name);
                        request.setType("EXPENSE");
                        request.setTransactionDate(java.time.LocalDateTime.now().toString());
                        request.setUserId(userId);
                        request.setCategoryId(categoryId);

                        RetrofitClient.getInstance().getTransactionApi().create(request)
                                .enqueue(new Callback<TransactionResponse>() {
                                    @Override
                                    public void onResponse(Call<TransactionResponse> call, Response<TransactionResponse> response) {
                                        if (response.isSuccessful()) {
                                            Toast.makeText(RecurringActivity.this, "Đã thanh toán " + formatVND(item.amount) + " cho " + item.name + " thành công!", Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(RecurringActivity.this, "Không thể thanh toán định kỳ.", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<TransactionResponse> call, Throwable t) {
                                        Toast.makeText(RecurringActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Call<List<CategoryResponse>> call, Throwable t) {
                        Toast.makeText(RecurringActivity.this, "Không thể lấy danh mục.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
