package com.expensetracker_manager.network.api;

import com.expensetracker_manager.model.request.RecurringTransactionRequest;
import com.expensetracker_manager.model.response.RecurringTransactionResponse;
import com.expensetracker_manager.model.response.TransactionResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RecurringTransactionApiService {

    @POST("api/recurring-transactions")
    Call<RecurringTransactionResponse> create(@Body RecurringTransactionRequest request);

    @GET("api/recurring-transactions/user/{userId}")
    Call<List<RecurringTransactionResponse>> getByUser(@Path("userId") Long userId);

    @PUT("api/recurring-transactions/{id}")
    Call<RecurringTransactionResponse> update(
            @Path("id") Long id,
            @Body RecurringTransactionRequest request
    );

    @POST("api/recurring-transactions/{id}/pay")
    Call<TransactionResponse> pay(
            @Path("id") Long id,
            @Query("userId") Long userId,
            @Query("categoryId") Long categoryId
    );

    @DELETE("api/recurring-transactions/{id}")
    Call<Void> delete(@Path("id") Long id);
}
