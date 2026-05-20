package com.chahbane.localllmchat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import java.util.ArrayList;

public class AvatarActivity extends AppCompatActivity {
    private WebView avatarWebView;
    private ImageButton backButton;
    private ImageButton micButton;
    private TextView avatarStatusText;

    // Compact floating triggers
    private View dialogueTriggerCard;
    private TextView triggerBubbleText;

    // Cached Conversation History
    private String lastUserQuery = "Tap Aura's Mic to Speak";
    private String lastAiResponse = "I am online. Ready for your query.";

    private ApiService apiService;
    private TextToSpeech tts;

    // Real-time animation controls
    private final Handler voiceHandler = new Handler(Looper.getMainLooper());
    private boolean isSpeakingAnimationRunning = false;
    private final Runnable voiceIntensityRunnable = new Runnable() {
        @Override
        public void run() {
            if (isSpeakingAnimationRunning) {
                // Synthesize an organic wave volume envelope (0.2 to 1.0)
                double amplitude = 0.2 + 0.8 * Math.random() * Math.abs(Math.sin(System.currentTimeMillis() / 120.0));
                avatarWebView.evaluateJavascript("setVoiceIntensity(" + amplitude + ");", null);
                voiceHandler.postDelayed(this, 50); // post updates at 20 FPS
            }
        }
    };

    private final ActivityResultLauncher<Intent> speechResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                setAvatarState("idle");
                avatarStatusText.setText("🔮 TAP DECK TO START CHATTING");
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        String query = matches.get(0);
                        lastUserQuery = query;
                        triggerBubbleText.setText("💬 View: " + (query.length() > 20 ? query.substring(0, 17) + "..." : query));
                        sendQueryToAI(query);
                    }
                }
            }
    );

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar);

        avatarWebView = findViewById(R.id.avatarWebView);
        backButton = findViewById(R.id.backButton);
        micButton = findViewById(R.id.micButton);
        avatarStatusText = findViewById(R.id.avatarStatusText);
        dialogueTriggerCard = findViewById(R.id.dialogueTriggerCard);
        triggerBubbleText = findViewById(R.id.triggerBubbleText);

        // Setup LLM Retrofit API Client
        Retrofit retrofit = RetrofitClient.getClient(this);
        apiService = retrofit.create(ApiService.class);

        // Initialize Android Text To Speech (TTS)
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(java.util.Locale.US);
                tts.setPitch(0.70f); // Deepen the voice
                tts.setSpeechRate(0.92f); // Clear natural conversational rate
            } else {
                Toast.makeText(this, "Failed to initialize Speech Engine", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize hardware-accelerated 3D WebView
        avatarWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        WebSettings settings = avatarWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        // Log JavaScript errors for debugging
        avatarWebView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                android.util.Log.d("AvatarWebView", consoleMessage.message() + " -- line " + consoleMessage.lineNumber());
                return true;
            }
        });

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        avatarWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                setAvatarState("idle");
            }
        });

        avatarWebView.loadUrl("https://appassets.androidplatform.net/assets/3d_avatar.html");

        // Action Listeners
        dialogueTriggerCard.setOnClickListener(v -> showTranscriptDialog());
        micButton.setOnClickListener(v -> startVoiceListening());
        backButton.setOnClickListener(v -> finish());
    }

    private void showTranscriptDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Aura Conversation Transcript");

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(48, 48, 48, 48);

        TextView userTitle = new TextView(this);
        userTitle.setText("👤 YOU:");
        userTitle.setTextSize(12f);
        userTitle.setTextColor(0xFF2196F3);
        userTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        container.addView(userTitle);

        TextView userContent = new TextView(this);
        userContent.setText(lastUserQuery);
        userContent.setTextSize(15f);
        userContent.setTextColor(0xFF000000);
        userContent.setPadding(0, 8, 0, 32);
        container.addView(userContent);

        TextView aiTitle = new TextView(this);
        aiTitle.setText("🔮 AURA:");
        aiTitle.setTextSize(12f);
        aiTitle.setTextColor(0xFF00E676);
        aiTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        container.addView(aiTitle);

        TextView aiContent = new TextView(this);
        aiContent.setText(lastAiResponse);
        aiContent.setTextSize(16f);
        aiContent.setTextColor(0xFF333333);
        aiContent.setPadding(0, 8, 0, 16);
        aiContent.setLineSpacing(1.2f, 1.1f);
        container.addView(aiContent);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.addView(container);
        builder.setView(scrollView);

        builder.setPositiveButton("Close Dialog", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void startVoiceListening() {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
            stopSpeakingAnimation();
        }

        setAvatarState("listening");
        avatarStatusText.setText("🔮 LISTENING TO YOUR VOICE...");

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Vocalize your request...");
        try {
            speechResultLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Voice recognition not supported on this device", Toast.LENGTH_SHORT).show();
            setAvatarState("idle");
            avatarStatusText.setText("🔮 TAP DECK TO START CHATTING");
        }
    }

    private void sendQueryToAI(String prompt) {
        setAvatarState("thinking");
        avatarStatusText.setText("🔮 AURA IS SYNTHESIZING RESPONSE...");
        lastAiResponse = "Thinking...";

        ChatRequest request = new ChatRequest(prompt, true); // Always apply document RAG
        apiService.sendMessage(request).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String reply = response.body().getReply();
                    lastAiResponse = reply;
                    triggerBubbleText.setText("💬 View Aura's Answer");
                    speakText(reply);
                } else {
                    setAvatarState("idle");
                    avatarStatusText.setText("🔮 ERROR PROCESSING RESPONSE");
                    lastAiResponse = "Error generating response (Code: " + response.code() + ")";
                    triggerBubbleText.setText("💬 View Error Details");
                }
            }

            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                setAvatarState("idle");
                avatarStatusText.setText("🔮 CONNECTION FAILURE");
                lastAiResponse = "Failed to connect to local LLM backend: " + t.getMessage();
                triggerBubbleText.setText("💬 View Connection Error");
            }
        });
    }

    private void speakText(String text) {
        if (tts == null) return;

        // Guarantee masculine voice by finding a male voice in system right before speaking!
        try {
            boolean voiceSet = false;
            for (android.speech.tts.Voice voice : tts.getVoices()) {
                String name = voice.getName().toLowerCase();
                if (name.contains("male") || 
                    name.contains("en-us-x-sfg") || 
                    name.contains("en-us-x-smg") || 
                    name.contains("en-gb-x-rjs") || 
                    name.contains("en-us-x-iom")) {
                    tts.setVoice(voice);
                    voiceSet = true;
                    android.util.Log.d("TTS", "Successfully bound Male Voice: " + voice.getName());
                    break;
                }
            }
            if (!voiceSet) {
                android.util.Log.d("TTS", "No explicit male voice found, using system default with deep pitch.");
            }
        } catch (Exception e) {
            android.util.Log.e("TTS", "Voice list search error: " + e.getMessage());
        }

        // Apply masculine pitch overrides
        tts.setPitch(0.70f); 
        tts.setSpeechRate(0.92f); 

        setAvatarState("speaking");
        avatarStatusText.setText("🔮 AURA IS VOCALIZING RESPONSE...");
        startSpeakingAnimation();

        // Standard TTS parameters
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "AURA_SPEECH_ID");

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "AURA_SPEECH_ID");

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    stopSpeakingAnimation();
                    setAvatarState("idle");
                    avatarStatusText.setText("🔮 TAP DECK TO START CHATTING");
                });
            }

            @Override
            public void onError(String utteranceId) {
                runOnUiThread(() -> {
                    stopSpeakingAnimation();
                    setAvatarState("idle");
                    avatarStatusText.setText("🔮 TAP DECK TO START CHATTING");
                });
            }
        });
    }

    private void setAvatarState(String state) {
        avatarWebView.evaluateJavascript("setAvatarState('" + state + "');", null);
    }

    private void startSpeakingAnimation() {
        isSpeakingAnimationRunning = true;
        voiceHandler.post(voiceIntensityRunnable);
    }

    private void stopSpeakingAnimation() {
        isSpeakingAnimationRunning = false;
        voiceHandler.removeCallbacks(voiceIntensityRunnable);
        avatarWebView.evaluateJavascript("setVoiceIntensity(0);", null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tts != null) {
            tts.stop();
        }
        stopSpeakingAnimation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.shutdown();
        }
        stopSpeakingAnimation();
    }
}

