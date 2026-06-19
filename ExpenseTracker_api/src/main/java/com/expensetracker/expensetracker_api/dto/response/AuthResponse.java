package com.expensetracker.expensetracker_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    private String message;
    private Long userId;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String jwtToken;

    // Compatibility constructor
    public AuthResponse(String message, Long userId) {
        this.message = message;
        this.userId = userId;
    }
}
