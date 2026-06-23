package com.expensetracker_manager.network.api;

import com.expensetracker_manager.model.request.PaymentLinkRequest;
import com.expensetracker_manager.model.response.PaymentLinkResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface PaymentApiService {
    @POST("api/payment/generate-link")
    Call<PaymentLinkResponse> generateLinks(@Body PaymentLinkRequest request);
}