package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.PapelJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para {@link PapelJpaEntity}. */
public interface RoleJpaRepository extends JpaRepository<PapelJpaEntity, String> {

  Optional<PapelJpaEntity> findByName(String name);
}
