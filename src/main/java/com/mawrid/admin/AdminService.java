package com.mawrid.admin;

import com.mawrid.admin.dto.AdminStatsResponse;
import com.mawrid.admin.dto.SimulationRequest;
import com.mawrid.admin.dto.SimulationResult;
import com.mawrid.category.Category;
import com.mawrid.category.CategoryService;
import com.mawrid.common.exception.ResourceNotFoundException;
import com.mawrid.demande.Demande;
import com.mawrid.demande.DemandeRepository;
import com.mawrid.demande.DemandeStatus;
import com.mawrid.matching.MatchingService;
import com.mawrid.reponse.ReponseRepository;
import com.mawrid.scoring.DemandeScoreEngine;
import com.mawrid.scoring.DemandeSupplierScore;
import com.mawrid.scoring.ScoringConfig;
import com.mawrid.user.Role;
import com.mawrid.user.User;
import com.mawrid.user.UserMapper;
import com.mawrid.user.UserRepository;
import com.mawrid.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final DemandeRepository demandeRepository;
    private final ReponseRepository reponseRepository;
    private final CategoryService categoryService;
    private final MatchingService matchingService;
    private final DemandeScoreEngine scoreEngine;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public byte[] exportUsersCsv() {
        List<User> users = userRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("id,email,firstName,lastName,phone,companyName,wilaya,role,enabled,createdAt\n");
        for (User u : users) {
            sb.append(u.getId()).append(',')
              .append(csv(u.getEmail())).append(',')
              .append(csv(u.getFirstName())).append(',')
              .append(csv(u.getLastName())).append(',')
              .append(csv(u.getPhone())).append(',')
              .append(csv(u.getCompanyName())).append(',')
              .append(csv(u.getWilaya())).append(',')
              .append(u.getRole()).append(',')
              .append(u.isEnabled()).append(',')
              .append(u.getCreatedAt() != null ? u.getCreatedAt().toString() : "")
              .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    @Transactional
    public UserResponse toggleEnabled(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setEnabled(!user.isEnabled());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        long totalUsers     = userRepository.count();
        long totalBuyers    = userRepository.countByRole(Role.BUYER);
        long totalSuppliers = userRepository.countByRole(Role.SUPPLIER);
        long totalDemandes  = demandeRepository.count();
        long openDemandes   = demandeRepository.countByStatus(DemandeStatus.OPEN);
        long totalReponses  = reponseRepository.count();

        double responseRate = totalDemandes > 0
                ? (double) totalReponses / totalDemandes : 0.0;

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalBuyers(totalBuyers)
                .totalSuppliers(totalSuppliers)
                .totalDemandes(totalDemandes)
                .openDemandes(openDemandes)
                .totalReponses(totalReponses)
                .responseRate(responseRate)
                .build();
    }

    /**
     * Dry-run matching simulation — reads only, writes nothing to the DB.
     */
    @Transactional(readOnly = true)
    public List<SimulationResult> simulate(SimulationRequest request) {
        Category category = categoryService.findOrThrow(request.getCategoryId());

        // Build a transient Demande for scoring (never persisted)
        Demande phantom = Demande.builder()
                .id(UUID.randomUUID())
                .title("SIMULATION")
                .category(category)
                .wilaya(request.getWilaya())
                .deadline(request.getDeadline())
                .quantity(request.getQuantity())
                .status(DemandeStatus.OPEN)
                .build();

        List<User> suppliers = matchingService.findEligibleSuppliers(phantom);

        return suppliers.stream()
                .map(supplier -> {
                    DemandeSupplierScore score = scoreEngine.computeScore(phantom, supplier);
                    return SimulationResult.builder()
                            .supplierId(supplier.getId())
                            .supplierEmail(supplier.getEmail())
                            .companyName(supplier.getCompanyName())
                            .wilaya(supplier.getWilaya())
                            .categoryScore(score.getCategoryScore())
                            .proximityScore(score.getProximityScore())
                            .urgencyScore(score.getUrgencyScore())
                            .baseScore(score.getBaseScore())
                            .finalScore(score.getFinalScore())
                            .notifTier(notifTier(score.getFinalScore()))
                            .build();
                })
                .sorted(Comparator.comparingInt(SimulationResult::getFinalScore).reversed())
                .toList();
    }

    private String notifTier(int score) {
        if (score >= 80) return "IMMEDIATE";
        if (score >= 50) return "DELAYED_15M";
        if (score >= 30) return "DELAYED_1H";
        return "IN_APP_ONLY";
    }
}
