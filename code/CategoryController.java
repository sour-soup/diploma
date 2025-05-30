package org.soursoup.bimbim.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.soursoup.bimbim.dto.request.CategoryCreateRequest;
import org.soursoup.bimbim.dto.request.CategoryIdRequest;
import org.soursoup.bimbim.dto.response.CategoryResponse;
import org.soursoup.bimbim.mapper.CategoryMapper;
import org.soursoup.bimbim.service.CategoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public CategoryResponse createCategory(@RequestBody CategoryCreateRequest categoryCreateRes) {
        return categoryMapper.toDto(categoryService.createCategory(categoryCreateRes));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public void deleteCategory(@RequestBody CategoryIdRequest categoryIdRequest) {
        categoryService.deleteCategory(categoryIdRequest.id());
    }

    @GetMapping("/{categoryId}")
    @SecurityRequirement(name = "bearerAuth")
    public CategoryResponse getCategory(@PathVariable Long categoryId) {
        return categoryMapper.toDto(categoryService.getCategory(categoryId));
    }

    @GetMapping("/all")
    @SecurityRequirement(name = "bearerAuth")
    public List<CategoryResponse> getCategories() {
        return categoryService.getCategories().stream().map(categoryMapper::toDto).toList();
    }
}
