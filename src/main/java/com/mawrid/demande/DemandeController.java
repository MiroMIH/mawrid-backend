package com.mawrid.demande;

import com.mawrid.common.response.ApiResponse;
import com.mawrid.demande.dto.DemandeRequest;
import com.mawrid.demande.dto.DemandeResponse;
import com.mawrid.demande.dto.DemandeSummaryResponse;
import com.mawrid.reponse.dto.ReponseResponse;
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
@RequestMapping("/api/v1/buyer/demandes")
@RequiredArgsConstructor
@Tag(name = "Demandes", description = "Buyer demande management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('BUYER')")
public class DemandeController {

    private final DemandeService demandeService;

    @PostMapping
    @Operation(summary = "Create a demande")
    public ResponseEntity<ApiResponse<DemandeResponse>> create(
            @Valid @RequestBody DemandeRequest request,
            @AuthenticationPrincipal User buyer
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(demandeService.create(request, buyer), "Demande created"));
    }

    @GetMapping
    @Operation(summary = "List my demandes (paginated)")
    public ResponseEntity<ApiResponse<Page<DemandeSummaryResponse>>> listMy(
            @AuthenticationPrincipal User buyer,
            @RequestParam(required = false) DemandeStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(demandeService.listMyDemandes(buyer, status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get demande detail (owner only)")
    public ResponseEntity<ApiResponse<DemandeResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User buyer
    ) {
        return ResponseEntity.ok(ApiResponse.ok(demandeService.getByIdForBuyer(id, buyer)));
    }

    @GetMapping("/{id}/reponses")
    @Operation(summary = "Get all responses for my demande ranked by score")
    public ResponseEntity<ApiResponse<Page<ReponseResponse>>> getReponses(
            @PathVariable UUID id,
            @AuthenticationPrincipal User buyer,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(demandeService.getDemandeReponses(id, buyer, pageable)));
    }

    @PatchMapping("/{id}/close")
    @Operation(summary = "Close my demande manually")
    public ResponseEntity<ApiResponse<DemandeResponse>> close(
            @PathVariable UUID id,
            @AuthenticationPrincipal User buyer
    ) {
        return ResponseEntity.ok(ApiResponse.ok(demandeService.closeDemande(id, buyer)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel demande (soft delete → CANCELLED)")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal User buyer
    ) {
        demandeService.cancelDemande(id, buyer);
        return ResponseEntity.ok(ApiResponse.ok(null, "Demande cancelled"));
    }
}
