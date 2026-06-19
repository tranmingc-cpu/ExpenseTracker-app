package com.expensetracker_manager.network.api;

import com.expensetracker_manager.model.request.SavingGoalRequest;
import com.expensetracker_manager.model.response.SavingGoalResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface SavingGoalApiService {
    @POST("api/saving-goals")
    Call<SavingGoalResponse> create(@Body SavingGoalRequest request);

    @GET("api/saving-goals/user/{userId}")
    Call<List<SavingGoalResponse>> getByUser(@Path("userId") Long userId);

    @PUT("api/saving-goals/{id}")
    Call<SavingGoalResponse> update(@Path("id") Long id, @Body SavingGoalRequest request);

    @DELETE("api/saving-goals/{id}")
    Call<Void> delete(@Path("id") Long id);
}
