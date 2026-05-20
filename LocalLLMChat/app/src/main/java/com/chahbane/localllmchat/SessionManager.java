package com.chahbane.localllmchat;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "LocalLLMChatSession";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USERNAME = "username";
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveAuthToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    public String fetchAuthToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    public void saveUsername(String username) {
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    public String fetchUsername() {
        return pref.getString(KEY_USERNAME, null);
    }

    private static final String KEY_SPRING_URL = "spring_base_url";
    private static final String KEY_LLM_URL = "llm_base_url";

    public void saveSpringBaseUrl(String url) {
        editor.putString(KEY_SPRING_URL, url);
        editor.apply();
    }

    public String fetchSpringBaseUrl() {
        return pref.getString(KEY_SPRING_URL, "http://192.168.11.168:8081/");
    }

    public void saveLlmBaseUrl(String url) {
        editor.putString(KEY_LLM_URL, url);
        editor.apply();
    }

    public String fetchLlmBaseUrl() {
        return pref.getString(KEY_LLM_URL, "http://192.168.11.168:8000/");
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}

