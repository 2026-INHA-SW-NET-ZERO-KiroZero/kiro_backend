package com.kirozero.netzero.domain.session.repository;

import com.kirozero.netzero.domain.session.entity.SessionIngredient;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionIngredientRepository extends JpaRepository<SessionIngredient, Long> {

    long countByParticipantId(Long participantId);

    @EntityGraph(attributePaths = "ingredient")
    List<SessionIngredient> findByParticipantIdOrderByIdAsc(Long participantId);

    @EntityGraph(attributePaths = "ingredient")
    List<SessionIngredient> findBySlotIdOrderByIdAsc(Long slotId);

    void deleteByParticipantId(Long participantId);
}
