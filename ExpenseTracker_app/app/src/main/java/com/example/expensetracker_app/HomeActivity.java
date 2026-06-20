package com.example.expensetracker_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.expensetracker_manager.model.response.ReportSummaryResponse;
import com.expensetracker_manager.model.response.TransactionResponse;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.TokenManager;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.OutputStream;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import android.widget.NumberPicker;
import java.util.Locale;

import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

public class HomeActivity extends BaseActivity {

    private TextView tvDashboardUserName, tvNetBalance, tvTotalIncome, tvTotalExpense, tvHabitsWarning, btnViewAllTransactions;
    private ImageView btnProfile;
    private Button btnNavAdd, btnNavBudgets, btnNavGoals, btnNavRecurring, btnNavSync, btnNavExport, btnSignOut, btnMonthFilter;

    private int selectedYear;
    private int selectedMonth;

    private Button btnNavAdd, btnNavBudgets, btnNavGoals, btnNavRecurring, btnNavSync, btnNavExport;
    private LinearLayout layoutTransactionsContainer;
    private LinearLayout btnBottomNavAdd, btnBottomNavGoals, btnBottomNavQr, btnBottomNavBudgets, btnBottomNavProfile;

    private androidx.cardview.widget.CardView cardBudgetWarning;
    private TextView tvBudgetWarningMessage;

    private android.widget.EditText etQuickIncomeAmount;
    private Button btnSaveQuickIncome;

    private List<TransactionResponse> transactionsList = new ArrayList<>();
    private List<com.expensetracker_manager.model.response.BudgetResponse> budgetList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvDashboardUserName = findViewById(R.id.tvDashboardUserName);
        tvNetBalance = findViewById(R.id.tvNetBalance);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvHabitsWarning = findViewById(R.id.tvHabitsWarning);
        btnProfile = findViewById(R.id.btnProfile);
        btnNavAdd = findViewById(R.id.btnNavAdd);
        btnNavBudgets = findViewById(R.id.btnNavBudgets);
        btnNavGoals = findViewById(R.id.btnNavGoals);
        btnNavRecurring = findViewById(R.id.btnNavRecurring);
        btnNavSync = findViewById(R.id.btnNavSync);
        btnNavExport = findViewById(R.id.btnNavExport);
        btnSignOut = findViewById(R.id.btnSignOut);
        btnMonthFilter = findViewById(R.id.btnMonthFilter);

        Calendar now = Calendar.getInstance();
        selectedYear = now.get(Calendar.YEAR);
        selectedMonth = now.get(Calendar.MONTH) + 1;
        updateMonthFilterText();

        layoutTransactionsContainer = findViewById(R.id.layoutTransactionsContainer);
        etQuickIncomeAmount = findViewById(R.id.etQuickIncomeAmount);
        btnSaveQuickIncome = findViewById(R.id.btnSaveQuickIncome);
        cardBudgetWarning = findViewById(R.id.cardBudgetWarning);
        tvBudgetWarningMessage = findViewById(R.id.tvBudgetWarningMessage);

        btnViewAllTransactions = findViewById(R.id.btnViewAllTransactions);
        btnBottomNavAdd = findViewById(R.id.btnBottomNavAdd);
        btnBottomNavGoals = findViewById(R.id.btnBottomNavGoals);
        btnBottomNavQr = findViewById(R.id.btnBottomNavQr);
        btnBottomNavBudgets = findViewById(R.id.btnBottomNavBudgets);
        btnBottomNavProfile = findViewById(R.id.btnBottomNavProfile);

        TokenManager tokenManager = TokenManager.getInstance(this);
        tvDashboardUserName.setText(tokenManager.getUserName().isEmpty() ? "Người dùng" : tokenManager.getUserName());

        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void setupListeners() {
        btnProfile.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));
        btnNavAdd.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, AddTransactionActivity.class)));
        btnNavBudgets.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, BudgetsActivity.class)));
        btnNavGoals.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, SavingsGoalsActivity.class)));

        btnNavRecurring.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, RecurringActivity.class)));

        btnNavSync.setOnClickListener(v -> simulateBankSync());
        btnNavExport.setOnClickListener(v -> exportTransactionsToCSV());
        btnMonthFilter.setOnClickListener(v -> showMonthFilterDialog());

        btnSaveQuickIncome.setOnClickListener(v -> saveQuickIncome());

        btnSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            TokenManager.getInstance(HomeActivity.this).clear();
            Toast.makeText(HomeActivity.this, "Đăng xuất thành công!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
        });

        btnViewAllTransactions.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TransactionHistoryActivity.class);
            intent.putExtra("selectedYear", selectedYear);
            intent.putExtra("selectedMonth", selectedMonth);
            startActivity(intent);
        });
        btnBottomNavAdd.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, AddTransactionActivity.class)));
        btnBottomNavGoals.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, SavingsGoalsActivity.class)));
        btnBottomNavBudgets.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, BudgetsActivity.class)));
        btnBottomNavProfile.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));
        btnBottomNavQr.setOnClickListener(v -> startQRScanner());
    }

    private void startQRScanner() {
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .enableAutoZoom()
                .build();

        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);

        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String qrContent = barcode.getRawValue();
                    if (qrContent == null || qrContent.trim().isEmpty()) {
                        Toast.makeText(this, "Không đọc được dữ liệu từ QR.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    VietQrParser.QrData data = VietQrParser.parse(qrContent);
                    Intent addTxIntent = new Intent(HomeActivity.this, AddTransactionActivity.class);

                    if (data.isValid) {
                        addTxIntent.putExtra("extra_amount", data.amount);

                        StringBuilder descBuilder = new StringBuilder();
                        descBuilder.append("Chuyển khoản đến ");
                        if (data.bankName != null && !data.bankName.isEmpty()) {
                            descBuilder.append(data.bankName).append(" ");
                        }
                        if (data.accountNumber != null && !data.accountNumber.isEmpty()) {
                            descBuilder.append("(").append(data.accountNumber).append(") ");
                        }
                        if (data.recipientName != null && !data.recipientName.isEmpty()) {
                            descBuilder.append("- ").append(data.recipientName);
                        }
                        if (data.memo != null && !data.memo.isEmpty()) {
                            descBuilder.append("\nNội dung: ").append(data.memo);
                        }
                        addTxIntent.putExtra("extra_desc", descBuilder.toString().trim());
                    } else {
                        addTxIntent.putExtra("extra_desc", qrContent);
                    }
                    startActivity(addTxIntent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Quét mã thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveQuickIncome() {
        String amountStr = etQuickIncomeAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        Long userId = TokenManager.getInstance(this).getUserId();
        if (userId == -1L) return;

        btnSaveQuickIncome.setEnabled(false);

        // Fetch categories to find the "Thu nhập" category
        RetrofitClient.getInstance().getCategoryApi().getAll()
                .enqueue(new Callback<List<com.expensetracker_manager.model.response.CategoryResponse>>() {
                    @Override
                    public void onResponse(Call<List<com.expensetracker_manager.model.response.CategoryResponse>> call,
                            Response<List<com.expensetracker_manager.model.response.CategoryResponse>> response) {
                        long categoryId = -1;
                        if (response.isSuccessful() && response.body() != null) {
                            for (com.expensetracker_manager.model.response.CategoryResponse cat : response.body()) {
                                if ("Thu nhập".equalsIgnoreCase(cat.getName()) || "INCOME".equalsIgnoreCase(cat.getType())) {
                                    categoryId = cat.getId();
                                    break;
                                }
                            }
                        }

                        // If not found online, fallback to 1
                        if (categoryId == -1) {
                            categoryId = 1;
                        }

                        com.expensetracker_manager.model.request.TransactionRequest request = new com.expensetracker_manager.model.request.TransactionRequest();
                        request.setAmount(amount);
                        request.setDescription("Bổ sung tiền mặt");
                        request.setType("INCOME");
                        request.setTransactionDate(java.time.LocalDateTime.now().toString());
                        request.setUserId(userId);
                        request.setCategoryId(categoryId);

                        RetrofitClient.getInstance().getTransactionApi().create(request)
                                .enqueue(new Callback<com.expensetracker_manager.model.response.TransactionResponse>() {
                                    @Override
                                    public void onResponse(Call<com.expensetracker_manager.model.response.TransactionResponse> call,
                                            Response<com.expensetracker_manager.model.response.TransactionResponse> response) {
                                        btnSaveQuickIncome.setEnabled(true);
                                        if (response.isSuccessful()) {
                                            Toast.makeText(HomeActivity.this, "Đã bổ sung thu nhập thành công!", Toast.LENGTH_SHORT).show();
                                            etQuickIncomeAmount.setText("");
                                            loadDashboardData();
                                        } else {
                                            Toast.makeText(HomeActivity.this, "Không thể lưu giao dịch thu nhập.", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<com.expensetracker_manager.model.response.TransactionResponse> call, Throwable t) {
                                        btnSaveQuickIncome.setEnabled(true);
                                        Toast.makeText(HomeActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Call<List<com.expensetracker_manager.model.response.CategoryResponse>> call, Throwable t) {
                        btnSaveQuickIncome.setEnabled(true);
                        Toast.makeText(HomeActivity.this, "Không thể lấy danh mục: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void updateMonthFilterText() {
        if (btnMonthFilter == null) {
            return;
        }

        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);
        int currentMonth = now.get(Calendar.MONTH) + 1;

        if (selectedYear == currentYear && selectedMonth == currentMonth) {
            btnMonthFilter.setText("Tháng hiện tại");
        } else {
            btnMonthFilter.setText(String.format(Locale.US, "Tháng %02d/%d", selectedMonth, selectedYear));
        }
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

        layout.addView(monthPicker, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        layout.addView(yearPicker, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn tháng báo cáo")
                .setView(layout)
                .setPositiveButton("Áp dụng", (dialog, which) -> {
                    selectedMonth = monthPicker.getValue();
                    selectedYear = yearPicker.getValue();
                    updateMonthFilterText();
                    loadDashboardData();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void loadDashboardData() {
        Long userId = TokenManager.getInstance(this).getUserId();
        if (userId == -1L) return;

        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            // Load from offline cache
            com.expensetracker_manager.utils.OfflineCacheManager cache = com.expensetracker_manager.utils.OfflineCacheManager.getInstance(this);
            ReportSummaryResponse summary = cache.getCachedReportSummary();
            double net = summary.getCurrentBalance() != 0 ? summary.getCurrentBalance() : (summary.getTotalIncome() - summary.getTotalExpense());
            tvNetBalance.setText(formatVND(net));
            tvTotalIncome.setText(formatVND(summary.getTotalIncome()));
            tvTotalExpense.setText(formatVND(summary.getTotalExpense()));
            tvHabitsWarning.setText("Offline Mode: Đang hiển thị dữ liệu lưu tạm.");

            transactionsList = filterTransactionsBySelectedMonth(cache.getCachedTransactions());
            renderTransactions();

            budgetList = cache.getCachedBudgets();
            evaluateBudgetStatus();
            return;
        }

        // Fetch balance report
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, selectedYear);
        cal.set(Calendar.MONTH, selectedMonth - 1);

        int year = selectedYear;
        int month = selectedMonth;

        String startDate = String.format(Locale.US, "%04d-%02d-01", year, month);
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        String endDate = String.format(Locale.US, "%04d-%02d-%02d", year, month, lastDay);

        RetrofitClient.getInstance().getReportApi().getSummary(userId, startDate, endDate)
                .enqueue(new Callback<ReportSummaryResponse>() {
                    @Override
                    public void onResponse(Call<ReportSummaryResponse> call, Response<ReportSummaryResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ReportSummaryResponse summary = response.body();
                            double net = summary.getCurrentBalance();
                            tvNetBalance.setText(formatVND(net));
                            tvTotalIncome.setText(formatVND(summary.getTotalIncome()));
                            tvTotalExpense.setText(formatVND(summary.getTotalExpense()));

                            com.expensetracker_manager.utils.OfflineCacheManager.getInstance(HomeActivity.this).cacheReportSummary(summary);

                            if (summary.getTotalIncome() > 0) {
                                double ratio = (summary.getTotalExpense() / summary.getTotalIncome()) * 100;
                                if (ratio > 80) {
                                    tvHabitsWarning.setText(String.format("Cảnh báo: Bạn đã tiêu xài đến %.1f%% thu nhập của tháng này!", ratio));
                                } else {
                                    tvHabitsWarning.setText(String.format("Tốt: Chi tiêu của bạn đang ở mức an toàn (%.1f%% thu nhập).", ratio));
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ReportSummaryResponse> call, Throwable t) {
                        com.expensetracker_manager.utils.OfflineCacheManager cache = com.expensetracker_manager.utils.OfflineCacheManager.getInstance(HomeActivity.this);
                        ReportSummaryResponse summary = cache.getCachedReportSummary();
                        double net = summary.getCurrentBalance() != 0 ? summary.getCurrentBalance() : (summary.getTotalIncome() - summary.getTotalExpense());
                        tvNetBalance.setText(formatVND(net));
                        tvTotalIncome.setText(formatVND(summary.getTotalIncome()));
                        tvTotalExpense.setText(formatVND(summary.getTotalExpense()));
                        tvHabitsWarning.setText("Offline Mode: Đang hiển thị dữ liệu lưu tạm.");
                    }
                });

        RetrofitClient.getInstance().getTransactionApi().getByUser(userId)
                .enqueue(new Callback<List<TransactionResponse>>() {
                    @Override
                    public void onResponse(Call<List<TransactionResponse>> call, Response<List<TransactionResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<TransactionResponse> allTransactions = response.body();
                            com.expensetracker_manager.utils.OfflineCacheManager.getInstance(HomeActivity.this).cacheTransactions(allTransactions);
                            transactionsList = filterTransactionsBySelectedMonth(allTransactions);

                            renderTransactions();
                            checkBudgets();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<TransactionResponse>> call, Throwable t) {
                        transactionsList = filterTransactionsBySelectedMonth(
                                com.expensetracker_manager.utils.OfflineCacheManager.getInstance(HomeActivity.this).getCachedTransactions()
                        );
                        renderTransactions();
                        checkBudgets();
                    }
                });
    }

    private void checkBudgets() {
        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            budgetList = com.expensetracker_manager.utils.OfflineCacheManager.getInstance(this).getCachedBudgets();
            evaluateBudgetStatus();
            return;
        }

        Long userId = TokenManager.getInstance(this).getUserId();
        if (userId == -1L) return;

        RetrofitClient.getInstance().getBudgetApi().getByUser(userId)
                .enqueue(new Callback<List<com.expensetracker_manager.model.response.BudgetResponse>>() {
                    @Override
                    public void onResponse(Call<List<com.expensetracker_manager.model.response.BudgetResponse>> call,
                            Response<List<com.expensetracker_manager.model.response.BudgetResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            budgetList = response.body();
                            com.expensetracker_manager.utils.OfflineCacheManager.getInstance(HomeActivity.this).cacheBudgets(budgetList);
                            evaluateBudgetStatus();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<com.expensetracker_manager.model.response.BudgetResponse>> call, Throwable t) {
                        budgetList = com.expensetracker_manager.utils.OfflineCacheManager.getInstance(HomeActivity.this).getCachedBudgets();
                        evaluateBudgetStatus();
                    }
                });
    }

    private void evaluateBudgetStatus() {
        if (budgetList.isEmpty() || transactionsList.isEmpty()) {
            cardBudgetWarning.setVisibility(View.GONE);
            return;
        }

        // Filter for current month's expenses only
        String curYearMonthPrefix = String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonth);

        java.util.Map<String, Double> expensesByCategory = new java.util.HashMap<>();
        for (TransactionResponse tr : transactionsList) {
            if ("EXPENSE".equalsIgnoreCase(tr.getType()) && tr.getTransactionDate() != null && tr.getTransactionDate().startsWith(curYearMonthPrefix)) {
                String catName = tr.getCategoryName();
                if (catName == null || catName.isEmpty()) {
                    catName = "Khác";
                }
                double amt = tr.getAmount();
                expensesByCategory.put(catName, expensesByCategory.getOrDefault(catName, 0.0) + amt);
            }
        }

        StringBuilder warningBuilder = new StringBuilder();
        for (com.expensetracker_manager.model.response.BudgetResponse b : budgetList) {
            String catName = b.getCategoryName();
            if (catName == null) continue;
            double spent = expensesByCategory.getOrDefault(catName, 0.0);
            double limit = b.getAmount();
            if (spent > limit) {
                double exceeded = spent - limit;
                if (warningBuilder.length() > 0) {
                    warningBuilder.append("\n");
                }
                warningBuilder.append("Cảnh báo: Danh mục ")
                        .append(catName)
                        .append(" vượt hạn mức ")
                        .append(formatVND(exceeded))
                        .append(" (Đã tiêu: ")
                        .append(formatVND(spent))
                        .append(" / Hạn mức: ")
                        .append(formatVND(limit))
                        .append(")");
            }
        }

        if (warningBuilder.length() > 0) {
            tvBudgetWarningMessage.setText(warningBuilder.toString());
            cardBudgetWarning.setVisibility(View.VISIBLE);
        } else {
            cardBudgetWarning.setVisibility(View.GONE);
        }
    }
    private List<TransactionResponse> filterTransactionsBySelectedMonth(List<TransactionResponse> source) {
        List<TransactionResponse> result = new ArrayList<>();

        if (source == null) {
            return result;
        }

        String monthPrefix = String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonth);

        for (TransactionResponse tr : source) {
            if (tr.getTransactionDate() != null && tr.getTransactionDate().startsWith(monthPrefix)) {
                result.add(tr);
            }
        }

        return result;
    }

    private void renderTransactions() {
        layoutTransactionsContainer.removeAllViews();

        int maxItems = Math.min(transactionsList.size(), 3);

        for (int i = 0; i < maxItems; i++) {
            TransactionResponse tr = transactionsList.get(i);
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

            layoutTransactionsContainer.addView(item);
        }
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

    private void renderOfflineMockTransactions() {
        layoutTransactionsContainer.removeAllViews();

        String[][] mocks = {
                {"Mua sắm siêu thị", "-350.000 ₫", "EXPENSE"},
                {"Nhận lương tháng 6", "+10.000.000 ₫", "INCOME"},
                {"Đóng tiền điện", "-1.200.000 ₫", "EXPENSE"}
        };

        for (String[] mock : mocks) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setPadding(12, 12, 12, 12);
            item.setBackgroundColor(0xFF1F1F35);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 8);
            item.setLayoutParams(params);

            TextView tvDesc = new TextView(this);
            tvDesc.setText(mock[0] + " (Offline)");
            tvDesc.setTextColor(0xFFFFFFFF);
            tvDesc.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            item.addView(tvDesc);

            TextView tvValue = new TextView(this);
            tvValue.setText(mock[1]);
            tvValue.setTextColor("INCOME".equals(mock[2]) ? 0xFF00FF66 : 0xFFFF3366);
            item.addView(tvValue);

            layoutTransactionsContainer.addView(item);
        }
    }

    private void simulateBankSync() {
        android.widget.EditText etPhone = new android.widget.EditText(this);
        etPhone.setHint("Nhập số điện thoại đăng ký Momo");
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        etPhone.setPadding(32, 32, 32, 32);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Kết nối Ví MoMo")
                .setView(etPhone)
                .setPositiveButton("Tiếp tục", (dialog, which) -> {
                    String phone = etPhone.getText().toString().trim();
                    if (phone.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showOtpDialog(phone);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showOtpDialog(String phone) {
        android.widget.EditText etOtp = new android.widget.EditText(this);
        etOtp.setHint("Nhập mã OTP (Mặc định: 123456)");
        etOtp.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etOtp.setPadding(32, 32, 32, 32);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác thực OTP MoMo")
                .setMessage("Mã OTP đã được gửi đến số " + phone)
                .setView(etOtp)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String otp = etOtp.getText().toString().trim();
                    if ("123456".equals(otp) || otp.length() == 6) {
                        importMomoTransactions();
                    } else {
                        Toast.makeText(this, "Mã OTP không đúng", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void importMomoTransactions() {
        Long userId = TokenManager.getInstance(this).getUserId();
        if (userId == -1L) return;

        Toast.makeText(this, "Đang đồng bộ giao dịch từ MoMo...", Toast.LENGTH_SHORT).show();

        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            com.expensetracker_manager.utils.OfflineCacheManager cache = com.expensetracker_manager.utils.OfflineCacheManager.getInstance(this);

            com.expensetracker_manager.model.response.ReportSummaryResponse summary = cache.getCachedReportSummary();
            summary.setTotalIncome(summary.getTotalIncome() + 500000);
            summary.setTotalExpense(summary.getTotalExpense() + 120000);
            summary.setCurrentBalance(summary.getCurrentBalance() + 380000);
            cache.cacheReportSummary(summary);

            List<TransactionResponse> txs = cache.getCachedTransactions();

            TransactionResponse newTx1 = new TransactionResponse();
            newTx1.setId(System.currentTimeMillis());
            newTx1.setDescription("Hoàn tiền từ MoMo");
            newTx1.setAmount(500000);
            newTx1.setType("INCOME");
            newTx1.setCategoryName("Thu nhập");
            newTx1.setTransactionDate(java.time.LocalDateTime.now().toString());
            txs.add(0, newTx1);

            TransactionResponse newTx2 = new TransactionResponse();
            newTx2.setId(System.currentTimeMillis() + 1);
            newTx2.setDescription("Thanh toán dịch vụ MoMo");
            newTx2.setAmount(120000);
            newTx2.setType("EXPENSE");
            newTx2.setCategoryName("Khác");
            newTx2.setTransactionDate(java.time.LocalDateTime.now().toString());
            txs.add(0, newTx2);

            cache.cacheTransactions(txs);

            Toast.makeText(HomeActivity.this, "Đồng bộ Ví MoMo thành công! Đã thêm 2 giao dịch (Offline).", Toast.LENGTH_LONG).show();
            loadDashboardData();
            return;
        }

        RetrofitClient.getInstance().getCategoryApi().getAll()
                .enqueue(new Callback<List<com.expensetracker_manager.model.response.CategoryResponse>>() {
                    @Override
                    public void onResponse(Call<List<com.expensetracker_manager.model.response.CategoryResponse>> call,
                            Response<List<com.expensetracker_manager.model.response.CategoryResponse>> response) {
                        long incomeCatId = 1;
                        long expenseCatId = 1;
                        if (response.isSuccessful() && response.body() != null) {
                            for (com.expensetracker_manager.model.response.CategoryResponse cat : response.body()) {
                                if ("Thu nhập".equalsIgnoreCase(cat.getName())) {
                                    incomeCatId = cat.getId();
                                } else if ("Khác".equalsIgnoreCase(cat.getName())) {
                                    expenseCatId = cat.getId();
                                }
                            }
                        }

                        com.expensetracker_manager.model.request.TransactionRequest req1 = new com.expensetracker_manager.model.request.TransactionRequest(
                                500000, "Hoàn tiền từ MoMo", java.time.LocalDateTime.now().toString(), "INCOME", userId, incomeCatId
                        );

                        com.expensetracker_manager.model.request.TransactionRequest req2 = new com.expensetracker_manager.model.request.TransactionRequest(
                                120000, "Thanh toán dịch vụ MoMo", java.time.LocalDateTime.now().toString(), "EXPENSE", userId, expenseCatId
                        );

                        RetrofitClient.getInstance().getTransactionApi().create(req1)
                                .enqueue(new Callback<TransactionResponse>() {
                                    @Override
                                    public void onResponse(Call<TransactionResponse> call, Response<TransactionResponse> response) {
                                        RetrofitClient.getInstance().getTransactionApi().create(req2)
                                                .enqueue(new Callback<TransactionResponse>() {
                                                    @Override
                                                    public void onResponse(Call<TransactionResponse> call, Response<TransactionResponse> response) {
                                                        Toast.makeText(HomeActivity.this, "Đồng bộ Ví MoMo thành công! Đã thêm 2 giao dịch.", Toast.LENGTH_LONG).show();
                                                        loadDashboardData();
                                                    }

                                                    @Override
                                                    public void onFailure(Call<TransactionResponse> call, Throwable t) {
                                                        loadDashboardData();
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onFailure(Call<TransactionResponse> call, Throwable t) {
                                        loadDashboardData();
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Call<List<com.expensetracker_manager.model.response.CategoryResponse>> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "Không thể lấy danh mục để lưu giao dịch MoMo.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void exportTransactionsToCSV() {
        try {
            String fileName = "Report_Transactions_" + System.currentTimeMillis() + ".csv";

            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            android.net.Uri uri = getContentResolver().insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
            );

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
            csv.append("ID,Date,Description,Amount,Type\n");

            if (!transactionsList.isEmpty()) {
                for (TransactionResponse tr : transactionsList) {
                    csv.append(tr.getId()).append(",")
                            .append("\"").append(formatTransactionDate(tr.getTransactionDate())).append("\"").append(",")
                            .append("\"").append(tr.getDescription() == null ? "" : tr.getDescription().replace("\"", "\"\"")).append("\"").append(",")
                            .append(tr.getAmount()).append(",")
                            .append(tr.getType()).append("\n");
                }
            } else {
                csv.append("1,\"Offline Transaction Sample\",500000,EXPENSE\n");
            }

            outputStream.write(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            outputStream.flush();
            outputStream.close();

            Toast.makeText(this, "Xuất CSV thành công! File nằm trong thư mục Tải về.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xuất file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
