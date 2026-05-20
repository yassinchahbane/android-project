package com.chahbane.localllmchat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText usernameInput, passwordInput;
    private Button loginButton;
    private TextView signupLink, settingsLink;
    private AuthApiService authService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        signupLink = findViewById(R.id.signupLink);
        settingsLink = findViewById(R.id.settingsLink);

        sessionManager = new SessionManager(this);
        initializeAuthService();

        loginButton.setOnClickListener(v -> handleLogin());
        
        signupLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        settingsLink.setOnClickListener(v -> showServerSettingsDialog());

        // Fetch and print Firebase Cloud Messaging registration token immediately on startup
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    android.util.Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.getException());
                    return;
                }
                String token = task.getResult();
                android.util.Log.d("FCM_TOKEN", "Current Device FCM Token: " + token);
            });

        // Request runtime permission for notifications on Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS")
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this,
                        new String[]{"android.permission.POST_NOTIFICATIONS"}, 101);
            }
        }
    }

    private void initializeAuthService() {
        authService = AuthRetrofitClient.getClient(this).create(AuthApiService.class);
    }

    private void showServerSettingsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Server Settings");

        // Create a custom layout dynamically
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        // Presets Section
        android.widget.TextView presetTitle = new android.widget.TextView(this);
        presetTitle.setText("Quick Presets");
        presetTitle.setTextSize(16);
        presetTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        presetTitle.setTextColor(android.graphics.Color.parseColor("#2196F3"));
        presetTitle.setPadding(0, 0, 0, 8);
        layout.addView(presetTitle);

        android.widget.Button emulatorPresetBtn = new android.widget.Button(this);
        emulatorPresetBtn.setText("Local Emulator (10.0.2.2)");
        android.widget.LinearLayout.LayoutParams presetBtnParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        presetBtnParams.setMargins(0, 0, 0, 24);
        emulatorPresetBtn.setLayoutParams(presetBtnParams);
        layout.addView(emulatorPresetBtn);

        // Separator line 0
        android.view.View separator0 = new android.view.View(this);
        separator0.setBackgroundColor(android.graphics.Color.LTGRAY);
        android.widget.LinearLayout.LayoutParams sepParams0 = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 2);
        sepParams0.setMargins(0, 0, 0, 24);
        separator0.setLayoutParams(sepParams0);
        layout.addView(separator0);

        // Helper Section for Quick IP Update
        android.widget.TextView quickIpTitle = new android.widget.TextView(this);
        quickIpTitle.setText("Quick IP Update (Local Wi-Fi)");
        quickIpTitle.setTextSize(16);
        quickIpTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        quickIpTitle.setTextColor(android.graphics.Color.parseColor("#2196F3"));
        quickIpTitle.setPadding(0, 0, 0, 8);
        layout.addView(quickIpTitle);

        android.widget.TextView quickIpDesc = new android.widget.TextView(this);
        quickIpDesc.setText("Enter your computer's IP address to update both services:");
        quickIpDesc.setTextSize(12);
        quickIpDesc.setPadding(0, 0, 0, 8);
        layout.addView(quickIpDesc);

        android.widget.EditText quickIpInput = new android.widget.EditText(this);
        quickIpInput.setHint("e.g. 192.168.1.15");
        quickIpInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        quickIpInput.setBackgroundResource(android.R.drawable.editbox_background);
        quickIpInput.setPadding(12, 12, 12, 12);
        layout.addView(quickIpInput);

        android.widget.Button applyIpButton = new android.widget.Button(this);
        applyIpButton.setText("Apply IP");
        android.widget.LinearLayout.LayoutParams btnParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 8, 0, 24);
        applyIpButton.setLayoutParams(btnParams);
        layout.addView(applyIpButton);

        // Separator line
        android.view.View separator = new android.view.View(this);
        separator.setBackgroundColor(android.graphics.Color.LTGRAY);
        android.widget.LinearLayout.LayoutParams sepParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 2);
        sepParams.setMargins(0, 0, 0, 24);
        separator.setLayoutParams(sepParams);
        layout.addView(separator);

        // Advanced Custom URLs Section
        android.widget.TextView customUrlsTitle = new android.widget.TextView(this);
        customUrlsTitle.setText("Custom Service URLs");
        customUrlsTitle.setTextSize(16);
        customUrlsTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        customUrlsTitle.setTextColor(android.graphics.Color.parseColor("#2196F3"));
        customUrlsTitle.setPadding(0, 0, 0, 8);
        layout.addView(customUrlsTitle);

        android.widget.TextView springUrlLabel = new android.widget.TextView(this);
        springUrlLabel.setText("Spring Boot Base URL (Port 8081):");
        springUrlLabel.setTextSize(12);
        springUrlLabel.setPadding(0, 0, 0, 4);
        layout.addView(springUrlLabel);

        android.widget.EditText springUrlInput = new android.widget.EditText(this);
        springUrlInput.setText(sessionManager.fetchSpringBaseUrl());
        springUrlInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        springUrlInput.setBackgroundResource(android.R.drawable.editbox_background);
        springUrlInput.setPadding(12, 12, 12, 12);
        layout.addView(springUrlInput);

        android.widget.TextView llmUrlLabel = new android.widget.TextView(this);
        llmUrlLabel.setText("LLM Backend Base URL (Port 8000):");
        llmUrlLabel.setTextSize(12);
        llmUrlLabel.setPadding(0, 16, 0, 4);
        layout.addView(llmUrlLabel);

        android.widget.EditText llmUrlInput = new android.widget.EditText(this);
        llmUrlInput.setText(sessionManager.fetchLlmBaseUrl());
        llmUrlInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        llmUrlInput.setBackgroundResource(android.R.drawable.editbox_background);
        llmUrlInput.setPadding(12, 12, 12, 12);
        layout.addView(llmUrlInput);

        // Separator line 2
        android.view.View separator2 = new android.view.View(this);
        separator2.setBackgroundColor(android.graphics.Color.LTGRAY);
        android.widget.LinearLayout.LayoutParams sepParams2 = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 2);
        sepParams2.setMargins(0, 24, 0, 16);
        separator2.setLayoutParams(sepParams2);
        layout.addView(separator2);

        // Help troubleshooting section
        android.widget.TextView helpTitle = new android.widget.TextView(this);
        helpTitle.setText("💡 School Wi-Fi & Connection Help");
        helpTitle.setTextSize(14);
        helpTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        helpTitle.setTextColor(android.graphics.Color.parseColor("#F44336"));
        helpTitle.setPadding(0, 0, 0, 8);
        layout.addView(helpTitle);

        android.widget.TextView helpText = new android.widget.TextView(this);
        helpText.setText("School Wi-Fi networks prevent device-to-device communication (AP Isolation) and block local ports (8081, 8000) on your PC. Direct IP connection (like 10.40.11.51) will fail.\n\n" +
                "• Emulator: Click the 'Local Emulator (10.0.2.2)' button at the top! It loops back inside your PC and works 100% of the time, bypassing Wi-Fi restrictions entirely.\n\n" +
                "• Physical Device (Option 1): Create a 'Mobile Hotspot' on your phone, connect your PC to it, run 'ipconfig', and use that IP here.\n\n" +
                "• Physical Device (Option 2): Use a free tunnel tool on your computer (e.g. run 'ngrok http 8081' and 'ngrok http 8000' on your PC) and paste the generated HTTPS URLs in the fields above.");
        helpText.setTextSize(11);
        helpText.setTextColor(android.graphics.Color.DKGRAY);
        layout.addView(helpText);

        // ScrollView wrapper to handle smaller screens nicely
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.addView(layout);
        builder.setView(scrollView);

        // Preset click handler
        emulatorPresetBtn.setOnClickListener(v -> {
            springUrlInput.setText("http://10.0.2.2:8081/");
            llmUrlInput.setText("http://10.0.2.2:8000/");
            Toast.makeText(LoginActivity.this, "Applied Emulator configuration (10.0.2.2)!", Toast.LENGTH_SHORT).show();
        });

        // Apply IP click handler
        applyIpButton.setOnClickListener(v -> {
            String ip = quickIpInput.getText().toString().trim();
            if (ip.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter an IP address", Toast.LENGTH_SHORT).show();
                return;
            }
            // Strip any http prefix if accidentally added
            if (ip.startsWith("http://")) ip = ip.substring(7);
            if (ip.startsWith("https://")) ip = ip.substring(8);
            if (ip.endsWith("/")) ip = ip.substring(0, ip.length() - 1);

            springUrlInput.setText("http://" + ip + ":8081/");
            llmUrlInput.setText("http://" + ip + ":8000/");
            Toast.makeText(LoginActivity.this, "Applied IP configuration!", Toast.LENGTH_SHORT).show();
        });

        builder.setPositiveButton("Save", (dialog, which) -> {
            String springUrl = springUrlInput.getText().toString().trim();
            String llmUrl = llmUrlInput.getText().toString().trim();

            if (springUrl.isEmpty() || llmUrl.isEmpty()) {
                Toast.makeText(LoginActivity.this, "URLs cannot be empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Standardize format: ensure they end with a slash
            if (!springUrl.endsWith("/")) springUrl += "/";
            if (!llmUrl.endsWith("/")) llmUrl += "/";

            sessionManager.saveSpringBaseUrl(springUrl);
            sessionManager.saveLlmBaseUrl(llmUrl);

            // Reset client singletons so they load the new base URLs immediately
            AuthRetrofitClient.reset();
            RetrofitClient.reset();
            SpringRetrofitClient.reset();

            // Re-initialize authentication service with new URL
            initializeAuthService();

            Toast.makeText(LoginActivity.this, "Server configurations updated successfully!", Toast.LENGTH_LONG).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void handleLogin() {
        String user = usernameInput.getText().toString().trim();
        String pass = passwordInput.getText().toString().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please enter credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        UserLoginRequest request = new UserLoginRequest(user, pass);
        authService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    
                    // 1. Save the token and username received from Spring Boot
                    String token = authResponse.getToken();
                    if (token != null) {
                        sessionManager.saveAuthToken(token);
                    }
                    String usernameToSave = (authResponse.getUsername() != null && !authResponse.getUsername().isEmpty()) ? authResponse.getUsername() : user;
                    sessionManager.saveUsername(usernameToSave);

                    // 2. Smart Sync with Firebase Authentication
                    String email = authResponse.getEmail();
                    if (email != null && !email.isEmpty()) {
                        com.google.firebase.auth.FirebaseAuth firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
                        firebaseAuth.signInWithEmailAndPassword(email, pass)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    android.util.Log.d("FirebaseAuth", "Successfully logged into Firebase Authentication!");
                                } else {
                                    // If the user doesn't exist in Firebase Auth yet, register them on the fly!
                                    if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                                        android.util.Log.d("FirebaseAuth", "User not found in Firebase Auth. Registering on the fly...");
                                        firebaseAuth.createUserWithEmailAndPassword(email, pass)
                                            .addOnCompleteListener(registerLocallyTask -> {
                                                if (registerLocallyTask.isSuccessful()) {
                                                    android.util.Log.d("FirebaseAuth", "Successfully registered existing user in Firebase Auth on the fly!");
                                                } else {
                                                    android.util.Log.e("FirebaseAuth", "Failed to register user in Firebase Auth on the fly", registerLocallyTask.getException());
                                                }
                                            });
                                    } else {
                                        android.util.Log.e("FirebaseAuth", "Firebase Authentication login failed", task.getException());
                                    }
                                }
                            });
                    }

                    Toast.makeText(LoginActivity.this, "Welcome back, " + usernameToSave + "!", Toast.LENGTH_SHORT).show();
                    
                    // 3. Move to the Chat Screen
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = "Invalid Login Credentials";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            if (response.code() == 401) {
                                errorMessage = "Invalid username or password";
                            } else if (response.code() == 404) {
                                errorMessage = "User not found";
                            } else if (response.code() == 400) {
                                errorMessage = "Invalid input data";
                            }
                        }
                    } catch (Exception e) {
                        // Keep default error message
                    }
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Connection Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
