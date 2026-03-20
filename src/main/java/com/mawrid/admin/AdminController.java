package com.mawrid.admin;

import com.mawrid.admin.dto.AdminStatsResponse;
import com.mawrid.admin.dto.SimulationRequest;
import com.mawrid.admin.dto.SimulationResult;
import com.mawrid.common.response.ApiResponse;
import com.mawrid.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Admin back-office endpoints")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "Paginated user list")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listUsers(pageable)));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user detail by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getUserById(id)));
    }

    @PatchMapping("/users/{id}/toggle-enabled")
    @Operation(summary = "Enable or disable a user account")
    public ResponseEntity<ApiResponse<UserResponse>> toggleEnabled(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.toggleEnabled(id)));
    }

    @GetMapping("/users/export")
    @Operation(summary = "Export all users as CSV")
    public ResponseEntity<byte[]> exportUsersCsv() {
        byte[] csv = adminService.exportUsersCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/stats")
    @Operation(summary = "Dashboard statistics")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getStats()));
    }

    @PostMapping("/matching/simulate")
    @Operation(summary = "Dry-run matching simulation (reads only, no DB writes)")
    public ResponseEntity<ApiResponse<List<SimulationResult>>> simulate(
            @Valid @RequestBody SimulationRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.simulate(request)));
    }
}
