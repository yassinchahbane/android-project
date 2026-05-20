package com.chahbane.localllmchat;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatResponse {
    @SerializedName("reply")
    private String reply;

    @SerializedName("sources")
    private List<String> sources;

    public String getReply() {
        return reply;
    }

    public List<String> getSources() {
        return sources;
    }
}
