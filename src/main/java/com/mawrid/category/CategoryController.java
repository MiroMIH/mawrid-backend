package com.mawrid.category;

import com.mawrid.category.dto.*;
import com.mawrid.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Industrial category tree")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get category tree (public)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getTree() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getTree()));
    }

    @GetMapping("/{id}/attributes")
    @Operation(summary = "Get effective attributes for a category (including inherited)")
    public ResponseEntity<ApiResponse<List<CategoryAttributeResponse>>> getAttributes(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getAttributes(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a new category (admin only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CategoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(categoryService.create(request), "Category created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Rename a category (admin only, non-SEEDED only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.update(id, request)));
    }

    @PostMapping("/{id}/move")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Move a category subtree to a new parent (admin only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> move(
            @PathVariable Long id,
            @Valid @RequestBody MoveRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.move(id, request.getNewParentId())));
    }

    @PostMapping("/{id}/attributes")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add an attribute schema to a category (admin only)")
    public ResponseEntity<ApiResponse<CategoryAttributeResponse>> addAttribute(
            @PathVariable Long id,
            @Valid @RequestBody CategoryAttributeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(categoryService.addAttribute(id, request), "Attribute added"));
    }
}
