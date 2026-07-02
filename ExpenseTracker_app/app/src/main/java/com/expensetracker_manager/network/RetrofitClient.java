package com.expensetracker_manager.network;

import android.content.Context;

import com.expensetracker_manager.network.api.PaymentApiService;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class RetrofitClient {

    private static final String BASE_URL = "http://192.168.1.9:8080";
    private static final String BASE_URL1 = "https://expensetracker-app-quec.onrender.com/";

    private static Context appContext;
    private static RetrofitClient instance;
    private final Retrofit retrofit;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    private RetrofitClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    okhttp3.Request original = chain.request();
                    okhttp3.Request.Builder requestBuilder = original.newBuilder();

                    if (appContext != null) {
                        String token = com.expensetracker_manager.utils.TokenManager.getInstance(appContext).getToken();
                        if (token != null && !token.isEmpty()) {
                            requestBuilder.header("Authorization", "Bearer " + token);
                        }
                    }

                    return chain.proceed(requestBuilder.build());
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public <T> T create(Class<T> service) {
        return retrofit.create(service);
    }

    public com.expensetracker_manager.network.api.AuthApiService getAuthApi() {
        return create(com.expensetracker_manager.network.api.AuthApiService.class);
    }

    public com.expensetracker_manager.network.api.BudgetApiService getBudgetApi() {
        return create(com.expensetracker_manager.network.api.BudgetApiService.class);
    }

    public com.expensetracker_manager.network.api.CategoryApiService getCategoryApi() {
        return create(com.expensetracker_manager.network.api.CategoryApiService.class);
    }

    public com.expensetracker_manager.network.api.TransactionApiService getTransactionApi() {
        return create(com.expensetracker_manager.network.api.TransactionApiService.class);
    }

    public com.expensetracker_manager.network.api.WalletApiService getWalletApi() {
        return create(com.expensetracker_manager.network.api.WalletApiService.class);
    }

    public com.expensetracker_manager.network.api.UserApiService getUserApi() {
        return create(com.expensetracker_manager.network.api.UserApiService.class);
    }

    public com.expensetracker_manager.network.api.ReportApiService getReportApi() {
        return create(com.expensetracker_manager.network.api.ReportApiService.class);
    }

    public com.expensetracker_manager.network.api.SavingGoalApiService getSavingGoalApi() {
        return create(com.expensetracker_manager.network.api.SavingGoalApiService.class);
    }

    public com.expensetracker_manager.network.api.RecurringTransactionApiService getRecurringTransactionApi() {
        return create(com.expensetracker_manager.network.api.RecurringTransactionApiService.class);
    }

    public com.expensetracker_manager.network.api.PaymentApiService getPaymentApi() {
        return create(com.expensetracker_manager.network.api.PaymentApiService.class);
    }

    public com.expensetracker_manager.network.api.AnalyticsApiService getAnalyticsApi() {
        return create(com.expensetracker_manager.network.api.AnalyticsApiService.class);
    }
}
