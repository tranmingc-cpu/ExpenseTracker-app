package com.example.expensetracker_app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.expensetracker_manager.model.request.SavingGoalRequest;
import com.expensetracker_manager.model.response.SavingGoalResponse;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.utils.TokenManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SavingsGoalsActivity extends BaseActivity {

    private EditText etGoalName, etGoalTarget, etGoalCurrent;
    private Button btnSaveGoal;
    private LinearLayout layoutGoalsContainer;

    private List<SavingGoalResponse> goals = new ArrayList<>();

    private double currentBalance = 0;
    private List<Long> alertedGoalIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savings_goals);

        etGoalName = findViewById(R.id.etGoalName);
        etGoalTarget = findViewById(R.id.etGoalTarget);
        etGoalCurrent = findViewById(R.id.etGoalCurrent);
        btnSaveGoal = findViewById(R.id.btnSaveGoal);
        layoutGoalsContainer = findViewById(R.id.layoutGoalsContainer);
        
        etGoalTarget.addTextChangedListener(new com.expensetracker_manager.utils.NumberTextWatcher(etGoalTarget));

        fetchCurrentBalanceAndLoadGoals();

        btnSaveGoal.setOnClickListener(v -> saveGoal());
    }

    private void fetchCurrentBalanceAndLoadGoals() {
        Long userId = TokenManager.getInstance(this).getUserId();
        if (userId == -1L) return;

        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            com.expensetracker_manager.model.response.ReportSummaryResponse cached =
                    com.expensetracker_manager.utils.OfflineCacheManager.getInstance(this).getCachedReportSummary();
            currentBalance = cached.getCurrentBalance() != 0 ? cached.getCurrentBalance() : (cached.getTotalIncome() - cached.getTotalExpense());
            etGoalCurrent.setText(formatVND(currentBalance));
            loadGoals();
            return;
        }

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        String startDate = String.format(java.util.Locale.US, "%04d-%02d-01", year, month);
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        String endDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month, lastDay);

        RetrofitClient.getInstance().getReportApi().getSummary(userId, startDate, endDate)
                .enqueue(new Callback<com.expensetracker_manager.model.response.ReportSummaryResponse>() {
                    @Override
                    public void onResponse(Call<com.expensetracker_manager.model.response.ReportSummaryResponse> call, Response<com.expensetracker_manager.model.response.ReportSummaryResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            com.expensetracker_manager.model.response.ReportSummaryResponse summary = response.body();
                            currentBalance = summary.getCurrentBalance();
                            com.expensetracker_manager.utils.OfflineCacheManager.getInstance(SavingsGoalsActivity.this).cacheReportSummary(summary);
                        } else {
                            currentBalance = 3500000; // Offline fallback
                        }
                        etGoalCurrent.setText(formatVND(currentBalance));
                        loadGoals();
                    }

                    @Override
                    public void onFailure(Call<com.expensetracker_manager.model.response.ReportSummaryResponse> call, Throwable t) {
                        currentBalance = 3500000; // Offline fallback
                        etGoalCurrent.setText(formatVND(currentBalance));
                        loadGoals();
                    }
                });
    }

    private void loadGoals() {
        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            goals = com.expensetracker_manager.utils.OfflineCacheManager.getInstance(this).getCachedSavingGoals();
            renderGoalsList();
            return;
        }

        Long userId = TokenManager.getInstance(this).getUserId();
        RetrofitClient.getInstance().getSavingGoalApi().getByUser(userId)
                .enqueue(new Callback<List<SavingGoalResponse>>() {
                    @Override
                    public void onResponse(Call<List<SavingGoalResponse>> call, Response<List<SavingGoalResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            goals = response.body();
                            com.expensetracker_manager.utils.OfflineCacheManager.getInstance(SavingsGoalsActivity.this).cacheSavingGoals(goals);
                            renderGoalsList();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<SavingGoalResponse>> call, Throwable t) {
                        goals = com.expensetracker_manager.utils.OfflineCacheManager.getInstance(SavingsGoalsActivity.this).getCachedSavingGoals();
                        renderGoalsList();
                    }
                });
    }

    private void renderGoalsList() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        layoutGoalsContainer.removeAllViews();

        for (SavingGoalResponse g : goals) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(16, 16, 16, 16);
            item.setBackgroundColor(0xFF2A2A3E);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            item.setLayoutParams(params);

            TextView tvName = new TextView(this);
            double target = g.getTargetAmount().doubleValue();
            boolean isCompleted = g.isCompleted() || currentBalance >= target;
            String nameText = g.getName();
            if (isCompleted) {
                nameText += " [Đã hoàn thành]";
                tvName.setTextColor(0xFF00FF66);
            } else {
                tvName.setTextColor(0xFFFFFFFF);
            }
            tvName.setText(nameText);
            tvName.setTextSize(16);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            item.addView(tvName);

            double current = currentBalance;
            double pct = target > 0 ? (current / target) * 100 : 0;
            if (pct > 100) pct = 100;

            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(100);
            pb.setProgress((int) pct);
            pb.getProgressDrawable().setColorFilter(0xFF00FF66, android.graphics.PorterDuff.Mode.SRC_IN);
            pb.setPadding(0, 8, 0, 8);
            item.addView(pb);

            TextView tvProgress = new TextView(this);
            tvProgress.setText(String.format("Tích lũy: %s / Mục tiêu: %s (%.1f%%)", formatVND(current), formatVND(target), pct));
            tvProgress.setTextColor(0xFF8A8A9E);
            tvProgress.setTextSize(12);
            item.addView(tvProgress);

            layoutGoalsContainer.addView(item);

            if (current >= target && !alertedGoalIds.contains(g.getId())) {
                alertedGoalIds.add(g.getId());
                showCompletionDialog(g.getName(), current);
            }
        }
    }

    private void showCompletionDialog(String goalName, double currentAmount) {
        if (!isFinishing() && !isDestroyed()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("🎉 Chúc mừng bạn!")
                    .setMessage("Bạn đã hoàn thành xuất sắc mục tiêu tiết kiệm: \"" + goalName + "\" với số dư hiện tại " + formatVND(currentAmount) + "!")
                    .setPositiveButton("Tuyệt vời", null)
                    .show();
        }
    }

    private void renderOfflineMockGoals() {
        layoutGoalsContainer.removeAllViews();

        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(16, 16, 16, 16);
        item.setBackgroundColor(0xFF2A2A3E);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
        params.setMargins(0, 0, 0, 16);
        item.setLayoutParams(params);

        TextView tvName = new TextView(this);
        tvName.setText("Quỹ mua xe máy (Offline Demo)");
        tvName.setTextColor(0xFFFFFFFF);
        tvName.setTextSize(16);
        item.addView(tvName);

        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgress(60);
        pb.getProgressDrawable().setColorFilter(0xFF00FF66, android.graphics.PorterDuff.Mode.SRC_IN);
        pb.setPadding(0, 8, 0, 8);
        item.addView(pb);

        TextView tvProgress = new TextView(this);
        tvProgress.setText("Tích lũy: " + formatVND(30000000) + " / Mục tiêu: " + formatVND(50000000) + " (60.0%)");
        tvProgress.setTextColor(0xFF8A8A9E);
        item.addView(tvProgress);

        layoutGoalsContainer.addView(item);
    }

    private void saveGoal() {
        String name = etGoalName.getText().toString().trim();
        String targetStr = etGoalTarget.getText().toString().trim().replace(".", "");

        if (name.isEmpty() || targetStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền tên mục tiêu và số tiền tích lũy", Toast.LENGTH_SHORT).show();
            return;
        }

        BigDecimal targetVal = new BigDecimal(targetStr);

        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            SavingGoalResponse mockGoal = new SavingGoalResponse();
            mockGoal.setId((long) (goals.size() + 1));
            mockGoal.setName(name);
            mockGoal.setTargetAmount(targetVal);
            mockGoal.setCurrentAmount(new BigDecimal(currentBalance));
            mockGoal.setCompleted(currentBalance >= targetVal.doubleValue());
            goals.add(mockGoal);
            com.expensetracker_manager.utils.OfflineCacheManager.getInstance(this).cacheSavingGoals(goals);
            com.expensetracker_manager.service.FinancialAnalysisEngine.analyze(this);
            Toast.makeText(this, "Đã lưu mục tiêu (Offline)", Toast.LENGTH_SHORT).show();
            etGoalName.setText("");
            etGoalTarget.setText("");
            renderGoalsList();
            return;
        }

        SavingGoalRequest request = new SavingGoalRequest();
        request.setName(name);
        request.setTargetAmount(targetVal);
        request.setCurrentAmount(new BigDecimal(currentBalance));
        request.setUserId(TokenManager.getInstance(this).getUserId());

        RetrofitClient.getInstance().getSavingGoalApi().create(request)
                .enqueue(new Callback<SavingGoalResponse>() {
                    @Override
                    public void onResponse(Call<SavingGoalResponse> call, Response<SavingGoalResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(SavingsGoalsActivity.this, "Thêm mục tiêu thành công!", Toast.LENGTH_SHORT).show();
                            com.expensetracker_manager.service.FinancialAnalysisEngine.analyze(SavingsGoalsActivity.this);
                            etGoalName.setText("");
                            etGoalTarget.setText("");
                            fetchCurrentBalanceAndLoadGoals();
                        } else {
                            Toast.makeText(SavingsGoalsActivity.this, "Không thể lưu mục tiêu.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<SavingGoalResponse> call, Throwable t) {
                        Toast.makeText(SavingsGoalsActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
