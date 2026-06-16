package com.expensetracker.expensetracker_api.repository;

import com.expensetracker.expensetracker_api.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
   Optional<UserEntity> findByEmail(String email);

   boolean existsByEmail(String email);

   Optional<UserEntity> findByFirebaseUid(String firebaseUid);
}