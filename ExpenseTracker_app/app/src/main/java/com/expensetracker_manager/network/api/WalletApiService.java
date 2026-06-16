package com.expensetracker_manager.network.api;

import com.expensetracker_manager.model.request.WalletRequest;
import com.expensetracker_manager.model.response.WalletResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface WalletApiService {

    @POST("api/wallets")
    Call<WalletResponse> createWallet(@Body WalletRequest request);

    @GET("api/wallets")
    Call<List<WalletResponse>> getAllWallets();

    @GET("api/wallets/{id}")
    Call<WalletResponse> getWallet(@Path("id") long id);

    @GET("api/wallets/user/{userId}")
    Call<List<WalletResponse>> getWalletByUser(@Path("userId") long userId);

    @PUT("api/wallets/{id}")
    Call<WalletResponse> updateWallet(@Path("id") long id, @Body WalletRequest request);

    @DELETE("api/wallets/{id}")
    Call<Void> deleteWallet(@Path("id") long id);
}
