package com.mawrid.admin.category;

import com.mawrid.category.CategoryService;
import com.mawrid.category.dto.*;
import com.mawrid.common.enums.NodeType;
import com.mawrid.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin — Categories", description = "Admin back-office category tree management")
public class AdminCategoryController {

    private final CategoryService categoryService;

    // ── Create ────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new category node")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CategoryCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(categoryService.create(request), "Category created"));
    }

    // ── Rename ────────────────────────────────────────────────────

    @PatchMapping("/{id}/rename")
    @Operation(summary = "Rename a category (forceRename=true required for SEEDED nodes — superadmin only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> rename(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRenameRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.rename(id, request)));
    }

    // ── Activate / deactivate ─────────────────────────────────────

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle active status. Deactivating cascades to all descendants.")
    public ResponseEntity<ApiResponse<CategoryResponse>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.toggleActive(id)));
    }

    // ── Move ─────────────────────────────────────────────────────

    @PostMapping("/{id}/move")
    @Operation(summary = "Move a category subtree to a new parent. Rewrites all descendant paths atomically.")
    public ResponseEntity<ApiResponse<CategoryResponse>> move(
            @PathVariable Long id,
            @Valid @RequestBody MoveRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.move(id, request.getNewParentId())));
    }

    // ── Delete ────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category. SEEDED nodes cannot be hard-deleted. Node must have no children and no demandes.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Leaf marking ─────────────────────────────────────────────

    @PatchMapping("/{id}/mark-leaf")
    @Operation(summary = "Mark a terminal node as LEAF (no children allowed after this)")
    public ResponseEntity<ApiResponse<CategoryResponse>> markAsLeaf(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.markAsLeaf(id)));
    }

    @PatchMapping("/{id}/unmark-leaf")
    @Operation(summary = "Remove LEAF status — allows adding children again")
    public ResponseEntity<ApiResponse<CategoryResponse>> unmarkAsLeaf(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.unmarkAsLeaf(id)));
    }

    // ── Attributes ────────────────────────────────────────────────

    @PostMapping("/{id}/attributes")
    @Operation(summary = "Add an attribute schema to a category")
    public ResponseEntity<ApiResponse<CategoryAttributeResponse>> addAttribute(
            @PathVariable Long id,
            @Valid @RequestBody CategoryAttributeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(categoryService.addAttribute(id, request), "Attribute added"));
    }

    @PatchMapping("/{id}/attributes/{attributeId}")
    @Operation(summary = "Update an attribute schema (key is immutable)")
    public ResponseEntity<ApiResponse<CategoryAttributeResponse>> updateAttribute(
            @PathVariable Long id,
            @PathVariable Long attributeId,
            @Valid @RequestBody CategoryAttributeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.updateAttribute(id, attributeId, request)));
    }

    @DeleteMapping("/{id}/attributes/{attributeId}")
    @Operation(summary = "Delete an attribute schema. Fails if used in existing demandes.")
    public ResponseEntity<Void> deleteAttribute(
            @PathVariable Long id,
            @PathVariable Long attributeId
    ) {
        categoryService.deleteAttribute(id, attributeId);
        return ResponseEntity.noContent().build();
    }

    // ── Stats ─────────────────────────────────────────────────────

    @GetMapping("/{id}/stats")
    @Operation(summary = "Statistics for a category node and its subtree")
    public ResponseEntity<ApiResponse<CategoryStatsResponse>> getStats(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getStats(id)));
    }

    // ── Search / filter ───────────────────────────────────────────

    @GetMapping("/search")
    @Operation(summary = "Search/filter categories for admin tree management")
    public ResponseEntity<ApiResponse<Page<CategoryResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer depth,
            @RequestParam(required = false) NodeType nodeType,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                categoryService.search(q, depth, nodeType, active, pageable)));
    }
}
