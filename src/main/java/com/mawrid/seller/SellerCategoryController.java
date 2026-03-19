package com.mawrid.seller;

import com.mawrid.category.CategoryService;
import com.mawrid.category.dto.CategoryResponse;
import com.mawrid.common.response.ApiResponse;
import com.mawrid.user.User;
import com.mawrid.user.UserService;
import com.mawrid.user.dto.UpdateCategoriesRequest;
import com.mawrid.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seller/categories")
@RequiredArgsConstructor
@Tag(name = "Seller - Categories", description = "Supplier category subscription management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('SUPPLIER')")
public class SellerCategoryController {

    private final CategoryService categoryService;
    private final UserService userService;

    @GetMapping("/subscribed")
    @Operation(summary = "Get the authenticated supplier's subscribed categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getSubscribed(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getSubscribed(user)));
    }

    @PatchMapping
    @Operation(summary = "Update supplier category subscriptions")
    public ResponseEntity<ApiResponse<UserResponse>> updateCategories(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateCategoriesRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateCategories(user.getId(), request, user)));
    }
}
