package com.mawrid.reponse;

import com.mawrid.common.exception.BusinessException;
import com.mawrid.common.exception.DuplicateResourceException;
import com.mawrid.demande.Demande;
import com.mawrid.demande.DemandeService;
import com.mawrid.demande.DemandeStatus;
import com.mawrid.demande.dto.DemandeSummaryResponse;
import com.mawrid.reponse.dto.ReponseRequest;
import com.mawrid.reponse.dto.ReponseResponse;
import com.mawrid.scoring.DemandeSupplierScore;
import com.mawrid.scoring.DemandeSupplierScoreId;
import com.mawrid.scoring.DemandeSupplierScoreRepository;
import com.mawrid.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReponseService {

    private final ReponseRepository reponseRepository;
    private final DemandeService demandeService;
    private final DemandeSupplierScoreRepository scoreRepository;
    private final ReponseMapper reponseMapper;

    @Transactional
    public ReponseResponse respond(UUID demandeId, ReponseRequest request, User supplier) {
        Demande demande = demandeService.findOrThrow(demandeId);

        if (demande.getStatus() != DemandeStatus.OPEN) {
            throw new BusinessException("Cannot respond to a " + demande.getStatus() + " demande");
        }

        // Check supplier is matched (has a score record)
        DemandeSupplierScoreId scoreId = new DemandeSupplierScoreId(demandeId, supplier.getId());
        if (!scoreRepository.existsById(scoreId)) {
            throw new BusinessException("You are not matched to this demande", HttpStatus.FORBIDDEN);
        }

        if (reponseRepository.existsByDemandeIdAndSupplierId(demandeId, supplier.getId())) {
            throw new DuplicateResourceException("You have already responded to this demande");
        }

        Reponse reponse = Reponse.builder()
                .demande(demande)
                .supplier(supplier)
                .status(request.getStatus())
                .note(request.getNote())
                .build();

        return reponseMapper.toResponse(reponseRepository.save(reponse));
    }

    @Transactional
    public ReponseResponse updateResponse(UUID demandeId, ReponseRequest request, User supplier) {
        Demande demande = demandeService.findOrThrow(demandeId);

        if (demande.getStatus() != DemandeStatus.OPEN) {
            throw new BusinessException("Cannot update response to a " + demande.getStatus() + " demande");
        }

        Reponse reponse = reponseRepository.findByDemandeIdAndSupplierId(demandeId, supplier.getId())
                .orElseThrow(() -> new BusinessException("No response found for this demande"));

        // Only within 1 hour of initial submission
        if (reponse.getCreatedAt() != null) {
            long minutesSinceCreation = ChronoUnit.MINUTES.between(reponse.getCreatedAt(), LocalDateTime.now());
            if (minutesSinceCreation > 60) {
                throw new BusinessException("Response can only be updated within 1 hour of submission");
            }
        }

        reponse.setStatus(request.getStatus());
        reponse.setNote(request.getNote());

        return reponseMapper.toResponse(reponseRepository.save(reponse));
    }

    @Transactional(readOnly = true)
    public Page<DemandeSummaryResponse> getSupplierFeed(User supplier, Long categoryId, Pageable pageable) {
        // Get scores for this supplier on open demandes, ordered by finalScore desc
        Page<DemandeSupplierScore> scores = scoreRepository.findSupplierFeedPaged(
                supplier.getId(), DemandeStatus.OPEN, categoryId, pageable);

        return scores.map(score -> {
            Demande demande = score.getDemande();
            DemandeSummaryResponse summary = demandeService.toSummary(demande);

            // Add supplier's own response if any
            Optional<Reponse> myReponse = reponseRepository.findByDemandeIdAndSupplierId(
                    demande.getId(), supplier.getId());

            DemandeSummaryResponse.SupplierResponseSummary supplierResponse = myReponse
                    .map(r -> DemandeSummaryResponse.SupplierResponseSummary.builder()
                            .status(r.getStatus())
                            .createdAt(r.getCreatedAt())
                            .build())
                    .orElse(null);

            return DemandeSummaryResponse.builder()
                    .id(summary.getId())
                    .title(summary.getTitle())
                    .quantity(summary.getQuantity())
                    .unit(summary.getUnit())
                    .deadline(summary.getDeadline())
                    .status(summary.getStatus())
                    .qualityScore(summary.getQualityScore())
                    .categoryId(summary.getCategoryId())
                    .categoryName(summary.getCategoryName())
                    .buyerWilaya(summary.getBuyerWilaya())
                    .totalReponses(summary.getTotalReponses())
                    .disponibleCount(summary.getDisponibleCount())
                    .createdAt(summary.getCreatedAt())
                    .daysUntilDeadline(summary.getDaysUntilDeadline())
                    .finalScore(score.getFinalScore())
                    .categoryScore(score.getCategoryScore())
                    .proximityScore(score.getProximityScore())
                    .urgencyScore(score.getUrgencyScore())
                    .supplierResponse(supplierResponse)
                    .build();
        });
    }

    @Transactional(readOnly = true)
    public DemandeSummaryResponse getFeedItem(UUID demandeId, User supplier) {
        Demande demande = demandeService.findOrThrow(demandeId);

        DemandeSupplierScoreId scoreId = new DemandeSupplierScoreId(demandeId, supplier.getId());
        DemandeSupplierScore score = scoreRepository.findById(scoreId)
                .orElseThrow(() -> new BusinessException("You are not matched to this demande", HttpStatus.FORBIDDEN));

        DemandeSummaryResponse summary = demandeService.toSummary(demande);
        Optional<Reponse> myReponse = reponseRepository.findByDemandeIdAndSupplierId(demandeId, supplier.getId());

        DemandeSummaryResponse.SupplierResponseSummary supplierResponse = myReponse
                .map(r -> DemandeSummaryResponse.SupplierResponseSummary.builder()
                        .status(r.getStatus())
                        .createdAt(r.getCreatedAt())
                        .build())
                .orElse(null);

        return DemandeSummaryResponse.builder()
                .id(summary.getId())
                .title(summary.getTitle())
                .quantity(summary.getQuantity())
                .unit(summary.getUnit())
                .deadline(summary.getDeadline())
                .status(summary.getStatus())
                .qualityScore(summary.getQualityScore())
                .categoryId(summary.getCategoryId())
                .categoryName(summary.getCategoryName())
                .buyerWilaya(summary.getBuyerWilaya())
                .totalReponses(summary.getTotalReponses())
                .disponibleCount(summary.getDisponibleCount())
                .createdAt(summary.getCreatedAt())
                .daysUntilDeadline(summary.getDaysUntilDeadline())
                .finalScore(score.getFinalScore())
                .categoryScore(score.getCategoryScore())
                .proximityScore(score.getProximityScore())
                .urgencyScore(score.getUrgencyScore())
                .supplierResponse(supplierResponse)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ReponseResponse> listMyReponses(User supplier, Pageable pageable) {
        return reponseRepository.findBySupplierId(supplier.getId(), pageable)
                .map(reponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReponseResponse> getAvailableSuppliers(UUID demandeId, User buyer, Pageable pageable) {
        Demande demande = demandeService.findOrThrow(demandeId);
        if (!demande.getBuyer().getId().equals(buyer.getId())) {
            throw new BusinessException("Access denied: not your demande", HttpStatus.FORBIDDEN);
        }
        return reponseRepository
                .findByDemandeAndStatus(demande, ReponseStatus.DISPONIBLE, pageable)
                .map(reponseMapper::toResponse);
    }
}
