package dev.rangel.linkservice.infrastructure.persistence.repository;

import dev.rangel.linkservice.domain.model.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {
    Optional<Link> findByCode(String code);
}