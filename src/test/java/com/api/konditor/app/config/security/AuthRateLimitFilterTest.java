package com.api.konditor.app.config.security;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Testes unitários de {@link AuthRateLimitFilter} (KON-21): janela por IP, resposta 429 com {@code
 * Retry-After} e escopo restrito aos endpoints públicos de auth.
 */
class AuthRateLimitFilterTest {

  private static final int LIMITE = 3;
  private static final long JANELA_SEGUNDOS = 60;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private AuthRateLimitFilter novoFiltro(boolean habilitado, boolean confiarXff) {
    return new AuthRateLimitFilter(habilitado, LIMITE, JANELA_SEGUNDOS, confiarXff, objectMapper);
  }

  private MockHttpServletRequest requisicao(String metodo, String uri, String ip) {
    MockHttpServletRequest request = new MockHttpServletRequest(metodo, uri);
    request.setRequestURI(uri);
    request.setRemoteAddr(ip);
    return request;
  }

  /** Executa a requisição no filtro e retorna a resposta; chainInvocado indica se passou. */
  private record Resultado(MockHttpServletResponse response, boolean chainInvocado) {}

  private Resultado executar(AuthRateLimitFilter filtro, MockHttpServletRequest request)
      throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    filtro.doFilter(request, response, chain);
    return new Resultado(response, chain.getRequest() != null);
  }

  @Nested
  @DisplayName("Aplicação do limite")
  class AplicacaoDoLimite {

    @Test
    @DisplayName("Requisições dentro do limite passam; excedentes recebem 429 com Retry-After")
    void acimaDoLimite_retorna429ComRetryAfter() throws Exception {
      AuthRateLimitFilter filtro = novoFiltro(true, false);

      for (int i = 0; i < LIMITE; i++) {
        Resultado ok = executar(filtro, requisicao("POST", "/auth/google", "10.0.0.1"));
        assertThat(ok.chainInvocado()).as("requisição %d dentro do limite", i + 1).isTrue();
      }

      Resultado bloqueada = executar(filtro, requisicao("POST", "/auth/google", "10.0.0.1"));

      assertThat(bloqueada.chainInvocado()).isFalse();
      assertThat(bloqueada.response().getStatus()).isEqualTo(429);
      assertThat(bloqueada.response().getHeader("Retry-After")).isNotNull();
      long retryAfter = Long.parseLong(bloqueada.response().getHeader("Retry-After"));
      assertThat(retryAfter).isBetween(1L, JANELA_SEGUNDOS);
      assertThat(bloqueada.response().getContentAsString()).contains("429");
    }

    @Test
    @DisplayName("O limite é contado por IP — IPs distintos não interferem entre si")
    void limitePorIp_ipsIndependentes() throws Exception {
      AuthRateLimitFilter filtro = novoFiltro(true, false);

      for (int i = 0; i < LIMITE + 1; i++) {
        executar(filtro, requisicao("POST", "/auth/google", "10.0.0.1"));
      }

      Resultado outroIp = executar(filtro, requisicao("POST", "/auth/google", "10.0.0.2"));
      assertThat(outroIp.chainInvocado()).isTrue();
      assertThat(outroIp.response().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("/auth/google e /auth/refresh compartilham a mesma janela por IP")
    void googleERefresh_compartilhamJanela() throws Exception {
      AuthRateLimitFilter filtro = novoFiltro(true, false);

      for (int i = 0; i < LIMITE; i++) {
        executar(filtro, requisicao("POST", "/auth/google", "10.0.0.9"));
      }

      Resultado bloqueada = executar(filtro, requisicao("POST", "/auth/refresh", "10.0.0.9"));
      assertThat(bloqueada.response().getStatus()).isEqualTo(429);
    }
  }

  @Nested
  @DisplayName("Escopo do filtro")
  class EscopoDoFiltro {

    @Test
    @DisplayName("Rotas fora de /auth não são limitadas")
    void rotasDeNegocio_naoSaoLimitadas() throws Exception {
      AuthRateLimitFilter filtro = novoFiltro(true, false);

      for (int i = 0; i < LIMITE * 2; i++) {
        Resultado r = executar(filtro, requisicao("POST", "/ingredientes/estoque", "10.0.0.1"));
        assertThat(r.chainInvocado()).isTrue();
      }
    }

    @Test
    @DisplayName("Métodos diferentes de POST não são limitados")
    void metodoGet_naoEhLimitado() throws Exception {
      AuthRateLimitFilter filtro = novoFiltro(true, false);

      for (int i = 0; i < LIMITE * 2; i++) {
        Resultado r = executar(filtro, requisicao("GET", "/auth/google", "10.0.0.1"));
        assertThat(r.chainInvocado()).isTrue();
      }
    }

    @Test
    @DisplayName("Filtro desabilitado (RATE_LIMIT_ENABLED=false) deixa tudo passar")
    void filtroDesabilitado_naoLimita() throws Exception {
      AuthRateLimitFilter filtro = novoFiltro(false, false);

      for (int i = 0; i < LIMITE * 2; i++) {
        Resultado r = executar(filtro, requisicao("POST", "/auth/google", "10.0.0.1"));
        assertThat(r.chainInvocado()).isTrue();
      }
    }
  }

  @Nested
  @DisplayName("Resolução do IP do cliente")
  class ResolucaoDeIp {

    @Test
    @DisplayName("Por padrão o X-Forwarded-For é ignorado (previne bypass por spoofing)")
    void semTrustXff_ignoraHeader() throws Exception {
      AuthRateLimitFilter filtro = novoFiltro(true, false);

      for (int i = 0; i < LIMITE; i++) {
        MockHttpServletRequest request = requisicao("POST", "/auth/google", "10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3." + i);
        executar(filtro, request);
      }

      MockHttpServletRequest request = requisicao("POST", "/auth/google", "10.0.0.1");
      request.addHeader("X-Forwarded-For", "9.9.9.9");
      Resultado bloqueada = executar(filtro, request);

      assertThat(bloqueada.response().getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("Com trust-x-forwarded-for, usa o primeiro IP do header")
    void comTrustXff_usaPrimeiroIpDoHeader() throws Exception {
      AuthRateLimitFilter filtro = novoFiltro(true, true);

      for (int i = 0; i < LIMITE + 1; i++) {
        MockHttpServletRequest request = requisicao("POST", "/auth/google", "10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");
        executar(filtro, request);
      }

      // Mesmo remoteAddr, mas outro cliente real no XFF → não é bloqueado
      MockHttpServletRequest outroCliente = requisicao("POST", "/auth/google", "10.0.0.1");
      outroCliente.addHeader("X-Forwarded-For", "198.51.100.4, 10.0.0.1");
      Resultado r = executar(filtro, outroCliente);

      assertThat(r.chainInvocado()).isTrue();
    }
  }
}
