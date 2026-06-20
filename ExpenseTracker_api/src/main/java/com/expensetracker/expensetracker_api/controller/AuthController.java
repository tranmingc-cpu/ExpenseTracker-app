package com.expensetracker.expensetracker_api.controller;

import com.expensetracker.expensetracker_api.dto.request.FirebaseLoginRequest;
import com.expensetracker.expensetracker_api.dto.request.LoginRequest;
import com.expensetracker.expensetracker_api.dto.request.RegisterRequest;
import com.expensetracker.expensetracker_api.dto.response.AuthResponse;
import com.expensetracker.expensetracker_api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.expensetracker.expensetracker_api.dto.request.ForgotPasswordRequest;
import com.expensetracker.expensetracker_api.dto.request.ResetPasswordRequest;
import org.springframework.web.bind.annotation.RequestBody;
import com.expensetracker.expensetracker_api.dto.request.VerifyResetCodeRequest;
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(
            @RequestBody RegisterRequest request) {

        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @RequestBody LoginRequest request) {

        return authService.login(request);
    }

    @PostMapping("/firebase-login")
    public AuthResponse firebaseLogin(
            @Valid @RequestBody FirebaseLoginRequest request) {

        return authService.firebaseLogin(request);
    }

    @PostMapping("/forgot-password")
    public AuthResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request.getEmail());
    }
    @PostMapping("/verify-reset-code")
    public AuthResponse verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        return authService.verifyResetCode(
                request.getEmail(),
                request.getOtp()
        );
    }
    @PostMapping("/reset-password")
    public AuthResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(
                request.getEmail(),
                request.getOtp(),
                request.getNewPassword()
        );
    }
}