package com.expensetracker.expensetracker_api.service.impl;

import com.expensetracker.expensetracker_api.dto.request.WalletRequest;
import com.expensetracker.expensetracker_api.entity.UserEntity;
import com.expensetracker.expensetracker_api.entity.WalletEntity;
import com.expensetracker.expensetracker_api.exception.ResourceNotFoundException;
import com.expensetracker.expensetracker_api.repository.UserRepository;
import com.expensetracker.expensetracker_api.repository.WalletRepository;
import com.expensetracker.expensetracker_api.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @Override
    public WalletEntity createWallet(WalletRequest request) {

        UserEntity user = userRepository.findById(
                request.getUserId()
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "User not found with id: "
                                + request.getUserId()));

        WalletEntity wallet = new WalletEntity();

        wallet.setName(request.getName());
        wallet.setType(request.getType());
        wallet.setBalance(request.getBalance());
        wallet.setDescription(request.getDescription());
        wallet.setUser(user);

        return walletRepository.save(wallet);
    }

    @Override
    public List<WalletEntity> getAllWallets() {
        return walletRepository.findAll();
    }

    @Override
    public WalletEntity getWallet(Long id) {

        return walletRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Wallet not found with id: " + id));
    }

    @Override
    public List<WalletEntity> getWalletsByUser(Long userId) {
        return walletRepository.findByUserId(userId);
    }

    @Override
    public WalletEntity updateWallet(
            Long id,
            WalletRequest request) {

        WalletEntity wallet = walletRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Wallet not found with id: " + id));

        wallet.setName(request.getName());
        wallet.setType(request.getType());
        wallet.setBalance(request.getBalance());
        wallet.setDescription(request.getDescription());

        return walletRepository.save(wallet);
    }

    @Override
    public void deleteWallet(Long id) {

        if (!walletRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Wallet not found with id: " + id);
        }

        walletRepository.deleteById(id);
    }
}