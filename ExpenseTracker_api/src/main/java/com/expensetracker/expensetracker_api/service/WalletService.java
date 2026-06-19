package com.expensetracker.expensetracker_api.service;

import com.expensetracker.expensetracker_api.dto.request.WalletRequest;
import com.expensetracker.expensetracker_api.entity.WalletEntity;

import java.util.List;

public interface WalletService {

    WalletEntity createWallet(WalletRequest request);

    List<WalletEntity> getAllWallets();

    WalletEntity getWallet(Long id);

    List<WalletEntity> getWalletsByUser(Long userId);

    WalletEntity updateWallet(Long id, WalletRequest request);

    void deleteWallet(Long id);
}