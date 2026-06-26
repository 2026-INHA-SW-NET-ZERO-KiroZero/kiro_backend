package com.kirozero.netzero.domain.vote.repository;

import com.kirozero.netzero.domain.vote.entity.MenuVote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuVoteRepository extends JpaRepository<MenuVote, Long> {

    boolean existsBySlotIdAndVoterIdAndRecommendationCount(Long slotId, Long voterId, int recommendationCount);

    List<MenuVote> findBySlotIdAndRecommendationCount(Long slotId, int recommendationCount);
}
