package com.chahbane.localllmchat;

import com.google.gson.annotations.SerializedName;

public class ChatRequest {
    @SerializedName("message")
    private String message;

    @SerializedName("use_rag")
    private boolean use_rag;

    public ChatRequest(String message, boolean use_rag) {
        this.message = message;
        this.use_rag = use_rag;
    }

    public String getMessage() {
        return message;
    }

    public boolean getUse_rag() {
        return use_rag;
    }
}
