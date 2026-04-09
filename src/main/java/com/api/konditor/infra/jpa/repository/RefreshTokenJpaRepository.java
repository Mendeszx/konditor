package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.RefreshTokenJpaEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

  Optional<RefreshTokenJpaEntity> findByToken(String token);

  @Modifying
  @Query(
      "UPDATE RefreshTokenJpaEntity rt SET rt.revoked = true WHERE rt.userId = :userId AND"
          + " rt.revoked = false")
  void revokeAllByUserId(UUID userId);

  void deleteByExpiresAtBefore(Instant threshold);
}
