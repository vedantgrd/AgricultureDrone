package com.example.agridronee;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class UserModel {
    private String username;
    private String email;
    private String profileImageUrl;
    private String userType;
    private long createdAt;
    private long lastLoginAt;

    // Required empty constructor for Firebase
    public UserModel() {
    }

    public UserModel(String username, String email) {
        this.username = username;
        this.email = email;
        this.userType = "standard"; // Default user type
        this.createdAt = System.currentTimeMillis();
        this.lastLoginAt = System.currentTimeMillis();
    }

    public UserModel(String username, String email, String userType) {
        this.username = username;
        this.email = email;
        this.userType = userType;
        this.createdAt = System.currentTimeMillis();
        this.lastLoginAt = System.currentTimeMillis();
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(long lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void updateLastLogin() {
        this.lastLoginAt = System.currentTimeMillis();
    }
}