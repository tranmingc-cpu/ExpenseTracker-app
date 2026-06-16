package com.expensetracker.expensetracker_api.controller;

import com.expensetracker.expensetracker_api.dto.request.CategoryRequest;
import com.expensetracker.expensetracker_api.dto.response.CategoryResponse;
import com.expensetracker.expensetracker_api.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public CategoryResponse create(
            @RequestBody CategoryRequest request) {

        return categoryService.create(request);
    }

    @GetMapping
    public List<CategoryResponse> getAll() {
        return categoryService.getAll();
    }

    @GetMapping("/{id}")
    public CategoryResponse getById(
            @PathVariable Long id) {

        return categoryService.getById(id);
    }

    @GetMapping("/user/{userId}")
    public List<CategoryResponse> getByUser(
            @PathVariable Long userId) {

        return categoryService.getByUser(userId);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(
            @PathVariable Long id,
            @RequestBody CategoryRequest request) {

        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id) {

        categoryService.delete(id);
    }
}