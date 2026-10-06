package dev.rangel.redirectservice.domain.repository;

import dev.rangel.redirectservice.domain.model.RedirectableLink;
import org.springframework.data.repository.Repository;

import java.util.Optional;

@org.springframework.stereotype.Repository // used for component scan
public interface RedirectableLinkRepository extends Repository<RedirectableLink, Long> {

    Optional<RedirectableLink> findByCode(String code);
}