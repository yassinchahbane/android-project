package com.chahbane.localllmchat;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {
    
    @POST("api/auth/register")
    Call<AuthResponse> signup(@Body UserSignupRequest request);
    
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body UserLoginRequest request);
    
    @POST("api/auth/test")
    Call<String> test();
}
