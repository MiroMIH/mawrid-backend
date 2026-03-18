package com.mawrid.demande;

import com.mawrid.category.Category;
import com.mawrid.category.CategoryService;
import com.mawrid.common.exception.BusinessException;
import com.mawrid.common.exception.ResourceNotFoundException;
import com.mawrid.demande.dto.DemandeAttributeDto;
import com.mawrid.demande.dto.DemandeRequest;
import com.mawrid.demande.dto.DemandeResponse;
import com.mawrid.demande.dto.DemandeSummaryResponse;
import com.mawrid.matching.MatchingOrchestrator;
import com.mawrid.reponse.ReponseRepository;
import com.mawrid.reponse.ReponseStatus;
import com.mawrid.reponse.dto.ReponseResponse;
import com.mawrid.reponse.ReponseMapper;
import com.mawrid.scoring.DemandeSupplierScore;
import com.mawrid.scoring.DemandeSupplierScoreRepository;
import com.mawrid.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DemandeService {

    private final DemandeRepository demandeRepository;
    private final DemandeAttributeRepository attributeRepository;
    private final CategoryService categoryService;
    private final DemandeMapper demandeMapper;
    private final ReponseRepository reponseRepository;
    private final ReponseMapper reponseMapper;
    private final DemandeSupplierScoreRepository scoreRepository;
    private final MatchingOrchestrator matchingOrchestrator;

    // ── Create ────────────────────────────────────────────────────

    @Transactional
    public DemandeResponse create(DemandeRequest request, User buyer) {
        Category category = categoryService.findOrThrow(request.getCategoryId());

        if (request.getDeadline() != null && !request.getDeadline().isAfter(LocalDate.now())) {
            throw new BusinessException("Deadline must be at least 1 day in the future");
        }

        Demande demande = Demande.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .quantity(request.getQuantity())
                .unit(request.getUnit() != null ? request.getUnit() : "unités")
                .deadline(request.getDeadline())
                .wilaya(request.getWilaya() != null ? request.getWilaya() : buyer.getWilaya())
                .qualityScore(computeQualityScore(request, category))
                .status(DemandeStatus.OPEN)
                .category(category)
                .buyer(buyer)
                .build();

        Demande saved = demandeRepository.save(demande);

        if (request.getAttributes() != null) {
            for (DemandeAttributeDto dto : request.getAttributes()) {
                attributeRepository.save(DemandeAttribute.builder()
                        .demande(saved)
                        .key(dto.getKey())
                        .value(dto.getValue())
                        .custom(dto.isCustom())
                        .build());
            }
        }

        // Async: match suppliers → score → schedule notifications
        matchingOrchestrator.run(saved);

        return demandeMapper.toResponse(saved);
    }

    // ── Buyer list / detail ────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<DemandeSummaryResponse> listMyDemandes(User buyer, DemandeStatus status, Pageable pageable) {
        Page<Demande> page = (status != null)
                ? demandeRepository.findByBuyerAndStatus(buyer, status, pageable)
                : demandeRepository.findByBuyer(buyer, pageable);
        return page.map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public DemandeResponse getByIdForBuyer(UUID id, User buyer) {
        Demande demande = findOrThrow(id);
        if (!demande.getBuyer().getId().equals(buyer.getId())) {
            throw new BusinessException("You are not the owner of this demande", HttpStatus.FORBIDDEN);
        }
        return demandeMapper.toResponse(demande);
    }

    @Transactional(readOnly = true)
    public DemandeResponse getById(UUID id) {
        return demandeMapper.toResponse(findOrThrow(id));
    }

    // ── Demande responses (buyer view) ────────────────────────────

    @Transactional(readOnly = true)
    public Page<ReponseResponse> getDemandeReponses(UUID demandeId, User buyer, Pageable pageable) {
        Demande demande = findOrThrow(demandeId);
        if (!demande.getBuyer().getId().equals(buyer.getId())) {
            throw new BusinessException("Access denied: not your demande", HttpStatus.FORBIDDEN);
        }

        // Build score map for this demande
        Map<UUID, Integer> scoreMap = scoreRepository.findByDemande(demande).stream()
                .collect(Collectors.toMap(
                        s -> s.getSupplier().getId(),
                        DemandeSupplierScore::getFinalScore
                ));

        return reponseRepository.findByDemande(demande, pageable)
                .map(r -> {
                    ReponseResponse base = reponseMapper.toResponse(r);
                    int score = scoreMap.getOrDefault(r.getSupplier().getId(), 0);
                    return ReponseResponse.builder()
                            .id(base.getId())
                            .demandeId(base.getDemandeId())
                            .demandeTitle(base.getDemandeTitle())
                            .supplierId(base.getSupplierId())
                            .supplierEmail(base.getSupplierEmail())
                            .supplierFirstName(base.getSupplierFirstName())
                            .supplierLastName(base.getSupplierLastName())
                            .supplierPhone(base.getSupplierPhone())
                            .supplierCompanyName(base.getSupplierCompanyName())
                            .supplierWilaya(base.getSupplierWilaya())
                            .supplierScore(score)
                            .status(base.getStatus())
                            .note(base.getNote())
                            .createdAt(base.getCreatedAt())
                            .updatedAt(base.getUpdatedAt())
                            .build();
                });
    }

    // ── Status changes ─────────────────────────────────────────────

    @Transactional
    public DemandeResponse closeDemande(UUID id, User buyer) {
        Demande demande = findOrThrow(id);
        assertOwner(demande, buyer);
        if (demande.getStatus() != DemandeStatus.OPEN) {
            throw new BusinessException("Cannot close a demande that is not OPEN");
        }
        demande.setStatus(DemandeStatus.CLOSED);
        demande.setClosedAt(LocalDateTime.now());
        return demandeMapper.toResponse(demandeRepository.save(demande));
    }

    @Transactional
    public void cancelDemande(UUID id, User buyer) {
        Demande demande = findOrThrow(id);
        assertOwner(demande, buyer);
        if (demande.getStatus() != DemandeStatus.OPEN) {
            throw new BusinessException("Cannot cancel a demande that is not OPEN");
        }
        long disponibleCount = reponseRepository.countByDemandeIdAndStatus(demande.getId(), ReponseStatus.DISPONIBLE);
        if (disponibleCount > 0) {
            throw new BusinessException("Cannot cancel: " + disponibleCount + " supplier(s) have confirmed availability",
                    HttpStatus.CONFLICT);
        }
        demande.setStatus(DemandeStatus.CANCELLED);
        demandeRepository.save(demande);
    }

    // ── Admin operations ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<DemandeSummaryResponse> listAll(DemandeStatus status, Pageable pageable) {
        Page<Demande> page = (status != null)
                ? demandeRepository.findByStatus(status, pageable)
                : demandeRepository.findAll(pageable);
        return page.map(this::toSummary);
    }

    @Transactional
    public DemandeResponse forceClose(UUID id) {
        Demande demande = findOrThrow(id);
        demande.setStatus(DemandeStatus.CLOSED);
        demande.setClosedAt(LocalDateTime.now());
        return demandeMapper.toResponse(demandeRepository.save(demande));
    }

    @Transactional
    public DemandeResponse expire(UUID id) {
        Demande demande = findOrThrow(id);
        demande.setStatus(DemandeStatus.EXPIRED);
        demande.setExpiredAt(LocalDateTime.now());
        return demandeMapper.toResponse(demandeRepository.save(demande));
    }

    @Transactional
    public DemandeResponse recategorize(UUID id, Long newCategoryId, MatchingOrchestrator orchestrator) {
        Demande demande = findOrThrow(id);
        Category newCategory = categoryService.findOrThrow(newCategoryId);
        demande.setCategory(newCategory);
        Demande saved = demandeRepository.save(demande);
        // Re-run matching for new category
        orchestrator.run(saved);
        return demandeMapper.toResponse(saved);
    }

    // ── Quality score ─────────────────────────────────────────────

    private int computeQualityScore(DemandeRequest request, Category category) {
        int score = 20;
        if (category.getDepth() >= 3) score += 10;
        if (request.getAttributes() != null) {
            long mandatory = request.getAttributes().stream().filter(a -> !a.isCustom()).count();
            if (mandatory > 0) score += 25;
            long custom = request.getAttributes().stream().filter(DemandeAttributeDto::isCustom).count();
            if (custom >= 2) score += 10;
        }
        if (request.getDescription() != null && request.getDescription().length() >= 50) score += 15;
        if (request.getDeadline() != null && request.getDeadline().isAfter(LocalDate.now().plusDays(3))) score += 10;
        return Math.min(score, 100);
    }

    // ── Summary builder ────────────────────────────────────────────

    public DemandeSummaryResponse toSummary(Demande demande) {
        long total = reponseRepository.countByDemandeId(demande.getId());
        long disponible = reponseRepository.countByDemandeIdAndStatus(demande.getId(), ReponseStatus.DISPONIBLE);
        long days = demande.getDeadline() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), demande.getDeadline()) : 0;
        return DemandeSummaryResponse.builder()
                .id(demande.getId())
                .title(demande.getTitle())
                .quantity(demande.getQuantity())
                .unit(demande.getUnit())
                .deadline(demande.getDeadline())
                .status(demande.getStatus())
                .qualityScore(demande.getQualityScore())
                .categoryId(demande.getCategory().getId())
                .categoryName(demande.getCategory().getName())
                .buyerWilaya(demande.getBuyer().getWilaya())
                .totalReponses((int) total)
                .disponibleCount((int) disponible)
                .createdAt(demande.getCreatedAt())
                .daysUntilDeadline(days)
                .build();
    }

    // ── Helpers ─────────────────────────────────────────────────

    public Demande findOrThrow(UUID id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", id));
    }

    private void assertOwner(Demande demande, User user) {
        if (!demande.getBuyer().getId().equals(user.getId())) {
            throw new BusinessException("You are not the owner of this demande", HttpStatus.FORBIDDEN);
        }
    }
}
