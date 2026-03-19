package com.mawrid.reponse;

import com.mawrid.common.response.ApiResponse;
import com.mawrid.demande.dto.DemandeSummaryResponse;
import com.mawrid.reponse.dto.ReponseRequest;
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
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
@Tag(name = "Réponses", description = "Supplier feed and responses")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('SUPPLIER')")
public class ReponseController {

    private final ReponseService reponseService;

    @GetMapping("/feed")
    @Operation(summary = "Get supplier's personalized demande feed ordered by score")
    public ResponseEntity<ApiResponse<Page<DemandeSummaryResponse>>> getFeed(
            @AuthenticationPrincipal User supplier,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reponseService.getSupplierFeed(supplier, categoryId, pageable)));
    }

    @GetMapping("/feed/{demandeId}")
    @Operation(summary = "Get single demande detail for supplier")
    public ResponseEntity<ApiResponse<DemandeSummaryResponse>> getFeedItem(
            @PathVariable UUID demandeId,
            @AuthenticationPrincipal User supplier
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reponseService.getFeedItem(demandeId, supplier)));
    }

    @PostMapping("/reponses/{demandeId}")
    @Operation(summary = "Submit response (DISPONIBLE or INDISPONIBLE)")
    public ResponseEntity<ApiResponse<ReponseResponse>> respond(
            @PathVariable UUID demandeId,
            @Valid @RequestBody ReponseRequest request,
            @AuthenticationPrincipal User supplier
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(reponseService.respond(demandeId, request, supplier), "Response submitted"));
    }

    @PatchMapping("/reponses/{demandeId}")
    @Operation(summary = "Update response (only within 1h of initial submission)")
    public ResponseEntity<ApiResponse<ReponseResponse>> updateResponse(
            @PathVariable UUID demandeId,
            @Valid @RequestBody ReponseRequest request,
            @AuthenticationPrincipal User supplier
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reponseService.updateResponse(demandeId, request, supplier)));
    }

    @GetMapping("/reponses")
    @Operation(summary = "List all my responses (paginated)")
    public ResponseEntity<ApiResponse<Page<ReponseResponse>>> listMy(
            @AuthenticationPrincipal User supplier,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reponseService.listMyReponses(supplier, pageable)));
    }
}
