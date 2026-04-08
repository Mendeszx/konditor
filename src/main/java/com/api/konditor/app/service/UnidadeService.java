package com.api.konditor.app.service;

import com.api.konditor.app.controller.response.UnidadeResponse;
import com.api.konditor.domain.enuns.UnitType;
import com.api.konditor.infra.jpa.repository.UnitJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serviço de aplicação para consulta de unidades de medida.
 *
 * <p>Unidades são recursos globais (não pertencem a um workspace)
 * e são usadas como referência nos ingredientes e receitas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnidadeService {

    private final UnitJpaRepository unitRepository;

    /**
     * Lista todas as unidades de medida ativas.
     *
     * @param tipo filtro opcional pelo tipo da unidade ({@code weight}, {@code volume}, {@code unit}).
     *             Quando {@code null}, retorna todas.
     * @return lista de unidades ordenada por nome
     */
    @Transactional(readOnly = true)
    public List<UnidadeResponse> listar(String tipo) {
        log.debug("[UNIDADE-SERVICE] Listando unidades. tipo={}", tipo);

        if (tipo != null && !tipo.isBlank()) {
            UnitType unitType;
            try {
                unitType = UnitType.valueOf(tipo.toLowerCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Tipo de unidade inválido: '" + tipo + "'. Use: weight, volume ou unit.");
            }
            return unitRepository.findAllByTypeAndDeletedAtIsNullOrderByNameAsc(unitType)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        return unitRepository.findAllByDeletedAtIsNullOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UnidadeResponse toResponse(com.api.konditor.infra.jpa.entity.UnitJpaEntity u) {
        return UnidadeResponse.builder()
                .id(u.getId().toString())
                .nome(u.getName())
                .simbolo(u.getSymbol())
                .tipo(u.getType() != null ? u.getType().name() : null)
                .isBase(Boolean.TRUE.equals(u.getIsBase()))
                .build();
    }
}

