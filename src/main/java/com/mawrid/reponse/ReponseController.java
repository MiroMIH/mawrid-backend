package com.mawrid.reponse;

import com.mawrid.common.response.ApiResponse;
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
@RequestMapping("/api/v1/demandes/{demandeId}/reponses")
@RequiredArgsConstructor
@Tag(name = "Reponses", description = "Supplier responses to procurement requests")
@SecurityRequirement(name = "bearerAuth")
public class ReponseController {

    private final ReponseService reponseService;

    @PostMapping
    @PreAuthorize("hasRole('SUPPLIER')")
    @Operation(summary = "Respond to a demande (supplier only)")
    public ResponseEntity<ApiResponse<ReponseResponse>> respond(
            @PathVariable UUID demandeId,
            @Valid @RequestBody ReponseRequest request,
            @AuthenticationPrincipal User supplier
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(reponseService.respond(demandeId, request, supplier), "Response submitted"));
    }

    @GetMapping
    @PreAuthorize("hasRole('BUYER') or hasRole('ADMIN')")
    @Operation(summary = "Get DISPONIBLE suppliers for a demande (buyer only)")
    public ResponseEntity<ApiResponse<Page<ReponseResponse>>> getAvailable(
            @PathVariable UUID demandeId,
            @AuthenticationPrincipal User buyer,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                reponseService.getAvailableSuppliers(demandeId, buyer, pageable)));
    }
}
