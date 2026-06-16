package com.expensetracker.expensetracker_api.controller;

import com.expensetracker.expensetracker_api.dto.request.FirebaseLoginRequest;
import com.expensetracker.expensetracker_api.dto.request.LoginRequest;
import com.expensetracker.expensetracker_api.dto.request.RegisterRequest;
import com.expensetracker.expensetracker_api.dto.response.AuthResponse;
import com.expensetracker.expensetracker_api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public AuthResponse forgotPassword(@RequestParam String email) {
        return authService.forgotPassword(email);
    }

    @PostMapping("/reset-password")
    public AuthResponse resetPassword(
            @RequestParam String email,
            @RequestParam String token,
            @RequestParam String newPassword) {
        return authService.resetPassword(email, token, newPassword);
    }
}