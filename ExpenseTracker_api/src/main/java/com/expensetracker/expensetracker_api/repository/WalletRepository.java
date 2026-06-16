package com.expensetracker.expensetracker_api.repository;

import com.expensetracker.expensetracker_api.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletRepository
        extends JpaRepository<WalletEntity, Long> {

    List<WalletEntity> findByUserId(Long userId);
}