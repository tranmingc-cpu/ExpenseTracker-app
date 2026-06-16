package com.expensetracker.expensetracker_api.controller;

import com.expensetracker.expensetracker_api.entity.UserEntity;
import com.expensetracker.expensetracker_api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserEntity> getAllUsers() {

        return userService.getAllUsers();
    }

    @PostMapping
    public UserEntity createUser(
            @RequestBody UserEntity user) {

        return userService.createUser(user);
    }

    @GetMapping("/{id}")
    public UserEntity getUserById(
            @PathVariable Long id) {

        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    public UserEntity updateUser(
            @PathVariable Long id,
            @RequestBody UserEntity user) {

        return userService.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    public String deleteUser(
            @PathVariable Long id) {

        userService.deleteUser(id);

        return "User deleted successfully";
    }
}