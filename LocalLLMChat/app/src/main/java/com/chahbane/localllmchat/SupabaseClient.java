package com.chahbane.localllmchat;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {
    
    // =========================================================================
    // 💡 TO RUN: Replace these placeholder values with your actual credentials 
    //            retrieved from your Supabase Dashboard (Settings -> API)
    // =========================================================================
    public static final String SUPABASE_URL = "https://uerwvqqdrxameenoavcy.supabase.co/";
    public static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVlcnd2cXFkcnhhbWVlbm9hdmN5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzkwNTczMzYsImV4cCI6MjA5NDYzMzMzNn0.2I_YGrUknvSaIvxnwAxc8pHgV6Oukm2yxyU4a83GZQ4";

    private static Retrofit retrofit = null;
    private static SupabaseService service = null;

    /**
     * Retrieve the active singleton Retrofit service instance.
     */
    public static synchronized SupabaseService getService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            // Simple check to prevent empty URL failures
            String baseUrl = SUPABASE_URL;
            if (!baseUrl.startsWith("http")) {
                baseUrl = "https://placeholder.supabase.co/";
            }

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();

            service = retrofit.create(SupabaseService.class);
        }
        return service;
    }

    /**
     * Helper to return the standardized Bearer authorization header.
     */
    public static String getAuthHeader() {
        return "Bearer " + SUPABASE_KEY;
    }
}

