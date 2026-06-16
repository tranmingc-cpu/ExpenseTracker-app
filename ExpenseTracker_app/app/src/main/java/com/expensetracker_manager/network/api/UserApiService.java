package com.expensetracker_manager.network.api;

import com.expensetracker_manager.model.response.UserModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface UserApiService {

    @GET("api/users")
    Call<List<UserModel>> getAllUsers();

    @POST("api/users")
    Call<UserModel> createUser(@Body UserModel user);

    @GET("api/users/{id}")
    Call<UserModel> getUserById(@Path("id") long id);

    @PUT("api/users/{id}")
    Call<UserModel> updateUser(@Path("id") long id, @Body UserModel user);

    @DELETE("api/users/{id}")
    Call<String> deleteUser(@Path("id") long id);
}
