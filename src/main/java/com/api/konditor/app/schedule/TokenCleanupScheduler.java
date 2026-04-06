package com.api.konditor.app.schedule;

import com.api.konditor.infra.jpa.repository.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Agendador responsável pela limpeza periódica de refresh tokens expirados.
 *
 * <p>Sem essa limpeza, a tabela {@code refresh_tokens} cresceria indefinidamente.
 * Executa diariamente às 03:00 UTC por padrão (configurável via {@code security.token-cleanup.cron}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenJpaRepository refreshTokenRepository;

    /**
     * Remove todos os refresh tokens com {@code expires_at} anterior ao momento atual.
     */
    @Scheduled(cron = "${security.token-cleanup.cron:0 0 3 * * *}")
    @Transactional
    public void purgarTokensExpirados() {
        Instant agora = Instant.now();
        log.info("[SCHEDULER] Iniciando purga de refresh tokens expirados antes de {}", agora);
        refreshTokenRepository.deleteByExpiresAtBefore(agora);
        log.info("[SCHEDULER] Purga de refresh tokens concluída");
    }
}
