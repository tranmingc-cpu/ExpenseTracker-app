package com.expensetracker.expensetracker_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FirebaseLoginRequest {
    @NotBlank(message = "ID Token is required")
    private String idToken;
}
