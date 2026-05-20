package com.chahbane.localllmchat;

public class UserSignupRequest {
    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private Double latitude;
    private Double longitude;

    public UserSignupRequest() {}

    public UserSignupRequest(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        // Set default values for firstName and lastName
        this.firstName = username;
        this.lastName = "User";
    }

    public UserSignupRequest(String username, String password, String email, String firstName, String lastName) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public UserSignupRequest(String username, String password, String email, String firstName, String lastName, Double latitude, Double longitude) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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
}
