package com.chahbane.localllmchat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {
    private EditText usernameInput, emailInput, passwordInput;
    private Button signupButton;
    private TextView loginLink;
    private AuthApiService authService;

    // Location components
    private FusedLocationProviderClient fusedLocationClient;
    private Double latitude = null;
    private Double longitude = null;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean fineLocationGranted = result.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (fineLocationGranted || coarseLocationGranted) {
                    fetchLastKnownLocation();
                } else {
                    Toast.makeText(this, "Location permission is required to fetch coordinates", Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        usernameInput = findViewById(R.id.usernameSignup);
        emailInput = findViewById(R.id.emailSignup);
        passwordInput = findViewById(R.id.passwordSignup);
        signupButton = findViewById(R.id.signupButton);
        loginLink = findViewById(R.id.loginLink);

        authService = AuthRetrofitClient.getClient(this).create(AuthApiService.class);

        // Initialize location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationPermission();

        signupButton.setOnClickListener(v -> handleSignup());

        loginLink.setOnClickListener(v -> {
            finish(); // Go back to LoginActivity
        });
    }

    private void requestLocationPermission() {
        locationPermissionLauncher.launch(new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    @SuppressLint("MissingPermission")
    private void fetchLastKnownLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                Toast.makeText(this, "GPS Coordinates Captured: " + latitude + ", " + longitude, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Unable to capture location. Please turn on your device GPS.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleSignup() {
        String user = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String pass = passwordInput.getText().toString().trim();

        if (user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate minimum requirements
        if (user.length() < 3) {
            Toast.makeText(this, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pass.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Capture GPS variables in UserSignupRequest DTO
        UserSignupRequest request = new UserSignupRequest(user, pass, email, user, "User", latitude, longitude);
        authService.signup(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    String usernameToSave = (authResponse.getUsername() != null && !authResponse.getUsername().isEmpty()) ? authResponse.getUsername() : user;
                    Toast.makeText(SignupActivity.this, "Registration Successful! Welcome " + usernameToSave, Toast.LENGTH_LONG).show();
                    
                    // Register the user in Firebase Authentication
                    com.google.firebase.auth.FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                android.util.Log.d("FirebaseAuth", "User registered in Firebase Auth successfully!");
                            } else {
                                android.util.Log.e("FirebaseAuth", "Failed to register user in Firebase Auth", task.getException());
                            }
                        });

                    if (authResponse.getToken() != null) {
                        SessionManager sessionManager = new SessionManager(SignupActivity.this);
                        sessionManager.saveAuthToken(authResponse.getToken());
                        sessionManager.saveUsername(usernameToSave);
                        
                        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        finish(); // Return to Login if no token
                    }
                } else {
                    String errorMessage = "Registration Failed";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            if (errorBody.contains("already exists") || errorBody.contains("duplicate")) {
                                errorMessage = "Username or email already exists";
                            } else if (response.code() == 403) {
                                errorMessage = "Access forbidden. Check server configuration.";
                            } else if (response.code() == 400) {
                                errorMessage = "Invalid input data. Check all fields.";
                            } else if (response.code() == 401) {
                                errorMessage = "Unauthorized access";
                            } else if (response.code() == 500) {
                                errorMessage = "Server error. Please try again";
                            } else if (response.code() == 404) {
                                errorMessage = "Service not found. Check server URL.";
                            }
                        }
                    } catch (Exception e) {
                        // Keep default error message
                    }
                    Toast.makeText(SignupActivity.this, errorMessage + " (Code: " + response.code() + ")", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(SignupActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}

