package com.expensetracker_manager.network.api;

import com.expensetracker_manager.model.request.RecurringTransactionRequest;
import com.expensetracker_manager.model.response.RecurringTransactionResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface RecurringTransactionApiService {
    @POST("api/recurring-transactions")
    Call<RecurringTransactionResponse> create(@Body RecurringTransactionRequest request);

    @GET("api/recurring-transactions/user/{userId}")
    Call<List<RecurringTransactionResponse>> getByUser(@Path("userId") Long userId);

    @PUT("api/recurring-transactions/{id}")
    Call<RecurringTransactionResponse> update(@Path("id") Long id, @Body RecurringTransactionRequest request);

    @DELETE("api/recurring-transactions/{id}")
    Call<Void> delete(@Path("id") Long id);
}
