package com.expensetracker.expensetracker_api.dto.request;

import lombok.Data;

@Data
public class SyncRequest {
    private Long userId;
    private String phoneNumber;
    private String otp;
}
