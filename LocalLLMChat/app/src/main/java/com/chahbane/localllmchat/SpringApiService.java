package com.chahbane.localllmchat;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.PUT;

public interface SpringApiService {
    @PUT("api/users/location")
    Call<Void> updateLocation(@Body LocationUpdateRequest request);
}

