package com.mawrid.demande;

import com.mawrid.category.Category;
import com.mawrid.category.CategoryService;
import com.mawrid.common.exception.BusinessException;
import com.mawrid.common.exception.ResourceNotFoundException;
import com.mawrid.demande.dto.DemandeAttributeDto;
import com.mawrid.demande.dto.DemandeRequest;
import com.mawrid.demande.dto.DemandeResponse;
import com.mawrid.demande.dto.DemandeStatusUpdate;
import com.mawrid.matching.MatchingOrchestrator;
import com.mawrid.user.Role;
import com.mawrid.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemandeService {

    private final DemandeRepository demandeRepository;
    private final DemandeAttributeRepository attributeRepository;
    private final CategoryService categoryService;
    private final DemandeMapper demandeMapper;
    private final MatchingOrchestrator matchingOrchestrator;

    // ── List ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<DemandeResponse> list(User currentUser, Pageable pageable) {
        if (currentUser.getRole() == Role.BUYER) {
            return demandeRepository.findByBuyer(currentUser, pageable)
                    .map(demandeMapper::toResponse);
        }
        // SUPPLIER and ADMIN see all open demandes (supplier feed ordering done by score in a future query)
        return demandeRepository.findAll(pageable).map(demandeMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public DemandeResponse getById(UUID id, User currentUser) {
        Demande demande = findOrThrow(id);
        assertReadAccess(demande, currentUser);
        return demandeMapper.toResponse(demande);
    }

    // ── Create ────────────────────────────────────────────────────

    @Transactional
    public DemandeResponse create(DemandeRequest request, User buyer) {
        Category category = categoryService.findOrThrow(request.getCategoryId());

        // Ghost demande prevention
        if (demandeRepository.existsByBuyerAndCategoryIdAndStatus(buyer, category.getId(), DemandeStatus.OPEN)) {
            throw new BusinessException(
                    "You already have an OPEN demande in this category. Close it before posting a new one.");
        }

        Demande demande = Demande.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .quantity(request.getQuantity())
                .unit(request.getUnit())
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

    // ── Status update ─────────────────────────────────────────────

    @Transactional
    public DemandeResponse updateStatus(UUID id, DemandeStatusUpdate request, User buyer) {
        Demande demande = findOrThrow(id);
        assertOwner(demande, buyer);

        if (request.getStatus() == DemandeStatus.OPEN) {
            throw new BusinessException("Cannot re-open a demande");
        }

        demande.setStatus(request.getStatus());
        return demandeMapper.toResponse(demandeRepository.save(demande));
    }

    @Transactional
    public void delete(UUID id, User buyer) {
        Demande demande = findOrThrow(id);
        assertOwner(demande, buyer);
        demandeRepository.delete(demande);
    }

    // ── Quality score ─────────────────────────────────────────────

    private int computeQualityScore(DemandeRequest request, Category category) {
        int score = 20; // category selected

        if (category.getDepth() >= 3) score += 10;

        if (request.getAttributes() != null) {
            long mandatory = request.getAttributes().stream().filter(a -> !a.isCustom()).count();
            if (mandatory > 0) score += 25;
            long custom = request.getAttributes().stream().filter(DemandeAttributeDto::isCustom).count();
            if (custom >= 2) score += 10;
        }

        if (request.getDescription() != null && request.getDescription().length() >= 50) score += 15;

        if (request.getDeadline() != null &&
                request.getDeadline().isAfter(java.time.LocalDate.now().plusDays(3))) score += 10;

        return Math.min(score, 100);
    }

    // ── Helpers ─────────────────────────────────────────────────

    public Demande findOrThrow(UUID id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", id));
    }

    private void assertOwner(Demande demande, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (!demande.getBuyer().getId().equals(user.getId())) {
            throw new BusinessException("You are not the owner of this demande", HttpStatus.FORBIDDEN);
        }
    }

    private void assertReadAccess(Demande demande, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (user.getRole() == Role.BUYER && !demande.getBuyer().getId().equals(user.getId())) {
            throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
        }
    }
}
