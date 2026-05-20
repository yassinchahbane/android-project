package com.chahbane.localllmchat;

import android.content.Context;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SpringRetrofitClient {
    private static Retrofit retrofit = null;
    private static String BASE_URL = "http://192.168.11.168:8081/";

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            SessionManager sessionManager = new SessionManager(context);
            BASE_URL = sessionManager.fetchSpringBaseUrl();

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Interceptor to add Auth Token to requests sent to Spring Boot
            Interceptor interceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request.Builder builder = chain.request().newBuilder();
                    String token = sessionManager.fetchAuthToken();
                    if (token != null) {
                        builder.addHeader("Authorization", "Bearer " + token);
                    }
                    return chain.proceed(builder.build());
                }
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(interceptor)
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

    public static void setBaseUrl(String url) {
        BASE_URL = url;
        retrofit = null;
    }

    public static void reset() {
        retrofit = null;
    }
}

