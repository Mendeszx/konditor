package com.api.konditor.app.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.api.konditor.app.config.security.AuthRateLimitFilter;
import com.api.konditor.app.config.security.JwtAuthenticationFilter;
import com.api.konditor.app.config.security.RestAuthenticationEntryPoint;
import com.api.konditor.app.config.security.SecurityConfig;
import com.api.konditor.app.controller.response.IngredienteResumoResponse;
import com.api.konditor.app.service.IngredienteService;
import com.api.konditor.app.service.JwtService;
import com.api.konditor.domain.enuns.Plan;
import com.api.konditor.domain.enuns.Role;
import com.api.konditor.infra.jpa.entity.UsuarioJpaEntity;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Teste de integração da camada web com o Spring Security real: endpoints protegidos exigem JWT
 * válido (401 sem token / token inválido; 200 com token emitido pelo {@link JwtService}).
 */
@WebMvcTest(IngredienteController.class)
@Import({
  SecurityConfig.class,
  JwtAuthenticationFilter.class,
  RestAuthenticationEntryPoint.class,
  AuthRateLimitFilter.class,
  JwtService.class
})
@TestPropertySource(
    properties = {
      "security.jwt.secret=segredo-de-teste-com-mais-de-32-caracteres!",
      "security.jwt.expiration-seconds=3600"
    })
class IngredienteControllerSecurityTest {

  /** O slice @WebMvcTest não auto-configura o ObjectMapper (Jackson 2) usado pelos filtros. */
  @TestConfiguration
  static class ObjectMapperTestConfig {
    @Bean
    com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
      return new com.fasterxml.jackson.databind.ObjectMapper();
    }
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;

  @MockitoBean private IngredienteService ingredienteService;

  private String tokenValido() {
    UsuarioJpaEntity usuario =
        UsuarioJpaEntity.builder()
            .id(UUID.randomUUID())
            .email("chef@konditor.io")
            .nome("Chef Teste")
            .build();
    return jwtService.gerarToken(
        new JwtService.ContextoToken(usuario, UUID.randomUUID().toString(), Role.owner, Plan.free));
  }

  @Test
  @DisplayName("Endpoint protegido sem token retorna 401 com corpo JSON padronizado")
  void semToken_retorna401() throws Exception {
    mockMvc
        .perform(get("/ingredientes/estoque/resumo"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));

    verifyNoInteractions(ingredienteService);
  }

  @Test
  @DisplayName("Token inválido retorna 401")
  void tokenInvalido_retorna401() throws Exception {
    mockMvc
        .perform(
            get("/ingredientes/estoque/resumo").header("Authorization", "Bearer token-invalido"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(ingredienteService);
  }

  @Test
  @DisplayName("Token válido retorna 200 e o controller é executado")
  void tokenValido_retorna200() throws Exception {
    when(ingredienteService.resumo(any())).thenReturn(new IngredienteResumoResponse(7));

    mockMvc
        .perform(
            get("/ingredientes/estoque/resumo").header("Authorization", "Bearer " + tokenValido()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalIngredientes").value(7));
  }
}
