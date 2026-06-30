package com.example.expensetracker_app;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.expensetracker_manager.model.request.BudgetRequest;
import com.expensetracker_manager.model.response.BudgetResponse;
import com.expensetracker_manager.model.response.CategoryResponse;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.TokenManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BudgetsActivity extends BaseActivity {

    private Spinner spinnerBudgetCategory;
    private EditText etBudgetAmount;
    private Button btnSaveBudget;
    private LinearLayout layoutBudgetsContainer;

    private List<CategoryResponse> categories = new ArrayList<>();
    private List<BudgetResponse> budgets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budgets);

        spinnerBudgetCategory = findViewById(R.id.spinnerBudgetCategory);
        etBudgetAmount = findViewById(R.id.etBudgetAmount);
        btnSaveBudget = findViewById(R.id.btnSaveBudget);
        layoutBudgetsContainer = findViewById(R.id.layoutBudgetsContainer);

        etBudgetAmount.addTextChangedListener(new com.expensetracker_manager.utils.NumberTextWatcher(etBudgetAmount));

        findViewById(R.id.btnAiPlanner).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, AiBudgetPlannerActivity.class));
        });

        loadCategories();
        loadBudgets();

        btnSaveBudget.setOnClickListener(v -> saveBudget());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBudgets();
    }

    private void loadCategories() {
        RetrofitClient.getInstance().getCategoryApi().getAll()
                .enqueue(new Callback<List<CategoryResponse>>() {
                    @Override
                    public void onResponse(Call<List<CategoryResponse>> call,
                                           Response<List<CategoryResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            categories = response.body();
                            List<String> names = new ArrayList<>();
                            for (CategoryResponse cat : categories) {
                                if (!names.contains(cat.getName())) {
                                    names.add(cat.getName());
                                }
                            }
                            if (!names.contains("Chuyển khoản")) {
                                names.add("Chuyển khoản");
                            }
                            if (names.isEmpty()) {
                                names.add("Ăn uống");
                                names.add("Đi lại");
                                names.add("Quần áo");
                                names.add("Chi ngoài");
                                names.add("Y tế");
                                names.add("Chuyển khoản");
                                names.add("Khác");
                            }
                            ArrayAdapter<String> catAdapter = createSpinnerAdapter(names);
                            spinnerBudgetCategory.setAdapter(catAdapter);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<CategoryResponse>> call, Throwable t) {
                        List<String> names = new ArrayList<>();
                        names.add("Ăn uống");
                        names.add("Đi lại");
                        names.add("Quần áo");
                        names.add("Chi ngoài");
                        names.add("Y tế");
                        names.add("Chuyển khoản");
                        names.add("Khác");
                        ArrayAdapter<String> catAdapter = createSpinnerAdapter(names);
                        spinnerBudgetCategory.setAdapter(catAdapter);
                    }
                });
    }

    private void loadBudgets() {
        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            budgets = com.expensetracker_manager.utils.OfflineCacheManager.getInstance(this).getCachedBudgets();
            renderBudgetsList();
            return;
        }

        Long userId = TokenManager.getInstance(this).getUserId();
        RetrofitClient.getInstance().getBudgetApi().getByUser(userId)
                .enqueue(new Callback<List<BudgetResponse>>() {
                    @Override
                    public void onResponse(Call<List<BudgetResponse>> call, Response<List<BudgetResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            budgets = response.body();
                            com.expensetracker_manager.utils.OfflineCacheManager.getInstance(BudgetsActivity.this).cacheBudgets(budgets);
                            renderBudgetsList();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<BudgetResponse>> call, Throwable t) {
                        budgets = com.expensetracker_manager.utils.OfflineCacheManager.getInstance(BudgetsActivity.this).getCachedBudgets();
                        renderBudgetsList();
                    }
                });
    }

    private void renderBudgetsList() {
        layoutBudgetsContainer.removeAllViews();

        for (BudgetResponse b : budgets) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(16, 16, 16, 16);
            item.setBackground(roundedBg(R.color.app_surface_alt));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 16);
            item.setLayoutParams(params);

            TextView tvCategory = new TextView(this);
            tvCategory.setText(b.getCategoryName() != null ? b.getCategoryName() : "Danh mục");
            tvCategory.setTextColor(themeColor(R.color.app_text_primary));
            tvCategory.setTextSize(16);
            tvCategory.setTypeface(null, android.graphics.Typeface.BOLD);
            item.addView(tvCategory);

            double spent = b.getSpent();
            double limit = b.getAmount();
            double pct = limit > 0 ? (spent / limit) * 100 : 0;
            if (pct > 100) pct = 100;

            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(100);
            pb.setProgress((int) pct);
            pb.getProgressDrawable().setColorFilter(
                    pct >= 100 ? themeColor(R.color.app_accent_expense) : (pct >= 80 ? themeColor(R.color.app_accent_warning) : themeColor(R.color.app_accent_income)),
                    android.graphics.PorterDuff.Mode.SRC_IN);
            pb.setPadding(0, 8, 0, 8);
            item.addView(pb);

            TextView tvProgress = new TextView(this);
            tvProgress.setText("Đã tiêu: " + formatVND(spent) + " / Giới hạn: " + formatVND(limit) + " (" + String.format(java.util.Locale.US, "%.0f", pct) + "%)");
            tvProgress.setTextColor(themeColor(R.color.app_text_secondary));
            tvProgress.setTextSize(12);
            item.addView(tvProgress);

            if (pct >= 80) {
                TextView tvWarning = new TextView(this);
                tvWarning.setText(
                        pct >= 100 ? "⚠️ Cảnh báo: VƯỢT HẠN MỨC 100%!" : "⚠️ Cảnh báo: Đã dùng hơn 80% ngân sách!");
                tvWarning.setTextColor(pct >= 100 ? themeColor(R.color.app_accent_expense) : themeColor(R.color.app_accent_warning));
                tvWarning.setTextSize(11);
                tvWarning.setPadding(0, 4, 0, 0);
                item.addView(tvWarning);
            }

            layoutBudgetsContainer.addView(item);
        }
    }

    private void renderOfflineMockBudgets() {
        layoutBudgetsContainer.removeAllViews();
        // Thêm một mục ngân sách mô phỏng
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(16, 16, 16, 16);
        item.setBackground(roundedBg(R.color.app_surface_alt));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        item.setLayoutParams(params);

        TextView tvCategory = new TextView(this);
        tvCategory.setText("Ăn uống (Offline Mode)");
        tvCategory.setTextColor(themeColor(R.color.app_text_primary));
        tvCategory.setTextSize(16);
        item.addView(tvCategory);

        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgress(85);
        pb.getProgressDrawable().setColorFilter(themeColor(R.color.app_accent_warning), android.graphics.PorterDuff.Mode.SRC_IN);
        pb.setPadding(0, 8, 0, 8);
        item.addView(pb);

        TextView tvProgress = new TextView(this);
        tvProgress.setText("Đã tiêu: " + formatVND(4250000) + " / Giới hạn: " + formatVND(5000000) + " (85%)");
        tvProgress.setTextColor(themeColor(R.color.app_text_secondary));
        item.addView(tvProgress);

        TextView tvWarning = new TextView(this);
        tvWarning.setText("⚠️ Cảnh báo: Đã dùng hơn 80% ngân sách!");
        tvWarning.setTextColor(themeColor(R.color.app_accent_warning));
        tvWarning.setTextSize(11);
        item.addView(tvWarning);

        layoutBudgetsContainer.addView(item);
    }

    private void saveBudget() {
        String amountStr = etBudgetAmount.getText().toString().trim().replace(".", "");
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập giới hạn số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            // Lưu cục bộ vào bộ nhớ đệm ngoại tuyến
            BudgetResponse mockBudget = new BudgetResponse();
            mockBudget.setId(System.currentTimeMillis());
            mockBudget.setAmount(amount);
            mockBudget.setMonth(Calendar.getInstance().get(Calendar.MONTH) + 1);
            mockBudget.setYear(Calendar.getInstance().get(Calendar.YEAR));
            if (spinnerBudgetCategory.getSelectedItem() != null) {
                mockBudget.setCategoryName(spinnerBudgetCategory.getSelectedItem().toString());
            } else {
                mockBudget.setCategoryName("Khác");
            }
            mockBudget.setSpent(0); // mức chi tiêu mô phỏng ban đầu khi ngoại tuyến
            budgets.add(mockBudget);
            com.expensetracker_manager.utils.OfflineCacheManager.getInstance(this).cacheBudgets(budgets);

            Toast.makeText(this, "Thiết lập ngân sách thành công (Offline)!", Toast.LENGTH_SHORT).show();
            etBudgetAmount.setText("");
            renderBudgetsList();
            return;
        }

        BudgetRequest request = new BudgetRequest();
        request.setAmount(amount);
        request.setMonth(Calendar.getInstance().get(Calendar.MONTH) + 1);
        request.setYear(Calendar.getInstance().get(Calendar.YEAR));
        request.setUserId(TokenManager.getInstance(this).getUserId());

        if (!categories.isEmpty()) {
            int selectedCatPos = spinnerBudgetCategory.getSelectedItemPosition();
            if (selectedCatPos >= 0 && selectedCatPos < categories.size()) {
                request.setCategoryId(categories.get(selectedCatPos).getId());
            }
        }

        RetrofitClient.getInstance().getBudgetApi().create(request)
                .enqueue(new Callback<BudgetResponse>() {
                    @Override
                    public void onResponse(Call<BudgetResponse> call, Response<BudgetResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(BudgetsActivity.this, "Thiết lập ngân sách thành công!", Toast.LENGTH_SHORT)
                                    .show();
                            etBudgetAmount.setText("");
                            loadBudgets();
                        } else {
                            Toast.makeText(BudgetsActivity.this, "Không thể lưu ngân sách.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BudgetResponse> call, Throwable t) {
                        Toast.makeText(BudgetsActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    private ArrayAdapter<String> createSpinnerAdapter(List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.widget.TextView view = (android.widget.TextView) super.getView(position, convertView, parent);
                view.setTextColor(themeColor(R.color.app_text_primary));
                view.setPadding(dp(12), 0, dp(12), 0);
                return view;
            }

            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.widget.TextView view = (android.widget.TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(themeColor(R.color.app_text_primary));
                view.setBackgroundColor(themeColor(R.color.app_surface));
                view.setPadding(dp(12), dp(12), dp(12), dp(12));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
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