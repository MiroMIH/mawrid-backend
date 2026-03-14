package com.mawrid.reponse;

import com.mawrid.common.exception.BusinessException;
import com.mawrid.common.exception.DuplicateResourceException;
import com.mawrid.demande.Demande;
import com.mawrid.demande.DemandeService;
import com.mawrid.demande.DemandeStatus;
import com.mawrid.reponse.dto.ReponseRequest;
import com.mawrid.reponse.dto.ReponseResponse;
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
public class ReponseService {

    private final ReponseRepository reponseRepository;
    private final DemandeService demandeService;
    private final ReponseMapper reponseMapper;

    @Transactional
    public ReponseResponse respond(UUID demandeId, ReponseRequest request, User supplier) {
        Demande demande = demandeService.findOrThrow(demandeId);

        if (demande.getStatus() != DemandeStatus.OPEN) {
            throw new BusinessException("Cannot respond to a demande that is not OPEN");
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
