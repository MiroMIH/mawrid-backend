package com.mawrid.demande;

import com.mawrid.common.response.ApiResponse;
import com.mawrid.demande.dto.DemandeRequest;
import com.mawrid.demande.dto.DemandeResponse;
import com.mawrid.demande.dto.DemandeStatusUpdate;
import com.mawrid.user.User;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/demandes")
@RequiredArgsConstructor
@Tag(name = "Demandes", description = "Procurement request management")
@SecurityRequirement(name = "bearerAuth")
public class DemandeController {

    private final DemandeService demandeService;

    @GetMapping
    @Operation(summary = "List demandes (buyers see own, suppliers see open in their categories)")
    public ResponseEntity<ApiResponse<Page<DemandeResponse>>> list(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(demandeService.list(user, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single demande")
    public ResponseEntity<ApiResponse<DemandeResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(demandeService.getById(id, user)));
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Create a demande (buyer only)")
    public ResponseEntity<ApiResponse<DemandeResponse>> create(
            @Valid @RequestBody DemandeRequest request,
            @AuthenticationPrincipal User buyer
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(demandeService.create(request, buyer), "Demande created"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Close or cancel a demande (buyer only)")
    public ResponseEntity<ApiResponse<DemandeResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody DemandeStatusUpdate request,
            @AuthenticationPrincipal User buyer
    ) {
        return ResponseEntity.ok(ApiResponse.ok(demandeService.updateStatus(id, request, buyer)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BUYER') or hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Delete a demande")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        demandeService.delete(id, user);
        return ResponseEntity.ok(ApiResponse.ok(null, "Demande deleted"));
    }
}
