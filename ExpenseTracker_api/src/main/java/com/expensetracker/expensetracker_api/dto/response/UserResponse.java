package com.expensetracker.expensetracker_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    private String provider;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime upadateAt;

}