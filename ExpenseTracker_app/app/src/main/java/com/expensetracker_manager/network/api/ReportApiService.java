package com.expensetracker_manager.network.api;

import com.expensetracker_manager.model.response.CategoryReportResponse;
import com.expensetracker_manager.model.response.ReportSummaryResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ReportApiService {

    @GET("api/reports/summary")
    Call<ReportSummaryResponse> getSummary(
            @Query("userId") long userId,
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    @GET("api/reports/category-expense")
    Call<List<CategoryReportResponse>> getExpenseByCategory(
            @Query("userId") long userId,
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );
}
