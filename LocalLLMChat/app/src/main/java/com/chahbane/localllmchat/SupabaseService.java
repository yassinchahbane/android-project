package com.chahbane.localllmchat;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseService {

    /**
     * Create/Insert a new location record in the Supabase 'location_logs' table.
     */
    @Headers({
        "Prefer: return=representation",
        "Content-Type: application/json"
    })
    @POST("rest/v1/location_logs")
    Call<List<SupabaseUser>> createUser(
        @Header("apikey") String apiKey,
        @Header("Authorization") String authHeader, // Format: "Bearer KEY"
        @Body SupabaseUser user
    );

    /**
     * Query a user profile by exact username match (PostgREST filter format: 'eq.username')
     */
    @Headers({
        "Content-Type: application/json"
    })
    @GET("rest/v1/users")
    Call<List<SupabaseUser>> getUserByUsername(
        @Header("apikey") String apiKey,
        @Header("Authorization") String authHeader,
        @Query("username") String usernameFilter // Format: "eq.john_doe"
    );

    /**
     * Update user coordinates inside the Supabase cloud table.
     */
    @Headers({
        "Prefer: return=representation",
        "Content-Type: application/json"
    })
    @PATCH("rest/v1/users")
    Call<List<SupabaseUser>> updateUserLocation(
        @Header("apikey") String apiKey,
        @Header("Authorization") String authHeader,
        @Query("username") String usernameFilter, // Format: "eq.john_doe"
        @Body SupabaseUser user
    );
}

