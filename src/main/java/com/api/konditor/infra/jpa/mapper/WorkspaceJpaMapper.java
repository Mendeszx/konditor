package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.Workspace;
import com.api.konditor.infra.jpa.entity.EspacoTrabalhoJpaEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper MapStruct entre {@link EspacoTrabalhoJpaEntity} (infra) e {@link Workspace} (domínio). O
 * campo {@code owner} é mapeado recursivamente via {@link UserJpaMapper}.
 *
 * <p>Os campos da entidade JPA estão em português e os do domínio em inglês — todo mapeamento é
 * explícito e {@code unmappedTargetPolicy = ERROR} garante que um campo esquecido vire erro de
 * compilação (ver KON-56, onboarding persistia workspace com nome null).
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {UserJpaMapper.class},
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface WorkspaceJpaMapper {

  @Mapping(target = "name", source = "nome")
  @Mapping(target = "owner", source = "proprietario")
  @Mapping(target = "planId", source = "plano.id")
  @Mapping(target = "currency", source = "moeda")
  @Mapping(target = "createdAt", source = "criadoEm")
  @Mapping(target = "updatedAt", source = "atualizadoEm")
  @Mapping(target = "deletedAt", source = "excluidoEm")
  @Mapping(target = "createdBy", source = "criadoPor")
  @Mapping(target = "updatedBy", source = "atualizadoPor")
  Workspace toDomain(EspacoTrabalhoJpaEntity entity);

  /**
   * O campo {@code plano} exige a entidade {@link
   * com.api.konditor.infra.jpa.entity.DetalhesPlanoJpaEntity} carregada do banco — é resolvido pelo
   * use case após o mapeamento ({@code jpa.setPlano(...)}).
   */
  @InheritInverseConfiguration
  @Mapping(target = "plano", ignore = true)
  EspacoTrabalhoJpaEntity toJpa(Workspace domain);
}
