package com.api.konditor.app.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiting por IP nos endpoints públicos de autenticação ({@code POST /auth/google} e {@code
 * POST /auth/refresh}).
 *
 * <p>Esses endpoints não exigem token e por isso são o alvo natural de brute-force de refresh
 * tokens e DoS de baixo custo. O filtro usa janela fixa em memória: até {@code max-requests}
 * requisições por IP a cada {@code window-seconds}; excedentes recebem {@code 429 Too Many
 * Requests} com header {@code Retry-After}.
 *
 * <p>Limites configuráveis por variável de ambiente ({@code RATE_LIMIT_*} — ver {@code
 * application.yaml}). Por padrão o IP considerado é o {@code remoteAddr} da conexão; atrás de um
 * proxy/load balancer confiável, habilite {@code trust-x-forwarded-for} para usar o primeiro IP do
 * header {@code X-Forwarded-For}.
 *
 * <p>O estado é local à instância (adequado para deploy de instância única). Com múltiplas
 * réplicas, cada uma aplica o limite de forma independente.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthRateLimitFilter extends OncePerRequestFilter {

  /** Acima deste número de IPs rastreados, janelas expiradas são removidas do mapa. */
  private static final int LIMIAR_LIMPEZA = 10_000;

  private final boolean habilitado;
  private final int maxRequisicoes;
  private final long janelaEmSegundos;
  private final boolean confiarXForwardedFor;
  private final ObjectMapper objectMapper;

  private final ConcurrentHashMap<String, Janela> janelasPorIp = new ConcurrentHashMap<>();

  /** Janela fixa de contagem: início (epoch segundos) + total de requisições no período. */
  private record Janela(long inicioEpochSegundos, AtomicInteger contagem) {}

  public AuthRateLimitFilter(
      @Value("${security.rate-limit.enabled:true}") boolean habilitado,
      @Value("${security.rate-limit.max-requests:10}") int maxRequisicoes,
      @Value("${security.rate-limit.window-seconds:60}") long janelaEmSegundos,
      @Value("${security.rate-limit.trust-x-forwarded-for:false}") boolean confiarXForwardedFor,
      ObjectMapper objectMapper) {
    this.habilitado = habilitado;
    this.maxRequisicoes = maxRequisicoes;
    this.janelaEmSegundos = janelaEmSegundos;
    this.confiarXForwardedFor = confiarXForwardedFor;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!habilitado || !"POST".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    String uri = request.getRequestURI();
    return !uri.equals("/auth/google") && !uri.equals("/auth/refresh");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String ip = resolverIp(request);
    long agora = Instant.now().getEpochSecond();

    Janela janela =
        janelasPorIp.compute(
            ip,
            (chave, atual) ->
                atual == null || agora - atual.inicioEpochSegundos() >= janelaEmSegundos
                    ? new Janela(agora, new AtomicInteger())
                    : atual);

    if (janela.contagem().incrementAndGet() > maxRequisicoes) {
      long retryAfter = Math.max(1, janela.inicioEpochSegundos() + janelaEmSegundos - agora);
      log.warn(
          "[RATE-LIMIT] IP {} excedeu {} requisições em {}s no endpoint {}",
          ip,
          maxRequisicoes,
          janelaEmSegundos,
          request.getRequestURI());
      responder429(request, response, retryAfter);
      return;
    }

    limparJanelasExpiradasSeNecessario(agora);
    filterChain.doFilter(request, response);
  }

  private String resolverIp(HttpServletRequest request) {
    if (confiarXForwardedFor) {
      String xff = request.getHeader("X-Forwarded-For");
      if (xff != null && !xff.isBlank()) {
        return xff.split(",")[0].trim();
      }
    }
    return request.getRemoteAddr();
  }

  private void responder429(
      HttpServletRequest request, HttpServletResponse response, long retryAfterSegundos)
      throws IOException {
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader("Retry-After", String.valueOf(retryAfterSegundos));
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    Map<String, Object> corpo =
        Map.of(
            "timestamp",
            Instant.now().toString(),
            "status",
            429,
            "error",
            "Muitas requisições",
            "message",
            "Limite de requisições excedido. Tente novamente em "
                + retryAfterSegundos
                + " segundos.",
            "path",
            request.getRequestURI());

    objectMapper.writeValue(response.getOutputStream(), corpo);
  }

  /** Evita crescimento sem limite do mapa quando muitos IPs distintos acessam a API. */
  private void limparJanelasExpiradasSeNecessario(long agora) {
    if (janelasPorIp.size() > LIMIAR_LIMPEZA) {
      janelasPorIp
          .entrySet()
          .removeIf(entry -> agora - entry.getValue().inicioEpochSegundos() >= janelaEmSegundos);
    }
  }
}
