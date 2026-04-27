package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.UsuarioJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para a entidade {@link UsuarioJpaEntity}. */
public interface UserJpaRepository extends JpaRepository<UsuarioJpaEntity, UUID> {

  Optional<UsuarioJpaEntity> findByGoogleId(String googleId);

  Optional<UsuarioJpaEntity> findByEmail(String email);
}
