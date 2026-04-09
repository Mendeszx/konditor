package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.RoleJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para {@link RoleJpaEntity}. */
public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, String> {

  Optional<RoleJpaEntity> findByName(String name);
}
