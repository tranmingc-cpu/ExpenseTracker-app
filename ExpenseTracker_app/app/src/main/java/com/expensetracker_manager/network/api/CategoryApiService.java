package com.expensetracker_manager.network.api;

import com.expensetracker_manager.model.request.CategoryRequest;
import com.expensetracker_manager.model.response.CategoryResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface CategoryApiService {

    @POST("api/categories")
    Call<CategoryResponse> create(@Body CategoryRequest request);

    @GET("api/categories")
    Call<List<CategoryResponse>> getAll();

    @GET("api/categories/{id}")
    Call<CategoryResponse> getById(@Path("id") long id);

    @GET("api/categories/user/{userId}")
    Call<List<CategoryResponse>> getByUser(@Path("userId") long userId);

    @PUT("api/categories/{id}")
    Call<CategoryResponse> update(@Path("id") long id, @Body CategoryRequest request);

    @DELETE("api/categories/{id}")
    Call<Void> delete(@Path("id") long id);
}
