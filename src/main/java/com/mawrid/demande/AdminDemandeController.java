package com.mawrid.demande;

import com.mawrid.common.response.ApiResponse;
import com.mawrid.demande.dto.DemandeSummaryResponse;
import com.mawrid.demande.dto.RecategorizeRequest;
import com.mawrid.demande.dto.DemandeResponse;
import com.mawrid.matching.MatchingOrchestrator;
import com.mawrid.scoring.dto.ScoreBreakdownResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/demandes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Demandes", description = "Admin demande oversight")
public class AdminDemandeController {

    private final DemandeService demandeService;
    private final AdminDemandeService adminDemandeService;
    private final MatchingOrchestrator matchingOrchestrator;

    @GetMapping
    @Operation(summary = "List all demandes (paginated, all filters)")
    public ResponseEntity<ApiResponse<Page<DemandeSummaryResponse>>> listAll(
            @RequestParam(required = false) DemandeStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(demandeService.listAll(status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full demande detail + all responses + scores")
    public ResponseEntity<ApiResponse<DemandeResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(demandeService.getById(id)));
    }

    @GetMapping("/{id}/scores")
    @Operation(summary = "Get score breakdown for all matched suppliers")
    public ResponseEntity<ApiResponse<List<ScoreBreakdownResponse>>> getScores(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminDemandeService.getScoreBreakdown(id)));
    }

    @PatchMapping("/{id}/force-close")
    @Operation(summary = "Force close any demande")
    public ResponseEntity<ApiResponse<DemandeResponse>> forceClose(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(demandeService.forceClose(id)));
    }

    @PatchMapping("/{id}/recategorize")
    @Operation(summary = "Move demande to different category and re-run matching")
    public ResponseEntity<ApiResponse<DemandeResponse>> recategorize(
            @PathVariable UUID id,
            @Valid @RequestBody RecategorizeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                demandeService.recategorize(id, request.getNewCategoryId(), matchingOrchestrator)));
    }

    @PatchMapping("/{id}/expire")
    @Operation(summary = "Mark demande as EXPIRED manually")
    public ResponseEntity<ApiResponse<DemandeResponse>> expire(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(demandeService.expire(id)));
    }

    @GetMapping("/stats")
    @Operation(summary = "Platform demande statistics")
    public ResponseEntity<ApiResponse<AdminDemandeStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(adminDemandeService.getStats()));
    }
}
