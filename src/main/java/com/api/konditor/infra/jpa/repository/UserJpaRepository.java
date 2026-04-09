package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.UserJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para a entidade {@link UserJpaEntity}. */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

  Optional<UserJpaEntity> findByGoogleId(String googleId);

  Optional<UserJpaEntity> findByEmail(String email);
}
