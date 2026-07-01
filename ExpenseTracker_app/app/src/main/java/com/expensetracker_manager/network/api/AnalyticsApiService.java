package com.expensetracker_manager.network.api;

import com.expensetracker_manager.model.response.CategoryReportResponse;
import com.expensetracker_manager.model.response.FinancialOverviewResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AnalyticsApiService {

    @GET("/api/analytics/overview")
    Call<List<FinancialOverviewResponse>> getOverview(
            @Query("userId") Long userId
    );

    @GET("/api/analytics/categories")
    Call<List<CategoryReportResponse>> getCategories(
            @Query("userId") Long userId,
            @Query("type") String type,
            @Query("month") Integer month,
            @Query("year") Integer year
    );

    @GET("/api/ai/budget-analysis")
    Call<com.expensetracker_manager.model.response.AiAnalysisResponse> getBudgetAnalysis(
            @Query("userId") Long userId,
            @Query("month") Integer month,
            @Query("year") Integer year
    );
}
