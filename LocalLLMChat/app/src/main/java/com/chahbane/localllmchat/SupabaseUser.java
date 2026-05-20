package com.chahbane.localllmchat;

import com.google.gson.annotations.SerializedName;

public class SupabaseUser {
    @SerializedName("username")
    private String username;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("updated_at")
    private String updatedAt;

    public SupabaseUser() {}

    public SupabaseUser(String username, Double latitude, Double longitude) {
        this.username = username;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}

