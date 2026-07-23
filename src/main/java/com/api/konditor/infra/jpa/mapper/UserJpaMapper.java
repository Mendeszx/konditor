package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.User;
import com.api.konditor.infra.jpa.entity.UsuarioJpaEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper MapStruct entre {@link UsuarioJpaEntity} (infra) e {@link User} (domínio).
 *
 * <p>Os campos da entidade JPA estão em português e os do domínio em inglês — todo mapeamento é
 * explícito e {@code unmappedTargetPolicy = ERROR} garante que um campo esquecido vire erro de
 * compilação (ver KON-56).
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserJpaMapper {

  @Mapping(target = "name", source = "nome")
  @Mapping(target = "googleId", source = "idGoogle")
  @Mapping(target = "locale", source = "idioma")
  @Mapping(target = "createdAt", source = "criadoEm")
  @Mapping(target = "updatedAt", source = "atualizadoEm")
  @Mapping(target = "deletedAt", source = "excluidoEm")
  @Mapping(target = "createdBy", source = "criadoPor")
  @Mapping(target = "updatedBy", source = "atualizadoPor")
  User toDomain(UsuarioJpaEntity entity);

  @InheritInverseConfiguration
  UsuarioJpaEntity toJpa(User domain);
}
