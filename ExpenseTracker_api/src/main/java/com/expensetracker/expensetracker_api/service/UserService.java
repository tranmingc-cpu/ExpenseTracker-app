package com.expensetracker.expensetracker_api.service;

import com.expensetracker.expensetracker_api.entity.UserEntity;

import java.util.List;

public interface UserService {

    List<UserEntity> getAllUsers();

    UserEntity getUserById(Long id);

    UserEntity createUser(UserEntity user);

    UserEntity updateUser(Long id, UserEntity user);

    void deleteUser(Long id);
}