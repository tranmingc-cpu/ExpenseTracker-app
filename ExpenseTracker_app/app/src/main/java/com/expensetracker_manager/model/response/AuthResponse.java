package com.expensetracker_manager.model.response;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("userid")
    private Long userid;

    @SerializedName("email")
    private String email;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("jwtToken")
    private String jwtToken;

    public AuthResponse() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getUserId() { return userId != null ? userId : userid; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getUserid() { return userid != null ? userid : userId; }
    public void setUserid(Long userid) { this.userid = userid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getJwtToken() { return jwtToken; }
    public void setJwtToken(String jwtToken) { this.jwtToken = jwtToken; }
}
