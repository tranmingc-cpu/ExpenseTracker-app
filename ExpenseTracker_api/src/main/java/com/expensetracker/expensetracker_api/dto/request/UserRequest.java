package com.expensetracker.expensetracker_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;
    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private String email;
    private String phoneNumber;
    private String avatarUrl;
}