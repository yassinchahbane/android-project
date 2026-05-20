package com.chahbane.localllmchat;

import com.google.gson.annotations.SerializedName;

public class LocationUpdateRequest {
    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    public LocationUpdateRequest(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}

