package com.chahbane.localllmchat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    private PreviewView viewFinder;
    private Button captureButton;
    private ImageButton backButton;
    private LinearLayout loadingLayout;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        viewFinder = findViewById(R.id.viewFinder);
        captureButton = findViewById(R.id.captureButton);
        backButton = findViewById(R.id.backButton);
        loadingLayout = findViewById(R.id.loadingLayout);

        cameraExecutor = Executors.newSingleThreadExecutor();

        startCamera();

        captureButton.setOnClickListener(v -> takePhoto());
        backButton.setOnClickListener(v -> finish());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                Toast.makeText(this, "Failed to start camera preview: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        loadingLayout.setVisibility(View.VISIBLE);

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                @SuppressLint("UnsafeOptInUsageError")
                android.media.Image mediaImage = image.getImage();
                if (mediaImage != null) {
                    InputImage inputImage = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
                    recognizeText(inputImage);
                }
                image.close();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> {
                    loadingLayout.setVisibility(View.GONE);
                    Toast.makeText(CameraActivity.this, "Snapping photo failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void recognizeText(InputImage image) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(visionText -> runOnUiThread(() -> {
                    loadingLayout.setVisibility(View.GONE);
                    String text = visionText.getText();
                    if (text == null || text.trim().isEmpty()) {
                        Toast.makeText(CameraActivity.this, "No readable text found in picture. Please try again.", Toast.LENGTH_LONG).show();
                    } else {
                        showTextResultDialog(text);
                    }
                }))
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    loadingLayout.setVisibility(View.GONE);
                    Toast.makeText(CameraActivity.this, "OCR Scanning failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }));
    }

    private void showTextResultDialog(String text) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Recognized Text");

        // Scrollable, selectable EditText allows highlight & copy
        final EditText input = new EditText(this);
        input.setText(text);
        input.setPadding(40, 40, 40, 40);
        input.setGravity(android.view.Gravity.TOP);
        input.setTextSize(16f);
        input.setBackground(null); // Remove default bottom line

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.addView(input);
        builder.setView(scrollView);

        // Copy button
        builder.setNeutralButton("Copy All", (dialog, which) -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Recognized Text", input.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(CameraActivity.this, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
        });

        // Insert into chat button
        builder.setPositiveButton("Insert into Chat", (dialog, which) -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("extracted_text", input.getText().toString());
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        builder.setNegativeButton("Retake", (dialog, which) -> dialog.dismiss());

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}

