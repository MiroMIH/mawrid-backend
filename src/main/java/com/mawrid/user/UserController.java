package com.mawrid.user;

import com.mawrid.common.response.ApiResponse;
import com.mawrid.user.dto.FcmTokenRequest;
import com.mawrid.user.dto.UpdateUserRequest;
import com.mawrid.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getById(user.getId())));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(user.getId(), request)));
    }

    @PatchMapping("/me/fcm-token")
    @Operation(summary = "Register FCM device token (Flutter)")
    public ResponseEntity<ApiResponse<Void>> updateFcmToken(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody FcmTokenRequest request
    ) {
        userService.updateFcmToken(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(null, "FCM token updated"));
    }
}
