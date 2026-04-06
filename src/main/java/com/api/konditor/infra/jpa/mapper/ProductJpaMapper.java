package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.Product;
import com.api.konditor.infra.jpa.entity.ProductJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct entre {@link ProductJpaEntity} (infra) e {@link Product} (domínio).
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {WorkspaceJpaMapper.class, UserJpaMapper.class})
public interface ProductJpaMapper {

    Product toDomain(ProductJpaEntity entity);

    ProductJpaEntity toJpa(Product domain);
}
