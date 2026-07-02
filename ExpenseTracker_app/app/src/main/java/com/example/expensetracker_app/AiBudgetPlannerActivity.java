package com.example.expensetracker_app;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.expensetracker_manager.model.response.BudgetResponse;
import com.expensetracker_manager.model.response.CategoryResponse;
import com.expensetracker_manager.model.request.BudgetRequest;
import com.expensetracker_manager.network.RetrofitClient;
import com.expensetracker_manager.service.AIInsightGenerator;
import com.expensetracker_manager.service.FinancialAnalysisEngine;
import com.expensetracker_manager.utils.OfflineCacheManager;
import com.expensetracker_manager.utils.TokenManager;
import com.expensetracker_manager.model.response.AiAnalysisResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class AiBudgetPlannerActivity extends BaseActivity {

    private TextView tvHealthScore, tvHealthStatus;
    private ProgressBar pbHealth;
    private TextView tvRiskStatus;
    private TextView tvCurrentSpent, tvPredictedSpent, tvRemainingBudget;
    private TextView tvGoalName, tvGoalProgress, tvGoalPercent;
    private ProgressBar pbGoal;
    private TextView tvRemainingDays, tvCompletionDate;
    private LinearLayout layoutRecommendations;
    private LinearLayout layoutSuggestedBudgets;
    private Button btnApplyAll;
    private FinancialAnalysisEngine.AnalysisResult analysis;
    private List<AIInsightGenerator.Recommendation> recommendations;
    private Long selectedGoalId = null;
    private int selectedMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
    private int selectedYear = Calendar.getInstance().get(Calendar.YEAR);
    private List<CategoryResponse> categories = new ArrayList<>();
    private List<BudgetResponse> backendBudgets = new ArrayList<>();
    private List<AiAnalysisResponse.Insight> aiInsights = new ArrayList<>();
    private List<AiAnalysisResponse.BudgetSuggestion> aiBudgetSuggestions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ai_budget_planner);
        tvHealthScore = findViewById(R.id.tvHealthScore);
        tvHealthStatus = findViewById(R.id.tvHealthStatus);
        pbHealth = findViewById(R.id.pbHealth);
        tvRiskStatus = findViewById(R.id.tvRiskStatus);
        tvCurrentSpent = findViewById(R.id.tvCurrentSpent);
        tvPredictedSpent = findViewById(R.id.tvPredictedSpent);
        tvRemainingBudget = findViewById(R.id.tvRemainingBudget);

        tvGoalName = findViewById(R.id.tvGoalName);
        tvGoalProgress = findViewById(R.id.tvGoalProgress);
        tvGoalPercent = findViewById(R.id.tvGoalPercent);
        pbGoal = findViewById(R.id.pbGoal);
        tvRemainingDays = findViewById(R.id.tvRemainingDays);
        tvCompletionDate = findViewById(R.id.tvCompletionDate);

        layoutRecommendations = findViewById(R.id.layoutRecommendations);
        layoutSuggestedBudgets = findViewById(R.id.layoutSuggestedBudgets);
        btnApplyAll = findViewById(R.id.btnApplyAll);

        refreshData();
        loadCategoriesAndBudgets();

        btnApplyAll.setOnClickListener(v -> applyAllRecommendations());

        TextView tvViewAllRecommendations = findViewById(R.id.tvViewAllRecommendations);
        if (tvViewAllRecommendations != null) {
            tvViewAllRecommendations.setOnClickListener(v -> showAllRecommendationsDialog());
        }

        TextView tvChangeGoal = findViewById(R.id.tvChangeGoal);
        if (tvChangeGoal != null) {
            tvChangeGoal.setOnClickListener(v -> showGoalSelectionDialog());
        }
    }

    private void loadCategoriesAndBudgets() {
        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            return;
        }
        RetrofitClient.getInstance().getCategoryApi().getAll()
                .enqueue(new Callback<List<CategoryResponse>>() {
                    @Override
                    public void onResponse(Call<List<CategoryResponse>> call,
                            Response<List<CategoryResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            categories = response.body();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<CategoryResponse>> call, Throwable t) {
                    }
                });
        long userId = TokenManager.getInstance(this).getUserId();
        RetrofitClient.getInstance().getBudgetApi().getByUser(userId, null, null)
                .enqueue(new Callback<List<BudgetResponse>>() {
                    @Override
                    public void onResponse(Call<List<BudgetResponse>> call, Response<List<BudgetResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            backendBudgets = response.body();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<BudgetResponse>> call, Throwable t) {
                    }
                });
    }

    /**
     * Gọi Gemini backend để lấy AI analysis và chuyển thành danh sách khuyến nghị.
     * Kết quả sẽ gán vào biến recommendations để UI render.
     */
    private void loadGeminiInsights(Long userId) {
        if (userId == -1L) return;
        if (!com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            return;
        }
        RetrofitClient.getInstance().getAnalyticsApi()
                .getBudgetAnalysis(userId, selectedMonth, selectedYear)
                .enqueue(new Callback<AiAnalysisResponse>() {
                    @Override
                    public void onResponse(Call<AiAnalysisResponse> call, Response<AiAnalysisResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            AiAnalysisResponse ai = response.body();

                            // Luồng 1: insights (cảnh báo – chỉ category có budget)
                            List<AIInsightGenerator.Recommendation> geminiRecs = new ArrayList<>();
                            List<AiAnalysisResponse.Insight> tempInsights = new ArrayList<>();
                            if (ai.getInsights() != null) {
                                for (AiAnalysisResponse.Insight ins : ai.getInsights()) {
                                    double recLimit = (ins.getRecommendedLimit() != null) ? ins.getRecommendedLimit() : 0;

                                    // Tiêu đề card hiển thị rõ mức độ và danh mục
                                    String risk = ins.getRisk() != null ? ins.getRisk() : "LOW";
                                    String titleEmoji;
                                    String riskLabel;
                                    switch (risk.toUpperCase()) {
                                        case "HIGH":
                                            titleEmoji = "🔴";
                                            riskLabel  = "Cần giảm ngay";
                                            break;
                                        case "MEDIUM":
                                            titleEmoji = "🟡";
                                            riskLabel  = "Cần kiểm soát";
                                            break;
                                        default:
                                            titleEmoji = "🟢";
                                            riskLabel  = "Đang tốt";
                                            break;
                                    }
                                    String cardTitle = titleEmoji + " " + ins.getCategory() + " — " + riskLabel;

                                    AIInsightGenerator.Recommendation r = new AIInsightGenerator.Recommendation(
                                            cardTitle,
                                            ins.getMessage(),   // message = nội dung khuyến nghị hành động từ AI
                                            ins.getCategory(),
                                            0,
                                            recLimit,
                                            "Hạn mức đề xuất: " + (recLimit > 0 ? String.format("%,.0fđ", recLimit) : "Không đổi"),
                                            "Phân tích bởi AI · Mức rủi ro: " + risk
                                    );
                                    geminiRecs.add(r);
                                    tempInsights.add(ins);
                                }
                            }
                            recommendations = geminiRecs;
                            aiInsights = tempInsights;

                            // Luồng 2: budgetSuggestions (gợi ý hạn mức – tất cả category có chi tiêu)
                            aiBudgetSuggestions = (ai.getBudgetSuggestions() != null)
                                    ? ai.getBudgetSuggestions()
                                    : new ArrayList<>();

                            runOnUiThread(() -> {
                                renderRecommendations();
                                renderSuggestedBudgets();
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<AiAnalysisResponse> call, Throwable t) {
                        // Khi lỗi, giữ nguyên danh sách hiện tại
                    }
                });
    }

    private void refreshData() {
        analysis = FinancialAnalysisEngine.analyze(this, selectedGoalId);
        // Gọi Gemini để lấy phân tích AI và đề xuất ngân sách
        loadGeminiInsights(TokenManager.getInstance(this).getUserId());

        renderHealthScorecard();
        renderRiskAndForecast();
        renderSavingsGoal();
        // renderRecommendations() sẽ được gọi trong callback của Gemini
        renderSuggestedBudgets();
    }

    private void showGoalSelectionDialog() {
        List<com.expensetracker_manager.model.response.SavingGoalResponse> goals = OfflineCacheManager.getInstance(this)
                .getCachedSavingGoals();
        if (goals == null || goals.isEmpty()) {
            Toast.makeText(this, "Không có mục tiêu nào.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] goalNames = new String[goals.size()];
        for (int i = 0; i < goals.size(); i++) {
            goalNames[i] = goals.get(i).getName() + (goals.get(i).isCompleted() ? " (Đã HT)" : "");
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn mục tiêu phân tích")
                .setItems(goalNames, (dialog, which) -> {
                    selectedGoalId = goals.get(which).getId();
                    Toast.makeText(this, "Đã đổi mục tiêu phân tích: " + goals.get(which).getName(), Toast.LENGTH_SHORT)
                            .show();
                    refreshData();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void renderHealthScorecard() {
        tvHealthScore.setText(String.valueOf(analysis.financialHealthScore));
        tvHealthStatus.setText(getHealthStatusString(analysis.financialHealth));
        pbHealth.setProgress(analysis.financialHealthScore);

        // Dynamic colors
        int color;
        if (analysis.financialHealthScore >= 80) {
            color = 0xFF00FF66;
        } else if (analysis.financialHealthScore >= 60) {
            color = 0xFF00E5FF;
        } else if (analysis.financialHealthScore >= 40) {
            color = 0xFFFFCC00;
        } else {
            color = 0xFFFF3366;
        }
        tvHealthScore.setTextColor(color);
        pbHealth.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
    }

    private void renderRiskAndForecast() {
        tvRiskStatus.setText(analysis.overspendingRisk.toUpperCase());
        int riskColor;
        int riskBg;
        if ("HIGH".equalsIgnoreCase(analysis.overspendingRisk)) {
            riskColor = 0xFFFF3366;
            riskBg = 0x3DFF3366;
        } else if ("MEDIUM".equalsIgnoreCase(analysis.overspendingRisk)) {
            riskColor = 0xFFFFCC00;
            riskBg = 0x3DFFCC00;
        } else {
            riskColor = 0xFF00FF66;
            riskBg = 0x153D2A;
        }
        tvRiskStatus.setTextColor(riskColor);
        tvRiskStatus.setBackgroundColor(riskBg);

        tvCurrentSpent.setText(formatVND(analysis.totalExpense));
        tvPredictedSpent.setText(formatVND(analysis.predictedEndOfMonthSpending));
        tvRemainingBudget.setText(formatVND(analysis.remainingBudget));
    }

    private void renderSavingsGoal() {
        tvGoalName.setText(analysis.primaryGoalName);
        tvGoalProgress.setText(formatVND(analysis.goalCurrent) + " / " + formatVND(analysis.goalTarget));
        tvGoalPercent.setText(String.format(Locale.US, "%.1f%%", analysis.goalProgressPct));
        pbGoal.setProgress((int) Math.min(100, analysis.goalProgressPct));

        if (analysis.remainingDays >= 0) {
            tvRemainingDays.setText(analysis.remainingDays + " ngày");
            tvCompletionDate.setText(analysis.estimatedCompletionDate);
        } else {
            tvRemainingDays.setText("N/A");
            tvCompletionDate.setText(analysis.estimatedCompletionDate);
        }
    }

    private void renderRecommendations() {
        layoutRecommendations.removeAllViews();

        // Nếu chưa có budget → dùng budgetSuggestions làm khuyến nghị
        List<AIInsightGenerator.Recommendation> displayRecs = new ArrayList<>();
        if (recommendations != null && !recommendations.isEmpty()) {
            displayRecs.addAll(recommendations);
        } else if (aiBudgetSuggestions != null && !aiBudgetSuggestions.isEmpty()) {
            // Chuyển budgetSuggestions → Recommendation để hiển thị chung layout
            for (AiAnalysisResponse.BudgetSuggestion sug : aiBudgetSuggestions) {
                double recLimit = sug.getRecommendedLimit() != null ? sug.getRecommendedLimit() : 0;
                if (recLimit <= 0) continue;
                String reason = (sug.getReason() != null && !sug.getReason().isEmpty())
                        ? sug.getReason()
                        : "Dựa trên lịch sử chi tiêu của bạn.";
                String msg = "💡 " + reason + "\n➡️ Đề xuất đặt hạn mức: " + String.format("%,.0fđ", recLimit);
                displayRecs.add(new AIInsightGenerator.Recommendation(
                        "💡 " + sug.getCategory() + " — Gợi ý hạn mức",
                        msg,
                        sug.getCategory(),
                        0,
                        recLimit,
                        "Hạn mức đề xuất: " + String.format("%,.0fđ", recLimit),
                        "Phân tích bởi AI · Chưa có hạn mức"
                ));
            }
        }

        if (displayRecs.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Chưa có dữ liệu chi tiêu để phân tích.");
            tvEmpty.setTextColor(0xFF808090);
            tvEmpty.setPadding(16, 16, 16, 16);
            layoutRecommendations.addView(tvEmpty);
            return;
        }


        for (AIInsightGenerator.Recommendation rec : displayRecs) {
            View recView = LayoutInflater.from(this).inflate(R.layout.item_ai_recommendation, layoutRecommendations,
                    false);

            TextView tvTitle = recView.findViewById(R.id.tvRecTitle);
            TextView tvDesc = recView.findViewById(R.id.tvRecDesc);
            TextView tvDetails = recView.findViewById(R.id.tvRecDetails);
            Button btnAction = recView.findViewById(R.id.btnAction);
            Button btnIgnore = recView.findViewById(R.id.btnIgnore);

            tvTitle.setText(rec.title);
            tvDesc.setText(rec.description);
            tvDetails.setText(rec.explanation + "\n\nHiệu quả: " + rec.estimatedImprovement);

            if (rec.recommendedLimit > 0) {
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setOnClickListener(v -> {
                    applyBudgetChange(rec.category, rec.recommendedLimit);
                    Toast.makeText(this, "Đã áp dụng ngân sách đề xuất cho " + rec.category, Toast.LENGTH_SHORT).show();
                    refreshData();
                });
            } else {
                btnAction.setVisibility(View.GONE);
            }
            btnIgnore.setOnClickListener(v -> {
                layoutRecommendations.removeView(recView);
                Toast.makeText(this, "Đã bỏ qua khuyến nghị", Toast.LENGTH_SHORT).show();
            });
            layoutRecommendations.addView(recView);
        }
    }

    private void showAllRecommendationsDialog() {
        if (analysis == null) return;
        // Sử dụng danh sách khuyến nghị đã được tải từ Gemini AI
        List<AIInsightGenerator.Recommendation> allRecs = recommendations != null ? recommendations : new ArrayList<>();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tất cả Khuyến nghị");

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        if (allRecs.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Hiện tại không có khuyến nghị nào.");
            layout.addView(tvEmpty);
        } else {
            for (AIInsightGenerator.Recommendation rec : allRecs) {
                View recView = LayoutInflater.from(this).inflate(R.layout.item_ai_recommendation, layout, false);

                TextView tvTitle = recView.findViewById(R.id.tvRecTitle);
                TextView tvDesc = recView.findViewById(R.id.tvRecDesc);
                TextView tvDetails = recView.findViewById(R.id.tvRecDetails);
                Button btnAction = recView.findViewById(R.id.btnAction);
                Button btnIgnore = recView.findViewById(R.id.btnIgnore);

                tvTitle.setText(rec.title);
                tvDesc.setText(rec.description);
                tvDetails.setText(rec.explanation + "\n\nHiệu quả: " + rec.estimatedImprovement);

                if (rec.recommendedLimit > 0) {
                    btnAction.setVisibility(View.VISIBLE);
                    btnAction.setOnClickListener(v -> {
                        applyBudgetChange(rec.category, rec.recommendedLimit);
                        Toast.makeText(this, "Đã áp dụng ngân sách đề xuất cho " + rec.category, Toast.LENGTH_SHORT)
                                .show();
                        refreshData();
                    });
                } else {
                    btnAction.setVisibility(View.GONE);
                }

                btnIgnore.setVisibility(View.GONE); // Ẩn nút bỏ qua trong dialog để đơn giản hoá UI

                layout.addView(recView);
            }
        }

        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void renderSuggestedBudgets() {
        layoutSuggestedBudgets.removeAllViews();

        // Ưu tiên dùng budgetSuggestions từ Gemini (tất cả category có chi tiêu)
        if (aiBudgetSuggestions != null && !aiBudgetSuggestions.isEmpty()) {
            for (AiAnalysisResponse.BudgetSuggestion sug : aiBudgetSuggestions) {
                double recLimit = (sug.getRecommendedLimit() != null) ? sug.getRecommendedLimit() : 0;
                if (recLimit <= 0) continue;

                String cat = sug.getCategory();
                double currentLimit = analysis.categoryBudgets.getOrDefault(cat, 0.0);
                // Dùng spentSoFar từ server (tránh mismatch tên với local cache)
                double spent = (sug.getSpentSoFar() != null) ? sug.getSpentSoFar() : analysis.categorySpending.getOrDefault(cat, 0.0);

                View bView = LayoutInflater.from(this).inflate(R.layout.item_suggested_budget, layoutSuggestedBudgets, false);
                TextView tvCategory = bView.findViewById(R.id.tvCategoryName);
                TextView tvLimits = bView.findViewById(R.id.tvBudgetLimits);
                TextView tvExplanation = bView.findViewById(R.id.tvBudgetExplanation);
                LinearLayout layoutExplanation = bView.findViewById(R.id.layoutExplanationContainer);
                Button btnApply = bView.findViewById(R.id.btnApplyRec);
                Button btnAdjust = bView.findViewById(R.id.btnAdjustRec);

                tvCategory.setText(cat);
                tvLimits.setText("Hiện tại: " + (currentLimit > 0 ? formatVND(currentLimit) : "Chưa thiết lập") + " ➜ AI đề xuất: " + formatVND(recLimit));

                String reason = (sug.getReason() != null && !sug.getReason().isEmpty())
                        ? sug.getReason()
                        : "Dựa trên lịch sử chi tiêu của bạn.";
                String expText = String.format(Locale.US,
                        "Chi tiêu hiện tại: %s.\n\n%s",
                        formatVND(spent), reason);
                tvExplanation.setText(expText);

                bView.setOnClickListener(v ->
                        layoutExplanation.setVisibility(
                                layoutExplanation.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

                double finalRecLimit = recLimit;
                btnApply.setOnClickListener(v -> {
                    applyBudgetChange(cat, finalRecLimit);
                    Toast.makeText(this, "Đã áp dụng hạn mức AI cho " + cat, Toast.LENGTH_SHORT).show();
                    refreshData();
                });
                btnAdjust.setOnClickListener(v -> showAdjustDialog(cat, finalRecLimit));

                layoutSuggestedBudgets.addView(bView);
            }

            if (layoutSuggestedBudgets.getChildCount() == 0) {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("Gemini AI chưa có đề xuất hạn mức cụ thể.");
                tvEmpty.setTextColor(0xFF808090);
                tvEmpty.setPadding(16, 16, 16, 16);
                layoutSuggestedBudgets.addView(tvEmpty);
            }
            return;
        }

        // Fallback: dùng heuristic nếu chưa có dữ liệu AI
        if (analysis.recommendedBudgets.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Không có danh mục nào cần đề xuất.");
            tvEmpty.setTextColor(0xFF808090);
            tvEmpty.setPadding(16, 16, 16, 16);
            layoutSuggestedBudgets.addView(tvEmpty);
            return;
        }

        for (Map.Entry<String, Double> entry : analysis.recommendedBudgets.entrySet()) {
            String cat = entry.getKey();
            double recLimit = entry.getValue();
            double currentLimit = analysis.categoryBudgets.getOrDefault(cat, 0.0);

            View bView = LayoutInflater.from(this).inflate(R.layout.item_suggested_budget, layoutSuggestedBudgets, false);
            TextView tvCategory = bView.findViewById(R.id.tvCategoryName);
            TextView tvLimits = bView.findViewById(R.id.tvBudgetLimits);
            TextView tvExplanation = bView.findViewById(R.id.tvBudgetExplanation);
            LinearLayout layoutExplanation = bView.findViewById(R.id.layoutExplanationContainer);
            Button btnApply = bView.findViewById(R.id.btnApplyRec);
            Button btnAdjust = bView.findViewById(R.id.btnAdjustRec);

            tvCategory.setText(cat);
            tvLimits.setText("Hiện tại: " + (currentLimit > 0 ? formatVND(currentLimit) : "Chưa thiết lập") + " ➜ Đề xuất: " + formatVND(recLimit));

            double spent = analysis.categorySpending.getOrDefault(cat, 0.0);
            String expText = String.format(Locale.US,
                    "Chi tiêu hiện tại: %s.\n\nHệ thống khuyến nghị mức ngân sách %s nhằm tối ưu thói quen chi tiêu của bạn.",
                    formatVND(spent), formatVND(recLimit));
            tvExplanation.setText(expText);

            bView.setOnClickListener(v -> {
                layoutExplanation.setVisibility(layoutExplanation.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            });

            btnApply.setOnClickListener(v -> {
                applyBudgetChange(cat, recLimit);
                Toast.makeText(this, "Đã áp dụng ngân sách đề xuất cho " + cat, Toast.LENGTH_SHORT).show();
                refreshData();
            });

            btnAdjust.setOnClickListener(v -> showAdjustDialog(cat, recLimit));

            layoutSuggestedBudgets.addView(bView);
        }
    }

    private void showAdjustDialog(String category, double defaultVal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tự điều chỉnh ngân sách: " + category);

        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.format(Locale.US, "%.0f", defaultVal));
        input.addTextChangedListener(new com.expensetracker_manager.utils.NumberTextWatcher(input));
        builder.setView(input);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String valStr = input.getText().toString().trim().replace(".", "");
            if (!valStr.isEmpty()) {
                double customVal = Double.parseDouble(valStr);
                applyBudgetChange(category, customVal);
                Toast.makeText(this, "Đã lưu điều chỉnh thành công!", Toast.LENGTH_SHORT).show();
                refreshData();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void applyBudgetChange(String category, double amount) {
        List<BudgetResponse> cached = OfflineCacheManager.getInstance(this).getCachedBudgets();
        boolean found = false;
        for (BudgetResponse b : cached) {
            if (category.equalsIgnoreCase(b.getCategoryName())) {
                b.setAmount(amount);
                found = true;
                break;
            }
        }
        if (!found) {
            BudgetResponse mock = new BudgetResponse();
            mock.setId(System.currentTimeMillis());
            mock.setCategoryName(category);
            mock.setAmount(amount);
            mock.setMonth(Calendar.getInstance().get(Calendar.MONTH) + 1);
            mock.setYear(Calendar.getInstance().get(Calendar.YEAR));
            mock.setSpent(0);
            cached.add(mock);
        }
        OfflineCacheManager.getInstance(this).cacheBudgets(cached);

        // Sync with backend if online
        if (com.expensetracker_manager.utils.NetworkUtils.isNetworkAvailable(this)) {
            long userId = TokenManager.getInstance(this).getUserId();
            long categoryId = -1;
            for (CategoryResponse cat : categories) {
                if (category.equalsIgnoreCase(cat.getName())) {
                    categoryId = cat.getId();
                    break;
                }
            }

            if (categoryId == -1) {
                return;
            }

            long serverBudgetId = -1;
            for (BudgetResponse b : backendBudgets) {
                if (category.equalsIgnoreCase(b.getCategoryName())) {
                    serverBudgetId = b.getId();
                    break;
                }
            }

            BudgetRequest request = new BudgetRequest();
            request.setAmount(amount);
            request.setMonth(Calendar.getInstance().get(Calendar.MONTH) + 1);
            request.setYear(Calendar.getInstance().get(Calendar.YEAR));
            request.setUserId(userId);
            request.setCategoryId(categoryId);

            if (serverBudgetId != -1) {
                RetrofitClient.getInstance().getBudgetApi().update(serverBudgetId, request)
                        .enqueue(new Callback<BudgetResponse>() {
                            @Override
                            public void onResponse(Call<BudgetResponse> call, Response<BudgetResponse> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(AiBudgetPlannerActivity.this,
                                            "Đã cập nhật ngân sách trực tuyến cho " + category, Toast.LENGTH_SHORT)
                                            .show();
                                    loadCategoriesAndBudgets();
                                }
                            }

                            @Override
                            public void onFailure(Call<BudgetResponse> call, Throwable t) {
                            }
                        });
            } else {
                RetrofitClient.getInstance().getBudgetApi().create(request)
                        .enqueue(new Callback<BudgetResponse>() {
                            @Override
                            public void onResponse(Call<BudgetResponse> call, Response<BudgetResponse> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(AiBudgetPlannerActivity.this,
                                            "Đã thiết lập ngân sách trực tuyến cho " + category, Toast.LENGTH_SHORT)
                                            .show();
                                    loadCategoriesAndBudgets();
                                }
                            }

                            @Override
                            public void onFailure(Call<BudgetResponse> call, Throwable t) {
                            }
                        });
            }
        }
    }

    private void applyAllRecommendations() {
        for (Map.Entry<String, Double> entry : analysis.recommendedBudgets.entrySet()) {
            applyBudgetChange(entry.getKey(), entry.getValue());
        }
        Toast.makeText(this, "Đã áp dụng toàn bộ hạn mức đề xuất!", Toast.LENGTH_SHORT).show();
        refreshData();
    }

    private String getHealthStatusString(String health) {
        switch (health) {
            case "Excellent":
                return "Xuất sắc";
            case "Good":
                return "Tốt";
            case "Fair":
                return "Trung bình";
            case "Poor":
            default:
                return "Cần cải thiện";
        }
    }
}
