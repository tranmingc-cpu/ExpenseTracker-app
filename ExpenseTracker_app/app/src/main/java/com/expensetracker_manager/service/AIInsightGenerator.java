package com.expensetracker_manager.service;

import java.util.*;

public class AIInsightGenerator {

    public static class Recommendation {
        public String title;
        public String description;
        public String category;
        public double currentLimit;
        public double recommendedLimit;
        public String explanation;
        public String estimatedImprovement;

        public Recommendation(String title, String description, String category, double currentLimit, double recommendedLimit, String explanation, String estimatedImprovement) {
            this.title = title;
            this.description = description;
            this.category = category;
            this.currentLimit = currentLimit;
            this.recommendedLimit = recommendedLimit;
            this.explanation = explanation;
            this.estimatedImprovement = estimatedImprovement;
        }
    }

    public static List<Recommendation> generateAllInsights(FinancialAnalysisEngine.AnalysisResult analysis) {
        List<Recommendation> allRecs = new ArrayList<>();

        // 1. Budget recommended limit adjustments
        for (Map.Entry<String, Double> entry : analysis.recommendedBudgets.entrySet()) {
            String cat = entry.getKey();
            double recommendedLimit = entry.getValue();
            double currentLimit = analysis.categoryBudgets.getOrDefault(cat, 0.0);
            double spent = analysis.categorySpending.getOrDefault(cat, 0.0);

            if (currentLimit > 0) {
                if (spent > currentLimit) {
                    double excess = spent - currentLimit;
                    double potentialSaving = excess + (currentLimit - recommendedLimit);
                    String improvement = String.format(Locale.US, "Tiết kiệm thêm khoảng %,.0fđ mỗi tháng", potentialSaving);
                    String explanation = String.format(Locale.US, "Bạn đã chi tiêu quá giới hạn của danh mục %s là %,.0fđ (đã chi %,.0fđ). Hãy điều chỉnh giới hạn ngân sách mới để giữ an toàn tài chính.", cat, currentLimit, spent);
                    
                    allRecs.add(new Recommendation(
                        "Điều chỉnh ngân sách " + cat,
                        "Giảm giới hạn " + cat + " xuống còn " + String.format(Locale.US, "%,.0f", recommendedLimit) + "đ",
                        cat,
                        currentLimit,
                        recommendedLimit,
                        explanation,
                        improvement
                    ));
                } else if (spent > 0.8 * currentLimit) {
                    double potentialSaving = currentLimit - recommendedLimit;
                    String improvement = String.format(Locale.US, "Tăng tích lũy thêm %,.0fđ", potentialSaving);
                    String explanation = String.format(Locale.US, "Chi tiêu cho %s đang ở mức tiệm cận giới hạn ngân sách (%,.0fđ/%,.0fđ). Đề xuất giảm nhẹ để tối ưu hóa quỹ tiết kiệm.", cat, spent, currentLimit);
                    
                    allRecs.add(new Recommendation(
                        "Tối ưu ngân sách " + cat,
                        "Đặt giới hạn mới cho " + cat + ": " + String.format(Locale.US, "%,.0f", recommendedLimit) + "đ",
                        cat,
                        currentLimit,
                        recommendedLimit,
                        explanation,
                        improvement
                    ));
                }
            } else {
                // No budget set
                String improvement = String.format(Locale.US, "Tạo thói quen quản lý chi tiêu cho danh mục %s", cat);
                String explanation = String.format(Locale.US, "Bạn chưa thiết lập ngân sách cho %s nhưng đã chi tiêu %,.0fđ. Thiết lập ngân sách ở mức %,.0fđ giúp hạn chế vung tay quá trán.", cat, spent, recommendedLimit);
                
                allRecs.add(new Recommendation(
                    "Thiết lập ngân sách " + cat,
                    "Đặt giới hạn cho " + cat + ": " + String.format(Locale.US, "%,.0f", recommendedLimit) + "đ",
                    cat,
                    0,
                    recommendedLimit,
                    explanation,
                    improvement
                ));
            }
        }

        // 2. High-impact alerts (e.g. overspending risk, goal progress)
        if ("High".equalsIgnoreCase(analysis.overspendingRisk)) {
            String explanation = "Dựa trên tốc độ chi tiêu hiện tại, dự kiến bạn sẽ vượt ngân sách tổng hoặc thu nhập tháng này.";
            allRecs.add(0, new Recommendation(
                "⚠️ Nguy cơ chi tiêu vượt mức cao",
                "Cắt giảm ngay các khoản chi không thiết yếu để đưa dự báo chi tiêu về mức an toàn.",
                "Tổng quan",
                0,
                0,
                explanation,
                "Giúp tránh thâm hụt tài chính cuối tháng"
            ));
        }

        if (!analysis.abnormalExpenses.isEmpty()) {
            String firstAbnormal = analysis.abnormalExpenses.get(0);
            String explanation = "Hệ thống phát hiện giao dịch có giá trị lớn bất thường: " + firstAbnormal + ". Các khoản chi này làm tăng nhanh tốc độ tiêu dùng.";
            allRecs.add(new Recommendation(
                "🔍 Phát hiện chi tiêu đột biến",
                "Xem xét lại giao dịch lớn: " + firstAbnormal,
                "Khác",
                0,
                0,
                explanation,
                "Hạn chế các chi phí phát sinh bất ngờ"
            ));
        }

        // Goal progress predictions
        if (analysis.remainingDays > 0 && analysis.goalTarget > 0) {
            double currentSavingRate = analysis.totalIncome - analysis.totalExpense;
            if (currentSavingRate > 0) {
                // reduce spending on food by 10% to reach savings goal earlier
                double foodSpent = analysis.categorySpending.getOrDefault("Ăn uống", 0.0);
                if (foodSpent > 0) {
                    double extraDailySavings = (foodSpent * 0.1) / Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                    double newDailySavings = (currentSavingRate / Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) + extraDailySavings;
                    double remainingTarget = analysis.goalTarget - analysis.goalCurrent;
                    int newRemainingDays = (int) Math.ceil(remainingTarget / newDailySavings);
                    int daysSaved = Math.max(1, analysis.remainingDays - newRemainingDays);

                    String explanation = String.format(Locale.US, "Giảm 10%% chi phí Ăn uống giúp tăng tốc độ tích lũy hàng ngày của bạn.");
                    allRecs.add(new Recommendation(
                        "🎯 Đạt mục tiêu tiết kiệm sớm hơn",
                        "Giảm 10% chi tiêu Ăn uống để đạt mục tiêu \"" + analysis.primaryGoalName + "\" sớm hơn " + daysSaved + " ngày",
                        "Ăn uống",
                        0,
                        0,
                        explanation,
                        String.format(Locale.US, "Hoàn thành mục tiêu sớm hơn %d ngày", daysSaved)
                    ));
                }
            }
        }

        //  Habit Analysis
        if (analysis.recentTransactions != null && !analysis.recentTransactions.isEmpty()) {
            Map<String, Integer> categoryFrequency = new HashMap<>();
            for (com.expensetracker_manager.model.response.TransactionResponse tr : analysis.recentTransactions) {
                if ("EXPENSE".equalsIgnoreCase(tr.getType())) {
                    String cat = tr.getCategoryName();
                    if (cat == null || cat.isEmpty()) cat = "Khác";
                    categoryFrequency.put(cat, categoryFrequency.getOrDefault(cat, 0) + 1);
                }
            }

            for (Map.Entry<String, Integer> entry : categoryFrequency.entrySet()) {
                if (entry.getValue() >= 5) {
                    String cat = entry.getKey();
                    String explanation = String.format(Locale.US, "Hệ thống nhận thấy bạn có thói quen chi tiêu thường xuyên vào danh mục %s (%d lần trong tháng). Hãy cân nhắc gộp các khoản chi này hoặc giảm tần suất để tối ưu hóa ngân sách.", cat, entry.getValue());
                    allRecs.add(new Recommendation(
                        "💡 Thói quen chi tiêu: " + cat,
                        "Giảm tần suất chi tiêu cho " + cat,
                        cat,
                        0,
                        0,
                        explanation,
                        "Giúp kiểm soát các khoản chi lặt vặt"
                    ));
                }
            }
        }

        // Sort or prioritize
        List<Recommendation> sorted = new ArrayList<>();
        for (Recommendation r : allRecs) {
            if (r.title.contains("⚠️") || r.title.contains("🎯") || r.title.contains("💡")) {
                sorted.add(r);
            }
        }
        for (Recommendation r : allRecs) {
            if (!sorted.contains(r) && r.title.contains("🔍")) {
                sorted.add(r);
            }
        }
        for (Recommendation r : allRecs) {
            if (!sorted.contains(r)) {
                sorted.add(r);
            }
        }

        return sorted;
    }

    public static List<Recommendation> generateInsights(FinancialAnalysisEngine.AnalysisResult analysis) {
        List<Recommendation> all = generateAllInsights(analysis);
        if (all.size() > 3) {
            return all.subList(0, 3);
        }
        return all;
    }
}
