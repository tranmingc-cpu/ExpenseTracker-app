package com.expensetracker_manager.model.response;

import java.util.List;

public class AiAnalysisResponse {
    private String overallStatus;
    /** Cảnh báo rủi ro – chỉ các category đã có hạn mức ngân sách */
    private List<Insight> insights;
    /** Gợi ý hạn mức – tất cả category có chi tiêu lịch sử */
    private List<BudgetSuggestion> budgetSuggestions;

    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }

    public List<Insight> getInsights() { return insights; }
    public void setInsights(List<Insight> insights) { this.insights = insights; }

    public List<BudgetSuggestion> getBudgetSuggestions() { return budgetSuggestions; }
    public void setBudgetSuggestions(List<BudgetSuggestion> budgetSuggestions) { this.budgetSuggestions = budgetSuggestions; }

    public static class Insight {
        private String category;
        private String risk;
        private String message;
        private Double recommendedLimit;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getRisk() { return risk; }
        public void setRisk(String risk) { this.risk = risk; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Double getRecommendedLimit() { return recommendedLimit; }
        public void setRecommendedLimit(Double recommendedLimit) { this.recommendedLimit = recommendedLimit; }
    }

    public static class BudgetSuggestion {
        private String category;
        private Double recommendedLimit;
        private String reason;
        /** Chi tiêu thực tế tháng hiện tại (từ server DB, gửi kèm để Android hiển thị) */
        private Double spentSoFar;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public Double getRecommendedLimit() { return recommendedLimit; }
        public void setRecommendedLimit(Double recommendedLimit) { this.recommendedLimit = recommendedLimit; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public Double getSpentSoFar() { return spentSoFar; }
        public void setSpentSoFar(Double spentSoFar) { this.spentSoFar = spentSoFar; }
    }
}
