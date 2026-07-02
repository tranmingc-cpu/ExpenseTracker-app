package com.example.expensetracker_app;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.expensetracker_manager.model.request.RecurringTransactionRequest;
import com.expensetracker_manager.model.response.CategoryResponse;
import com.expensetracker_manager.model.response.RecurringTransactionResponse;
import com.expensetracker_manager.model.response.TransactionResponse;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.NetworkUtils;
import com.expensetracker_manager.utils.OfflineCacheManager;
import com.expensetracker_manager.utils.TokenManager;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecurringActivity extends BaseActivity {

    private Button btnOpenAddRecurringDialog;
    private LinearLayout layoutRecurringContainer;
    private List<RecurringItem> recurringList = new ArrayList<>();

    public static class RecurringItem {
        public Long id;
        public String name;
        public double amount;
        public int day;
        public Integer lastPaidYear;
        public Integer lastPaidMonth;
        public String lastPaidAt;

        public RecurringItem(Long id, String name, double amount, int day) {
            this(id, name, amount, day, null, null, null);
        }

        public RecurringItem(
                Long id,
                String name,
                double amount,
                int day,
                Integer lastPaidYear,
                Integer lastPaidMonth,
                String lastPaidAt
        ) {
            this.id = id;
            this.name = name;
            this.amount = amount;
            this.day = day;
            this.lastPaidYear = lastPaidYear;
            this.lastPaidMonth = lastPaidMonth;
            this.lastPaidAt = lastPaidAt;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recurring);

        btnOpenAddRecurringDialog = findViewById(R.id.btnOpenAddRecurringDialog);
        layoutRecurringContainer = findViewById(R.id.layoutRecurringContainer);

        btnOpenAddRecurringDialog.setOnClickListener(v -> showAddRecurringDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecurringItems();
    }

    private void showAddRecurringDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        final android.widget.EditText etName = new android.widget.EditText(this);
        etName.setHint("Tên dịch vụ (Ví dụ: Tiền mạng, Netflix)");
        etName.setHintTextColor(themeColor(R.color.app_input_hint));
        etName.setTextColor(themeColor(R.color.app_text_primary));
        layout.addView(etName);

        final android.widget.EditText etAmount = new android.widget.EditText(this);
        etAmount.setHint("Số tiền thanh toán (đ)");
        etAmount.setHintTextColor(themeColor(R.color.app_input_hint));
        etAmount.setTextColor(themeColor(R.color.app_text_primary));
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etAmount.addTextChangedListener(new com.expensetracker_manager.utils.NumberTextWatcher(etAmount));
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        amountParams.setMargins(0, 24, 0, 0);
        etAmount.setLayoutParams(amountParams);
        layout.addView(etAmount);

        final android.widget.EditText etDay = new android.widget.EditText(this);
        etDay.setHint("Ngày nhắc hàng tháng (1-31)");
        etDay.setHintTextColor(themeColor(R.color.app_input_hint));
        etDay.setTextColor(themeColor(R.color.app_text_primary));
        etDay.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
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
            Button button = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                String amountStr = etAmount.getText().toString().trim().replace(".", "");
                String dayStr = etDay.getText().toString().trim();

                if (name.isEmpty() || amountStr.isEmpty() || dayStr.isEmpty()) {
                    Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                int day;
                try {
                    Double.parseDouble(amountStr);
                    day = Integer.parseInt(dayStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Số tiền hoặc ngày không hợp lệ", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (day < 1 || day > 31) {
                    Toast.makeText(this, "Ngày thanh toán phải từ 1 đến 31", Toast.LENGTH_SHORT).show();
                    return;
                }

                performSaveRecurring(name, amountStr, day);
                dialog.dismiss();
            });
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            android.graphics.drawable.GradientDrawable background =
                    new android.graphics.drawable.GradientDrawable();
            background.setColor(themeColor(R.color.app_surface));
            background.setCornerRadius(24f);
            dialog.getWindow().setBackgroundDrawable(background);
        }
    }

    private void loadRecurringItems() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            recurringList = OfflineCacheManager.getInstance(this).getCachedRecurringItems();
            renderRecurringList();
            return;
        }

        Long userId = TokenManager.getInstance(this).getUserId();
        if (userId == -1L) {
            return;
        }

        RetrofitClient.getInstance().getRecurringTransactionApi().getByUser(userId)
                .enqueue(new Callback<List<RecurringTransactionResponse>>() {
                    @Override
                    public void onResponse(
                            Call<List<RecurringTransactionResponse>> call,
                            Response<List<RecurringTransactionResponse>> response
                    ) {
                        if (!response.isSuccessful() || response.body() == null) {
                            recurringList = OfflineCacheManager.getInstance(RecurringActivity.this)
                                    .getCachedRecurringItems();
                            renderRecurringList();
                            return;
                        }

                        recurringList.clear();
                        for (RecurringTransactionResponse item : response.body()) {
                            int day = parsePaymentDay(item.getNextExecutionDate());
                            double amount = item.getAmount() == null
                                    ? 0d
                                    : item.getAmount().doubleValue();

                            recurringList.add(new RecurringItem(
                                    item.getId(),
                                    item.getDescription(),
                                    amount,
                                    day,
                                    item.getLastPaidYear(),
                                    item.getLastPaidMonth(),
                                    item.getLastPaidAt()
                            ));
                        }

                        OfflineCacheManager.getInstance(RecurringActivity.this)
                                .cacheRecurringItems(recurringList);
                        RecurringNotificationManager.saveItems(
                                RecurringActivity.this,
                                response.body()
                        );
                        renderRecurringList();
                    }

                    @Override
                    public void onFailure(Call<List<RecurringTransactionResponse>> call, Throwable throwable) {
                        recurringList = OfflineCacheManager.getInstance(RecurringActivity.this)
                                .getCachedRecurringItems();
                        renderRecurringList();
                    }
                });
    }

    private int parsePaymentDay(String rawDate) {
        try {
            if (rawDate != null) {
                String datePart = rawDate.split("T")[0];
                String[] parts = datePart.split("-");
                return Integer.parseInt(parts[2]);
            }
        } catch (Exception ignored) {
        }
        return 15;
    }

    private void performSaveRecurring(String name, String amountStr, int day) {
        double amount = Double.parseDouble(amountStr);
        Long userId = TokenManager.getInstance(this).getUserId();

        if (!NetworkUtils.isNetworkAvailable(this)) {
            RecurringItem item = new RecurringItem(
                    System.currentTimeMillis(),
                    name,
                    amount,
                    day,
                    null,
                    null,
                    null
            );
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

        LocalDateTime nextExecution = LocalDateTime.now();
        int currentDay = nextExecution.getDayOfMonth();
        if (day < currentDay) {
            nextExecution = nextExecution.plusMonths(1);
        }

        try {
            nextExecution = nextExecution.withDayOfMonth(day)
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);
        } catch (Exception ignored) {
            nextExecution = nextExecution
                    .withDayOfMonth(nextExecution.toLocalDate().lengthOfMonth())
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);
        }
        request.setNextExecutionDate(nextExecution.toString());

        RetrofitClient.getInstance().getRecurringTransactionApi().create(request)
                .enqueue(new Callback<RecurringTransactionResponse>() {
                    @Override
                    public void onResponse(
                            Call<RecurringTransactionResponse> call,
                            Response<RecurringTransactionResponse> response
                    ) {
                        if (response.isSuccessful()) {
                            loadRecurringItems();
                            Toast.makeText(
                                    RecurringActivity.this,
                                    "Đã lưu khoản định kỳ thành công!",
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            Toast.makeText(
                                    RecurringActivity.this,
                                    "Không thể lưu khoản định kỳ.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<RecurringTransactionResponse> call, Throwable throwable) {
                        Toast.makeText(
                                RecurringActivity.this,
                                "Lỗi kết nối mạng: " + throwable.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void renderRecurringList() {
        layoutRecurringContainer.removeAllViews();

        for (int index = 0; index < recurringList.size(); index++) {
            final int itemIndex = index;
            RecurringItem item = recurringList.get(index);
            boolean paidThisMonth = isPaidThisMonth(item);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(10), dp(9), dp(10), dp(9));
            row.setBackground(roundedBg(R.color.app_surface_alt));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 0, 0, dp(8));
            row.setLayoutParams(rowParams);

            TextView tvName = new TextView(this);
            tvName.setText(item.name);
            tvName.setTextColor(themeColor(R.color.app_text_primary));
            tvName.setTextSize(15);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(tvName);

            TextView tvInfo = new TextView(this);
            String info = "Số tiền: " + formatVND(item.amount)
                    + "\nNgày thanh toán: ngày " + item.day + " hằng tháng";
            if (paidThisMonth) {
                YearMonth currentMonth = YearMonth.now();
                info += String.format(
                        Locale.US,
                        "\nTrạng thái: Đã thanh toán tháng %02d/%d",
                        currentMonth.getMonthValue(),
                        currentMonth.getYear()
                );
            }
            tvInfo.setText(info);
            tvInfo.setTextColor(themeColor(
                    paidThisMonth ? R.color.app_accent_income : R.color.app_text_secondary
            ));
            tvInfo.setTextSize(11);
            tvInfo.setLineSpacing(0, 1.03f);
            row.addView(tvInfo);

            LinearLayout actionRow = new LinearLayout(this);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            actionRow.setGravity(Gravity.CENTER_VERTICAL);
            actionRow.setPadding(0, dp(7), 0, 0);
            actionRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            Button btnPay = createCompactButton();
            btnPay.setText(paidThisMonth
                    ? "Đã thanh toán T" + YearMonth.now().getMonthValue()
                    : "Thanh toán ngay");
            btnPay.setBackgroundTintList(ColorStateList.valueOf(themeColor(
                    paidThisMonth ? R.color.app_text_muted : R.color.app_accent_income
            )));
            btnPay.setLayoutParams(new LinearLayout.LayoutParams(0, dp(36), 1.25f));
            btnPay.setAlpha(paidThisMonth ? 0.78f : 1f);
            btnPay.setOnClickListener(v -> {
                if (isPaidThisMonth(item)) {
                    showAlreadyPaidMessage();
                    return;
                }
                payRecurringItem(item, btnPay);
            });
            actionRow.addView(btnPay);

            Button btnDelete = createCompactButton();
            btnDelete.setText("Xóa");
            btnDelete.setBackgroundTintList(ColorStateList.valueOf(
                    themeColor(R.color.app_accent_expense)
            ));
            LinearLayout.LayoutParams deleteParams =
                    new LinearLayout.LayoutParams(0, dp(36), 0.75f);
            deleteParams.setMargins(dp(8), 0, 0, 0);
            btnDelete.setLayoutParams(deleteParams);
            btnDelete.setOnClickListener(v -> deleteRecurringItem(itemIndex));
            actionRow.addView(btnDelete);

            row.addView(actionRow);
            layoutRecurringContainer.addView(row);
        }
    }

    private Button createCompactButton() {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setSingleLine(true);
        button.setTextSize(11);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setTextColor(themeColor(R.color.app_button_text));
        return button;
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
                        if (response.isSuccessful()) {
                            loadRecurringItems();
                            Toast.makeText(
                                    RecurringActivity.this,
                                    "Đã xóa thành công!",
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            Toast.makeText(
                                    RecurringActivity.this,
                                    "Không thể xóa khoản định kỳ.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable throwable) {
                        Toast.makeText(
                                RecurringActivity.this,
                                "Lỗi kết nối: " + throwable.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void payRecurringItem(RecurringItem item, Button btnPay) {
        Long userId = TokenManager.getInstance(this).getUserId();
        if (userId == -1L || item.id == null) {
            Toast.makeText(this, "Không tìm thấy thông tin khoản định kỳ.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isPaidThisMonth(item)) {
            showAlreadyPaidMessage();
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(
                    this,
                    "Cần kết nối mạng để thanh toán định kỳ và tránh ghi trùng.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        setPayButtonLoading(btnPay, true);
        RetrofitClient.getInstance().getCategoryApi().getAll()
                .enqueue(new Callback<List<CategoryResponse>>() {
                    @Override
                    public void onResponse(
                            Call<List<CategoryResponse>> call,
                            Response<List<CategoryResponse>> response
                    ) {
                        Long categoryId = findExpenseCategoryId(response.body());
                        if (!response.isSuccessful() || categoryId == null) {
                            setPayButtonLoading(btnPay, false);
                            Toast.makeText(
                                    RecurringActivity.this,
                                    "Không tìm thấy danh mục chi tiêu phù hợp.",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        submitRecurringPayment(item, userId, categoryId, btnPay);
                    }

                    @Override
                    public void onFailure(Call<List<CategoryResponse>> call, Throwable throwable) {
                        setPayButtonLoading(btnPay, false);
                        Toast.makeText(
                                RecurringActivity.this,
                                "Không thể lấy danh mục.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private Long findExpenseCategoryId(List<CategoryResponse> categories) {
        if (categories == null || categories.isEmpty()) {
            return null;
        }

        Long firstExpenseId = null;
        for (CategoryResponse category : categories) {
            if (category == null) {
                continue;
            }

            if ("Khác".equalsIgnoreCase(category.getName())) {
                return category.getId();
            }

            if (firstExpenseId == null && "EXPENSE".equalsIgnoreCase(category.getType())) {
                firstExpenseId = category.getId();
            }
        }
        return firstExpenseId;
    }

    private void submitRecurringPayment(
            RecurringItem item,
            Long userId,
            Long categoryId,
            Button btnPay
    ) {
        RetrofitClient.getInstance().getRecurringTransactionApi()
                .pay(item.id, userId, categoryId)
                .enqueue(new Callback<TransactionResponse>() {
                    @Override
                    public void onResponse(
                            Call<TransactionResponse> call,
                            Response<TransactionResponse> response
                    ) {
                        if (response.isSuccessful()) {
                            YearMonth currentMonth = YearMonth.now();
                            item.lastPaidYear = currentMonth.getYear();
                            item.lastPaidMonth = currentMonth.getMonthValue();
                            item.lastPaidAt = LocalDateTime.now().toString();

                            Toast.makeText(
                                    RecurringActivity.this,
                                    "Đã thanh toán " + formatVND(item.amount)
                                            + " cho " + item.name + " thành công!",
                                    Toast.LENGTH_LONG
                            ).show();
                            loadRecurringItems();
                            return;
                        }

                        setPayButtonLoading(btnPay, false);
                        String message = extractApiError(
                                response,
                                "Không thể thanh toán khoản định kỳ."
                        );

                        if (message.toLowerCase(Locale.ROOT).contains("đã được thanh toán")) {
                            YearMonth currentMonth = YearMonth.now();
                            item.lastPaidYear = currentMonth.getYear();
                            item.lastPaidMonth = currentMonth.getMonthValue();
                            renderRecurringList();
                        }

                        Toast.makeText(
                                RecurringActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    @Override
                    public void onFailure(Call<TransactionResponse> call, Throwable throwable) {
                        setPayButtonLoading(btnPay, false);
                        Toast.makeText(
                                RecurringActivity.this,
                                "Lỗi kết nối mạng: " + throwable.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void setPayButtonLoading(Button button, boolean loading) {
        button.setEnabled(!loading);
        button.setAlpha(loading ? 0.6f : 1f);
        button.setText(loading ? "Đang xử lý..." : "Thanh toán ngay");
    }

    private String extractApiError(Response<?> response, String fallback) {
        try {
            if (response.errorBody() == null) {
                return fallback;
            }

            String raw = response.errorBody().string();
            if (raw.trim().isEmpty()) {
                return fallback;
            }

            String message = new JSONObject(raw).optString("message", fallback);
            return message == null || message.trim().isEmpty() ? fallback : message;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private boolean isPaidThisMonth(RecurringItem item) {
        if (item == null || item.lastPaidYear == null || item.lastPaidMonth == null) {
            return false;
        }

        YearMonth currentMonth = YearMonth.now();
        return item.lastPaidYear == currentMonth.getYear()
                && item.lastPaidMonth == currentMonth.getMonthValue();
    }

    private void showAlreadyPaidMessage() {
        YearMonth currentMonth = YearMonth.now();
        Toast.makeText(
                this,
                String.format(
                        Locale.US,
                        "Khoản này đã được thanh toán trong tháng %02d/%d.",
                        currentMonth.getMonthValue(),
                        currentMonth.getYear()
                ),
                Toast.LENGTH_LONG
        ).show();
    }

    private int themeColor(int colorResId) {
        return androidx.core.content.ContextCompat.getColor(this, colorResId);
    }

    private android.graphics.drawable.GradientDrawable roundedBg(int colorResId) {
        android.graphics.drawable.GradientDrawable background =
                new android.graphics.drawable.GradientDrawable();
        background.setColor(themeColor(colorResId));
        background.setCornerRadius(dp(12));
        return background;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
