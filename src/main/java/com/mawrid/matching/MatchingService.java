package com.mawrid.matching;

import com.mawrid.demande.Demande;
import com.mawrid.user.User;
import com.mawrid.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    private final UserRepository userRepository;

    /**
     * Find all active suppliers whose subscribed category is equal to or
     * an ancestor of the demande's category (path-prefix match).
     */
    @Transactional(readOnly = true)
    public List<User> findEligibleSuppliers(Demande demande) {
        String demandePath = demande.getCategory().getPath();
        List<User> suppliers = userRepository.findEligibleSuppliers(demandePath);

        log.info("Matched {} suppliers for demande '{}' (category path: {})",
                suppliers.size(), demande.getTitle(), demandePath);

        return suppliers;
    }
}
