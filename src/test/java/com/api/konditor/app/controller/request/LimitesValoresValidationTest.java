package com.api.konditor.app.controller.request;

import static org.assertj.core.api.Assertions.*;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Garante que os limites máximos (anti valores exorbitantes) são aplicados via Bean Validation nos
 * DTOs dos formulários de ingrediente e receita.
 */
class LimitesValoresValidationTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  /** Verifica se existe alguma violação no campo informado. */
  private <T> boolean violaCampo(T obj, String campo) {
    return validator.validate(obj).stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals(campo));
  }

  @Test
  @DisplayName("Ingrediente: preço acima de 1.000.000 é rejeitado; no limite é aceito")
  void ingrediente_precoAcimaDoLimite_rejeitado() {
    CriarIngredienteRequest req = new CriarIngredienteRequest();
    req.setNome("Chocolate");
    req.setUnidadeId(UUID.randomUUID());

    req.setPrecoPorUnidade(new BigDecimal("1000000.01"));
    assertThat(violaCampo(req, "precoPorUnidade")).isTrue();

    req.setPrecoPorUnidade(new BigDecimal("1000000.00"));
    assertThat(violaCampo(req, "precoPorUnidade")).isFalse();
  }

  @Test
  @DisplayName("Receita: rendimento e preço final acima do limite são rejeitados")
  void receita_camposAcimaDoLimite_rejeitados() {
    CriarReceitaRequest req = new CriarReceitaRequest();
    req.setNome("Bolo");
    req.setRendimentoUnidadeId(UUID.randomUUID());

    req.setRendimentoQuantidade(new BigDecimal("1000001"));
    req.setPrecoFinal(new BigDecimal("2000000"));
    assertThat(violaCampo(req, "rendimentoQuantidade")).isTrue();
    assertThat(violaCampo(req, "precoFinal")).isTrue();

    req.setRendimentoQuantidade(new BigDecimal("1000000"));
    req.setPrecoFinal(new BigDecimal("1000000.00"));
    assertThat(violaCampo(req, "rendimentoQuantidade")).isFalse();
    assertThat(violaCampo(req, "precoFinal")).isFalse();
  }

  @Test
  @DisplayName("Ingrediente da receita: quantidade acima do limite é rejeitada")
  void ingredienteReceita_quantidadeAcimaDoLimite_rejeitada() {
    IngredienteReceitaRequest req = new IngredienteReceitaRequest();
    req.setIngredienteId(UUID.randomUUID());
    req.setUnidadeId(UUID.randomUUID());

    req.setQuantidade(new BigDecimal("1000001"));
    assertThat(violaCampo(req, "quantidade")).isTrue();

    req.setQuantidade(new BigDecimal("1000000"));
    assertThat(violaCampo(req, "quantidade")).isFalse();
  }

  @Test
  @DisplayName("Cálculo de custos: tempo de preparo acima do limite é rejeitado")
  void calculoCustos_tempoAcimaDoLimite_rejeitado() {
    CalcularCustosRequest req = new CalcularCustosRequest();
    req.setRendimentoQuantidade(BigDecimal.ONE);
    req.setMaoDeObraValorHora(new BigDecimal("25"));
    req.setCustosFixosValor(new BigDecimal("15"));
    req.setCustosFixosTipo("percentual");

    req.setTempoPreparoMinutos(new BigDecimal("100001"));
    assertThat(violaCampo(req, "tempoPreparoMinutos")).isTrue();

    req.setTempoPreparoMinutos(new BigDecimal("100000"));
    assertThat(violaCampo(req, "tempoPreparoMinutos")).isFalse();
  }
}
