package com.expensetracker_manager.model.request;

public class VerifyResetCodeRequest {
    private String email;
    private String otp;

    public VerifyResetCodeRequest(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }

    public String getEmail() {
        return email;
    }

    public String getOtp() {
        return otp;
    }
}