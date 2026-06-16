package com.expensetracker.expensetracker_api.service.impl;

import com.expensetracker.expensetracker_api.dto.request.CategoryRequest;
import com.expensetracker.expensetracker_api.dto.response.CategoryResponse;
import com.expensetracker.expensetracker_api.entity.CategoryEntity;
import com.expensetracker.expensetracker_api.entity.UserEntity;
import com.expensetracker.expensetracker_api.exception.ResourceNotFoundException;
import com.expensetracker.expensetracker_api.repository.CategoryRepository;
import com.expensetracker.expensetracker_api.repository.UserRepository;
import com.expensetracker.expensetracker_api.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private CategoryResponse map(CategoryEntity c) {
        // Chuyển dữ liệu từ Entity đến Response DTO
        // Tránh trả trực tiếp Entity ra API
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .type(c.getType())
                .icon(c.getIcon())
                .color(c.getColor())
                .userId(c.getUser().getId())
                .userName(c.getUser().getFullName())
                .build();
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {

        UserEntity user = userRepository.findById(
                request.getUserId()
        ).orElseThrow(() ->
                new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        CategoryEntity c = new CategoryEntity();

        c.setName(request.getName());
        c.setType(request.getType());
        c.setIcon(request.getIcon());
        c.setColor(request.getColor());
        c.setUser(user);

        return map(categoryRepository.save(c));
    }

    @Override
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream().map(this::map).toList();
    }

    @Override
    public List<CategoryResponse> getByUser(Long userId) {
        return categoryRepository.findByUserId(userId).stream().map(this::map).toList();
    }

    @Override

    public CategoryResponse getById(Long id) {

        CategoryEntity category = categoryRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category not found with id: " + id));

        return map(category);
    }

    @Override
    public CategoryResponse update(Long id, CategoryRequest request) {

        CategoryEntity c = categoryRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Category not found with id: " + id));
        c.setName(request.getName());
        c.setType(request.getType());
        c.setIcon(request.getIcon());
        c.setColor(request.getColor());

        return map(categoryRepository.save(c));
    }
    @Override
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }
}