package com.expensetracker.expensetracker_api.service;

import com.expensetracker.expensetracker_api.dto.request.CategoryRequest;
import com.expensetracker.expensetracker_api.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    List<CategoryResponse> getAll();

    List<CategoryResponse> getByUser(Long userId);

    CategoryResponse getById(Long id);

    CategoryResponse update(Long id,
                            CategoryRequest request);

    void delete(Long id);
}