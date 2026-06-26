package com.kirozero.netzero.domain.session.repository;

import com.kirozero.netzero.domain.session.entity.SessionParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, Long> {

    long countBySlotId(Long slotId);

    boolean existsBySlotIdAndUserId(Long slotId, Long userId);

    @EntityGraph(attributePaths = "user")
    List<SessionParticipant> findBySlotIdOrderByJoinedAtAsc(Long slotId);

    Optional<SessionParticipant> findBySlotIdAndUserId(Long slotId, Long userId);
}
