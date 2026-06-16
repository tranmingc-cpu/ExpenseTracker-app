package com.expensetracker_manager.network.api;

import com.expensetracker_manager.model.request.FirebaseLoginRequest;
import com.expensetracker_manager.model.request.LoginRequest;
import com.expensetracker_manager.model.request.RegisterRequest;
import com.expensetracker_manager.model.response.AuthResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/firebase-login")
    Call<AuthResponse> firebaseLogin(@Body FirebaseLoginRequest request);

    @POST("api/auth/forgot-password")
    Call<AuthResponse> forgotPassword(@retrofit2.http.Query("email") String email);

    @POST("api/auth/reset-password")
    Call<AuthResponse> resetPassword(
            @retrofit2.http.Query("email") String email,
            @retrofit2.http.Query("token") String token,
            @retrofit2.http.Query("newPassword") String newPassword
    );
}
