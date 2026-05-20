package com.chahbane.localllmchat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView chatHistory;
    private EditText messageInput;
    private Button sendButton, logoutButton;
    private ImageButton micButton;
    private Switch ragSwitch;
    private ApiService apiService;
    private ScrollView scrollView;
    private SessionManager sessionManager;

    // Location-related UI and variables
    private TextView locationText;
    private FusedLocationProviderClient fusedLocationClient;
    private SpringApiService springApiService;

    // Camera OCR scanner components
    private ImageButton cameraButton;

    private final Handler locationUpdateHandler = new Handler(Looper.getMainLooper());
    private final Runnable locationUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            fetchAndUpdateLocation();
            // Schedule next execution in 5 minutes (300,000 milliseconds)
            locationUpdateHandler.postDelayed(this, 300000);
        }
    };

    private final ActivityResultLauncher<Intent> speechResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        messageInput.setText(matches.get(0));
                    }
                }
            }
    );

    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean fine = result.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarse = result.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (fine || coarse) {
                    fetchAndUpdateLocation();
                } else {
                    locationText.setText("📍 GPS Location: Permissions Denied");
                }
            }
    );

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openCameraActivity();
                } else {
                    Toast.makeText(this, "Camera permission is required to scan text", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String extractedText = result.getData().getStringExtra("extracted_text");
                    if (extractedText != null && !extractedText.isEmpty()) {
                        messageInput.setText(extractedText);
                        Toast.makeText(this, "Text inserted into message field!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 1. Check Session first
        sessionManager = new SessionManager(this);
        if (sessionManager.fetchAuthToken() == null) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        chatHistory = findViewById(R.id.chatHistory);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        logoutButton = findViewById(R.id.logoutButton);
        micButton = findViewById(R.id.micButton);
        cameraButton = findViewById(R.id.cameraButton);
        ragSwitch = findViewById(R.id.ragSwitch);
        scrollView = findViewById(R.id.scrollView);
        locationText = findViewById(R.id.locationText);

        // Get Retrofit instances
        Retrofit retrofit = RetrofitClient.getClient(this);
        apiService = retrofit.create(ApiService.class);

        Retrofit springRetrofit = SpringRetrofitClient.getClient(this);
        springApiService = springRetrofit.create(SpringApiService.class);

        // Initialize GPS Services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        sendButton.setOnClickListener(v -> {
            String userMessage = messageInput.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                sendMessageToAI(userMessage);
                messageInput.setText("");
            }
        });

        micButton.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
            try {
                speechResultLauncher.launch(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show();
            }
        });

        cameraButton.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                openCameraActivity();
            } else {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
            }
        });

        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                sessionManager.clearSession();
                redirectToLogin();
            });
        }

        ImageButton avatarScreenButton = findViewById(R.id.avatarScreenButton);
        if (avatarScreenButton != null) {
            avatarScreenButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AvatarActivity.class);
                startActivity(intent);
            });
        }

        messageInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) scrollToBottom();
        });

        // Run automatic Supabase Connection self-test
        testSupabaseConnection();

        // Fetch and print Firebase Cloud Messaging registration token
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    android.util.Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.getException());
                    return;
                }
                String token = task.getResult();
                android.util.Log.d("FCM_TOKEN", "Current Device FCM Token: " + token);
            });
    }

    private void testSupabaseConnection() {
        android.util.Log.d("SupabaseTest", "Initializing Supabase Connection Self-Test...");
        SupabaseClient.getService().getUserByUsername(
            SupabaseClient.SUPABASE_KEY,
            SupabaseClient.getAuthHeader(),
            "eq.test_connection_username"
        ).enqueue(new Callback<List<SupabaseUser>>() {
            @Override
            public void onResponse(Call<List<SupabaseUser>> call, Response<List<SupabaseUser>> response) {
                if (response.isSuccessful()) {
                    android.util.Log.d("SupabaseTest", "✅ SUCCESS: Connected to Supabase Cloud Database! Status: " + response.code());
                    Toast.makeText(MainActivity.this, "✅ Supabase Cloud DB Connected!", Toast.LENGTH_SHORT).show();
                } else {
                    android.util.Log.e("SupabaseTest", "❌ FAILED: Supabase responded with error code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<SupabaseUser>> call, Throwable t) {
                android.util.Log.e("SupabaseTest", "❌ FAILED: Supabase connection failure: " + t.getMessage());
            }
        });
    }

    private boolean checkCameraPermission() {
        return androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void openCameraActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        cameraResultLauncher.launch(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void startLocationUpdates() {
        // Fetch immediately and start the periodic task
        locationUpdateHandler.post(locationUpdateRunnable);
    }

    private void stopLocationUpdates() {
        locationUpdateHandler.removeCallbacks(locationUpdateRunnable);
    }

    private boolean checkLocationPermissions() {
        return androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                || androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        locationPermissionLauncher.launch(new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    @SuppressLint("MissingPermission")
    private void fetchAndUpdateLocation() {
        if (checkLocationPermissions()) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    updateLocationUI(lat, lon);
                    sendLocationToServer(lat, lon);
                } else {
                    android.util.Log.d("GPS", "Cached location is null. Requesting single active high-accuracy update...");
                    requestFreshLocation();
                }
            });
        } else {
            requestLocationPermissions();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestFreshLocation() {
        com.google.android.gms.location.LocationRequest locationRequest = 
            com.google.android.gms.location.LocationRequest.create()
                .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)
                .setFastestInterval(500)
                .setNumUpdates(1);

        fusedLocationClient.requestLocationUpdates(locationRequest, new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(com.google.android.gms.location.LocationResult locationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    double lat = locationResult.getLastLocation().getLatitude();
                    double lon = locationResult.getLastLocation().getLongitude();
                    updateLocationUI(lat, lon);
                    sendLocationToServer(lat, lon);
                } else {
                    locationText.setText("📍 Location: Coordinates unavailable (turn on GPS)");
                }
            }
        }, Looper.getMainLooper());
    }

    private void updateLocationUI(double lat, double lon) {
        new Thread(() -> {
            String addressText = "📍 Location: " + lat + ", " + lon;
            try {
                android.location.Geocoder geocoder = new android.location.Geocoder(MainActivity.this, java.util.Locale.getDefault());
                java.util.List<android.location.Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    android.location.Address address = addresses.get(0);
                    StringBuilder sb = new StringBuilder("📍 Location: ");
                    
                    String locality = address.getLocality();
                    String subAdmin = address.getSubAdminArea();
                    String adminArea = address.getAdminArea();
                    String country = address.getCountryName();
                    
                    if (locality != null) {
                        sb.append(locality);
                    } else if (subAdmin != null) {
                        sb.append(subAdmin);
                    } else if (adminArea != null) {
                        sb.append(adminArea);
                    }
                    
                    if (country != null) {
                        if (sb.length() > 12) {
                            sb.append(", ");
                        }
                        sb.append(country);
                    }
                    
                    if (sb.length() <= 12) {
                        String addressLine = address.getAddressLine(0);
                        if (addressLine != null) {
                            sb = new StringBuilder("📍 Location: ").append(addressLine);
                        }
                    }
                    
                    addressText = sb.toString();
                }
            } catch (Exception e) {
                android.util.Log.e("Geocoder", "Failed reverse-geocoding: " + e.getMessage());
            }

            final String finalText = addressText;
            runOnUiThread(() -> locationText.setText(finalText));
        }).start();
    }

    private void sendLocationToServer(double lat, double lon) {
        // 1. Sync to local Spring Boot Server
        LocationUpdateRequest request = new LocationUpdateRequest(lat, lon);
        springApiService.updateLocation(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    android.util.Log.d("GPS", "Real-time location updated on Spring Boot server successfully");
                } else if (response.code() == 401) {
                    android.util.Log.e("GPS", "Unauthorized. Session expired.");
                    sessionManager.clearSession();
                    redirectToLogin();
                } else {
                    android.util.Log.e("GPS", "Failed to update location on server. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                android.util.Log.e("GPS", "Connection failure updating location: " + t.getMessage());
            }
        });

        // 2. Smart Sync to Supabase Cloud Database!
        String username = sessionManager.fetchUsername();
        if (username != null) {
            sendLocationToSupabase(username, lat, lon);
        } else {
            android.util.Log.d("SupabaseGPS", "No active username cached, skipping cloud database sync.");
        }
    }

    private void sendLocationToSupabase(String username, double lat, double lon) {
        String currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(new java.util.Date());
        SupabaseUser user = new SupabaseUser(username, lat, lon);
        user.setUpdatedAt(currentTime);
        
        android.util.Log.d("SupabaseGPS", "Attempting to insert new location log to Supabase cloud...");
        
        // 1. Try to POST (Insert new row) first
        SupabaseClient.getService().createUser(
            SupabaseClient.SUPABASE_KEY,
            SupabaseClient.getAuthHeader(),
            user
        ).enqueue(new Callback<List<SupabaseUser>>() {
            @Override
            public void onResponse(Call<List<SupabaseUser>> call, Response<List<SupabaseUser>> response) {
                if (response.isSuccessful()) {
                    android.util.Log.d("SupabaseGPS", "✅ SUCCESS: Added a new location entry row to Supabase logs!");
                } else if (response.code() == 409) {
                    // 2. Fallback to PATCH if there is a unique key violation on username
                    android.util.Log.w("SupabaseGPS", "Unique constraint detected on username (Code 409). Falling back to updating the existing profile...");
                    updateExistingLocationInSupabase(username, user);
                } else {
                    String errorMsg = "";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        errorMsg = e.getMessage();
                    }
                    android.util.Log.e("SupabaseGPS", "Failed to insert location log. Code: " + response.code() + ", Error: " + errorMsg);
                }
            }

            @Override
            public void onFailure(Call<List<SupabaseUser>> call, Throwable t) {
                android.util.Log.e("SupabaseGPS", "Supabase insert connection failure: " + t.getMessage());
            }
        });
    }

    private void updateExistingLocationInSupabase(String username, SupabaseUser user) {
        SupabaseClient.getService().updateUserLocation(
            SupabaseClient.SUPABASE_KEY,
            SupabaseClient.getAuthHeader(),
            "eq." + username,
            user
        ).enqueue(new Callback<List<SupabaseUser>>() {
            @Override
            public void onResponse(Call<List<SupabaseUser>> call, Response<List<SupabaseUser>> response) {
                if (response.isSuccessful()) {
                    android.util.Log.d("SupabaseGPS", "✅ SUCCESS: Updated existing location and timestamp in Supabase cloud!");
                } else {
                    String errorMsg = "";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        errorMsg = e.getMessage();
                    }
                    android.util.Log.e("SupabaseGPS", "Failed to update existing location. Code: " + response.code() + ", Error: " + errorMsg);
                }
            }

            @Override
            public void onFailure(Call<List<SupabaseUser>> call, Throwable t) {
                android.util.Log.e("SupabaseGPS", "Supabase update connection failure: " + t.getMessage());
            }
        });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void sendMessageToAI(String message) {
        chatHistory.append("\n👤 You: " + message);
        chatHistory.append("\n🤖 AI: Thinking...");
        scrollToBottom();

        boolean useRag = ragSwitch.isChecked();
        ChatRequest request = new ChatRequest(message, useRag);

        apiService.sendMessage(request).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String aiReply = response.body().getReply();
                    List<String> sources = response.body().getSources();
                    if (sources != null && !sources.isEmpty()) {
                        aiReply += "\n\n📚 Sources: " + sources.toString();
                    }
                    updateChatHistory(aiReply);
                } else if (response.code() == 401) {
                    Toast.makeText(MainActivity.this, "Session Expired", Toast.LENGTH_SHORT).show();
                    sessionManager.clearSession();
                    redirectToLogin();
                } else {
                    updateChatHistory("❌ Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                updateChatHistory("❌ Connection Error: " + t.getMessage());
            }
        });
    }

    private void updateChatHistory(String aiReply) {
        String currentText = chatHistory.getText().toString();
        currentText = currentText.replace("🤖 AI: Thinking...", "");
        chatHistory.setText(currentText + "🤖 AI: " + aiReply + "\n\n");
        scrollToBottom();
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}
