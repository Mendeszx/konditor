# Documentação da API de Dashboard

Esta documentação detalha os endpoints do controller de Dashboard, incluindo exemplos de resposta e explicação de cada campo, para uso pelo front-end.

---

## Autenticação

Todos os endpoints requerem JWT válido. O `workspaceId` do tenant é extraído automaticamente do token, não sendo necessário informar parâmetros adicionais.

---

## Endpoints

### 1. `GET /dashboard/estatisticas`

Retorna estatísticas gerais do workspace autenticado, incluindo totais, médias e a receita com maior margem.

#### Exemplo de resposta

```json
{
  "totalReceitas": 42,
  "totalRascunhos": 5,
  "receitasComMargemBaixa": 3,
  "receitasAbaixoMargemDesejada": 8,
  "margemMedia": 68,
  "melhorMargem": {
    "id": "3fa85f64-...",
    "nome": "Macaron Baunilha",
    "margem": 81
  }
}
```

#### Campos do `DashboardStatsResponse`

- **totalReceitas** (`int`): Número total de receitas publicadas ativas no workspace.
- **totalRascunhos** (`int`): Número de receitas em status `rascunho` no workspace.
- **receitasComMargemBaixa** (`int`): Quantidade de receitas publicadas cuja margem real está abaixo de 30% (limiar crítico). Exibir como alerta vermelho.
- **receitasAbaixoMargemDesejada** (`int`): Quantidade de receitas publicadas onde a margem real ficou abaixo da `margemDesejada` configurada pelo usuário. Inclui as `receitasComMargemBaixa`. Exibir como alerta amarelo.
- **margemMedia** (`int`): Média das margens de lucro de todas as receitas publicadas, arredondada para inteiro.
- **melhorMargem** (`MelhorMargemResponse` ou `null`): Dados da receita com maior margem de lucro. `null` se não houver receitas.

##### Estrutura de `MelhorMargemResponse`

- **id** (`string`): ID único da receita (UUID).
- **nome** (`string`): Nome da receita.
- **margem** (`int`): Margem de lucro em percentual, arredondada para inteiro.

---

### 2. `GET /dashboard/receitas?status={publicada|rascunho}`

Retorna a lista de receitas do workspace formatadas para o grid do dashboard. Por padrão, retorna apenas receitas `publicada`. Use `?status=rascunho` para listar rascunhos.

#### Exemplo de resposta

```json
[
  {
    "id": "3fa85f64-...",
    "nome": "Ganache de Framboesa",
    "categoria": "Tortas",
    "quantidade": 24,
    "unidade": "mini tortas",
    "custoTotal": 63.26,
    "custoIngredientesPorUnidade": 0.93,
    "custoMaoDeObraPorUnidade": 1.56,
    "custosFixosPorUnidade": 0.14,
    "custoUnitario": 2.64,
    "precoUnitario": 6.50,
    "precoSugerido": 4.39,
    "margem": 59,
    "margemDesejada": 40,
    "margemStatus": "normal",
    "tempoPreparo": 90,
    "status": "publicada",
    "pesoPorUnidade": 15,
    "pesoPorUnidadeSimbolo": "g",
    "numeroPorcoesUnidades": 45.00,
    "custoPorGramaOuMl": 0.0685,
    "precoPorGramaOuMl": 0.0979,
    "custoPorPorcaoOuUnidade": 0.68,
    "precoPorPorcaoOuUnidade": 0.98
  }
]
```

#### Campos do `ReceitaCardResponse`

- **id** (`string`): ID único da receita.
- **nome** (`string`): Nome da receita.
- **categoria** (`string|null`): Nome da categoria (ex: "Tortas"). `null` se não categorizada.
- **quantidade** (`number`): Quantidade produzida pela receita (rendimento).
- **unidade** (`string|null`): Unidade do rendimento (ex: "mini tortas"). `null` se não cadastrada.
- **custoTotal** (`number`): Custo total do lote completo (ingredientes + mão de obra + custos fixos).
- **custoIngredientesPorUnidade** (`number`): Custo de ingredientes por unidade.
- **custoMaoDeObraPorUnidade** (`number`): Custo de mão de obra por unidade.
- **custosFixosPorUnidade** (`number`): Custos fixos por unidade.
- **custoUnitario** (`number`): Custo total por unidade (soma dos três acima).
- **precoUnitario** (`number`): Preço de venda por unidade definido pelo usuário.
- **precoSugerido** (`number|null`): Preço sugerido por unidade. `null` se não calculado.
- **margem** (`int`): Margem de lucro real em percentual, arredondada para inteiro.
- **margemDesejada** (`int`): Margem de lucro desejada pelo usuário (%).
- **margemStatus** (`string`): Status da margem de lucro:
  - `"baixa"`: margem < 30% (alerta vermelho)
  - `"abaixo_desejada"`: margem >= 30% mas < margemDesejada (alerta amarelo)
  - `"normal"`: margem >= margemDesejada (verde)
- **tempoPreparo** (`int|null`): Tempo estimado de preparo em minutos. `null` se não informado.
- **status** (`string`): Status da receita: `"publicada"` ou `"rascunho"`.
- **pesoPorUnidade** (`number|null`): Peso ou volume de cada unidade/porção. `null` se não configurado.
- **pesoPorUnidadeSimbolo** (`string|null`): Símbolo da unidade do peso/volume (ex: "g", "ml"). `null` se não configurado.
- **numeroPorcoesUnidades** (`number|null`): Número de porções/unidades do lote. `null` se não calculado.
- **custoPorGramaOuMl** (`number|null`): Custo total por grama ou mililitro. Disponível se unidade de rendimento for peso/volume.
- **precoPorGramaOuMl** (`number|null`): Preço sugerido por grama ou mililitro. Disponível se unidade de rendimento for peso/volume.
- **custoPorPorcaoOuUnidade** (`number|null`): Custo por porção/unidade individual. Disponível se `numeroPorcoesUnidades` calculado.
- **precoPorPorcaoOuUnidade** (`number|null`): Preço sugerido por porção/unidade individual. Disponível se `numeroPorcoesUnidades` calculado.

---

## Observações

- Todos os valores monetários são retornados como `number` (ponto flutuante).
- Os campos `null` indicam ausência de informação ou configuração.
- O front-end deve usar os campos de status (`margemStatus`, `status`) para exibir cores/alertas adequados.
- Para receitas sem custo registrado, a margem é considerada 100%.
- Para receitas sem preço de venda, a margem é 0%.

Se precisar de exemplos de payloads, regras de cálculo ou mais detalhes sobre algum campo, solicite!

