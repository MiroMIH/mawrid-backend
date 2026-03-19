package com.mawrid.category;

import com.mawrid.category.dto.CategoryAttributeResponse;
import com.mawrid.category.dto.CategoryResponse;
import com.mawrid.category.dto.CategoryTreeResponse;
import com.mawrid.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Shared authenticated category endpoints (all roles).
 * Admin management endpoints live in AdminCategoryController at /api/v1/admin/categories.
 * Supplier subscription endpoints live in SellerCategoryController at /api/v1/seller/categories.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Industrial category tree")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    @Operation(summary = "Get full active category tree")
    public ResponseEntity<ApiResponse<List<CategoryTreeResponse>>> getTree() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getTree()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single category by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getById(id)));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get a category by slug")
    public ResponseEntity<ApiResponse<CategoryResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getBySlug(slug)));
    }

    @GetMapping("/{id}/attributes")
    @Operation(summary = "Get effective attribute schema for a category — own + inherited")
    public ResponseEntity<ApiResponse<List<CategoryAttributeResponse>>> getAttributes(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getAttributes(id)));
    }
}
