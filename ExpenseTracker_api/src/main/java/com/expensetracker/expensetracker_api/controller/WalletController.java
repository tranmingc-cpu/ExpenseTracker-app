package com.expensetracker.expensetracker_api.controller;

import com.expensetracker.expensetracker_api.dto.request.WalletRequest;
import com.expensetracker.expensetracker_api.entity.WalletEntity;
import com.expensetracker.expensetracker_api.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public WalletEntity createWallet(
            @Valid @RequestBody WalletRequest request) {

        return walletService.createWallet(request);
    }

    @GetMapping
    public List<WalletEntity> getAllWallets() {
        return walletService.getAllWallets();
    }

    @GetMapping("/{id}")
    public WalletEntity getWallet(
            @PathVariable Long id) {

        return walletService.getWallet(id);
    }

    @GetMapping("/user/{userId}")
    public List<WalletEntity> getWalletByUser(
            @PathVariable Long userId) {

        return walletService.getWalletsByUser(userId);
    }

    @PutMapping("/{id}")
    public WalletEntity updateWallet(
            @PathVariable Long id,
            @Valid @RequestBody WalletRequest request) {

        return walletService.updateWallet(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteWallet(
            @PathVariable Long id) {

        walletService.deleteWallet(id);
    }
}