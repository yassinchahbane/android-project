package com.chahbane.localllmchat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DELAY = 2000; // 2 seconds
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        sessionManager = new SessionManager(this);

        // Check for valid session after splash delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkSessionAndRedirect();
        }, SPLASH_DELAY);
    }

    private void checkSessionAndRedirect() {
        String token = sessionManager.fetchAuthToken();
        
        if (token != null && !token.isEmpty()) {
            // Token exists, go directly to MainActivity
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            // No token, go to LoginActivity
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
        }
        
        finish(); // Close splash activity so user can't go back to it
    }
}

