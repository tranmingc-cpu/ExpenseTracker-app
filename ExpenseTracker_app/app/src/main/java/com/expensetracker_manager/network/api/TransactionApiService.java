package com.expensetracker_manager.network.api;

import com.expensetracker_manager.model.request.TransactionRequest;
import com.expensetracker_manager.model.response.TransactionResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface TransactionApiService {

    @POST("api/transactions")
    Call<TransactionResponse> create(@Body TransactionRequest request);

    @GET("api/transactions/{id}")
    Call<TransactionResponse> getById(@Path("id") long id);

    @GET("api/transactions/user/{userId}")
    Call<List<TransactionResponse>> getByUser(@Path("userId") long userId);

    @PUT("api/transactions/{id}")
    Call<TransactionResponse> update(@Path("id") long id, @Body TransactionRequest request);

    @DELETE("api/transactions/{id}")
    Call<Void> delete(@Path("id") long id);
}
