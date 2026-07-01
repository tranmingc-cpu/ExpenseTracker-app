package com.expensetracker_manager.model.request;

import com.google.gson.annotations.SerializedName;

public class SyncRequest {
    @SerializedName("userId")
    private Long userId;

    @SerializedName("phoneNumber")
    private String phoneNumber;

    @SerializedName("otp")
    private String otp;

    public SyncRequest(Long userId, String phoneNumber, String otp) {
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.otp = otp;
    }

    public Long getUserId() { return userId; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getOtp() { return otp; }
}
