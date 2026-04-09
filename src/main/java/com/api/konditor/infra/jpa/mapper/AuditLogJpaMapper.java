package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.AuditLog;
import com.api.konditor.infra.jpa.entity.AuditLogJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/** Mapper MapStruct entre {@link AuditLogJpaEntity} (infra) e {@link AuditLog} (domínio). */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {WorkspaceJpaMapper.class, UserJpaMapper.class})
public interface AuditLogJpaMapper {

  AuditLog toDomain(AuditLogJpaEntity entity);

  AuditLogJpaEntity toJpa(AuditLog domain);
}
