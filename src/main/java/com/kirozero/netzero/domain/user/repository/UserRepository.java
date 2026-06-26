package com.kirozero.netzero.domain.user.repository;

import com.kirozero.netzero.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "allergies")
    Optional<User> findWithAllergiesByEmail(String email);

    @EntityGraph(attributePaths = "allergies")
    Optional<User> findWithAllergiesById(Long id);
}
