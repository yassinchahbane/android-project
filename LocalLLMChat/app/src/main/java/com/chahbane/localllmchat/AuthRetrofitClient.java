package com.chahbane.localllmchat;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class AuthRetrofitClient {
    private static Retrofit retrofit = null;
    
    // ⚠️ Check your Spring Boot port in application.properties (usually 8080 or 8081)
    private static String BASE_URL = "http://192.168.11.168:8081/";

    public static Retrofit getClient(android.content.Context context) {
        if (retrofit == null) {
            SessionManager sessionManager = new SessionManager(context);
            BASE_URL = sessionManager.fetchSpringBaseUrl();

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static void reset() {
        retrofit = null;
    }
}

