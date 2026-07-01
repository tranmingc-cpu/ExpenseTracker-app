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
    private List<Insight> insights;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Insight {
        private String category;
        private String risk;
        private String message;
    }
}
