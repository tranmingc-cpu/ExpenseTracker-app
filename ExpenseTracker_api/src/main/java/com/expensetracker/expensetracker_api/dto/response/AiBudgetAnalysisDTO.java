package com.expensetracker.expensetracker_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiBudgetAnalysisDTO {
    private String overallStatus;
    /** Chỉ chứa các category ĐÃ CÓ budgetLimit: cảnh báo rủi ro, % sử dụng */
    private List<Insight> insights;
    /** Gợi ý hạn mức cho TẤT CẢ category có chi tiêu trong lịch sử (kể cả chưa có budget) */
    private List<BudgetSuggestion> budgetSuggestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Insight {
        private String category;
        private String risk;
        private String message;
        private Double recommendedLimit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetSuggestion {
        private String category;
        private Double recommendedLimit;
        private String reason;
        /** Chi tiêu thực tế tháng hiện tại (từ DB, gửi về Android để tránh mismatch tên) */
        private Double spentSoFar;
    }
}
