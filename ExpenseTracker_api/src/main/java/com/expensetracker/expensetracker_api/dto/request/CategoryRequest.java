package com.expensetracker.expensetracker_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    @NotBlank(message = "Category type is required")
    private String type;

    private String icon;

    private String color;
    @NotNull(message = "User id is required")
    private Long userId;
}