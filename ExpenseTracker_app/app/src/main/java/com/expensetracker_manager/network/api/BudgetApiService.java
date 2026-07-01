package com.expensetracker_manager.network.api;

import com.expensetracker_manager.model.request.BudgetRequest;
import com.expensetracker_manager.model.response.BudgetResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface BudgetApiService {

    @POST("api/budgets")
    Call<BudgetResponse> create(@Body BudgetRequest request);

    @GET("api/budgets/{id}")
    Call<BudgetResponse> getById(@Path("id") long id);

    @GET("api/budgets/user/{userId}")
    Call<List<BudgetResponse>> getByUser(
            @Path("userId") long userId,
            @retrofit2.http.Query("month") Integer month,
            @retrofit2.http.Query("year") Integer year
    );

    @PUT("api/budgets/{id}")
    Call<BudgetResponse> update(@Path("id") long id, @Body BudgetRequest request);

    @DELETE("api/budgets/{id}")
    Call<Void> delete(@Path("id") long id);
}
