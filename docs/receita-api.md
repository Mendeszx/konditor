# Documentação da API de Receitas

Esta documentação detalha os endpoints do controller de Receitas, exemplos de resposta e explicação de cada campo, para uso pelo front-end.

---

## Autenticação

Todos os endpoints requerem JWT válido. O `workspaceId` do tenant é extraído automaticamente do token.

---

## Endpoints

### 1. `POST /receitas`

Cria uma nova receita (por padrão como rascunho). O custo total e o preço sugerido são calculados automaticamente.

#### Exemplo de resposta

Veja estrutura em [ReceitaResponse](#estrutura-de-receitaresponse).

---

### 2. `GET /receitas/{id}`

Retorna os detalhes completos de uma receita pelo ID, incluindo ingredientes e custos.

#### Exemplo de resposta

Veja estrutura em [ReceitaResponse](#estrutura-de-receitaresponse).

---

### 3. `PUT /receitas/{id}`

Atualiza todos os campos de uma receita (salvar rascunho ou editar publicada). Ingredientes são substituídos e custos recalculados.

#### Exemplo de resposta

Veja estrutura em [ReceitaResponse](#estrutura-de-receitaresponse).

---

### 4. `POST /receitas/{id}/publicar`

Publica uma receita, tornando-a visível no dashboard e listagens. Muda o status de `rascunho` para `publicada`.

#### Exemplo de resposta

Veja estrutura em [ReceitaResponse](#estrutura-de-receitaresponse).

---

### 5. `POST /receitas/calcular`

Calcula custos e preço sugerido em tempo real para um conjunto de ingredientes, sem persistir dados. Ideal para atualização dinâmica.

#### Exemplo de resposta

Veja estrutura em [CustosCalculadosResponse](#estrutura-de-custoscalculadosresponse).

---

### 6. `GET /receitas/categorias`

Retorna todas as categorias de receita globais, ordenadas por nome. Usadas para preencher chips de filtro e o seletor de categoria.

#### Exemplo de resposta

```json
[
  {
    "id": "1",
    "nome": "Tortas",
    "cor": "#F59E0B"
  }
]
```

#### Campos do `CategoriaReceitaResponse`
- **id** (`string`): ID da categoria.
- **nome** (`string`): Nome da categoria.
- **cor** (`string|null`): Cor em hexadecimal para exibição na UI (ex: #F59E0B). `null` se não definida.

---

### 7. `GET /ingredientes?query=...`

Busca ingredientes do workspace por nome (autocomplete). Retorna todos os ingredientes ativos quando `query` é omitido.

#### Exemplo de resposta

```json
[
  {
    "id": "abc123",
    "nome": "Farinha de Trigo",
    "marca": "Dona Benta",
    "unidadeId": "g",
    "unidadeSimbolo": "g",
    "unidadeNome": "grama",
    "custoPorUnidade": 0.12
  }
]
```

#### Campos do `BuscaIngredienteResponse`
- **id** (`string`): ID do ingrediente (usar no payload de criação/edição de receitas).
- **nome** (`string`): Nome do ingrediente.
- **marca** (`string`): Marca cadastrada.
- **unidadeId** (`string`): ID da unidade de medida.
- **unidadeSimbolo** (`string`): Símbolo da unidade (ex: "g").
- **unidadeNome** (`string`): Nome da unidade (ex: "grama").
- **custoPorUnidade** (`number`): Custo por unidade base cadastrada.

---

## Estrutura de ReceitaResponse

```json
{
  "id": "...",
  "nome": "Bolo de Cenoura",
  "descricao": "Receita clássica...",
  "categoriaId": "1",
  "categoriaNome": "Tortas",
  "rendimentoQuantidade": 12,
  "rendimentoUnidadeId": "un",
  "rendimentoUnidadeSimbolo": "un",
  "rendimentoUnidadeNome": "unidade",
  "tempoPreparoMinutos": 90,
  "ingredientes": [ /* lista de IngredienteReceitaResponse */ ],
  "notas": "Dica: peneire a farinha.",
  "precoFinal": 60.00,
  "precoSugerido": 65.00,
  "custoIngredientes": 20.00,
  "custoMaoDeObra": 10.00,
  "custosFixos": 5.00,
  "custoCalculado": 35.00,
  "margem": 41.7,
  "maoDeObraValorHora": 20.00,
  "custosFixosValor": 5.00,
  "custosFixosTipo": "fixo",
  "margemDesejada": 30.00,
  "status": "publicada",
  "ativo": true,
  "criadoEm": "2024-04-18T12:00:00Z",
  "atualizadoEm": "2024-04-18T12:00:00Z",
  "pesoPorUnidade": 100,
  "pesoPorUnidadeUnidadeId": "g",
  "pesoPorUnidadeUnidadeSimbolo": "g",
  "numeroPorcoesUnidades": 12,
  "custoPorGramaOuMl": 0.35,
  "precoPorGramaOuMl": 0.40,
  "custoPorPorcaoOuUnidade": 2.92,
  "precoPorPorcaoOuUnidade": 3.33
}
```

Veja todos os campos possíveis na classe `ReceitaResponse`.

---

## Estrutura de CustosCalculadosResponse

```json
{
  "custoIngredientes": 20.00,
  "custoMaoDeObra": 10.00,
  "custosFixos": 5.00,
  "custoTotal": 35.00,
  "precoSugerido": 65.00,
  "margem": 41.7,
  "rendimentoQuantidade": 12,
  "custoTotalPorUnidade": 2.92,
  "precoSugeridoPorUnidade": 3.33,
  "maoDeObraValorHora": 20.00,
  "tempoPreparoMinutos": 90,
  "custosFixosValor": 5.00,
  "custosFixosTipo": "fixo",
  "margemUtilizada": 30.00,
  "numeroPorcoesUnidades": 12,
  "custoPorGramaOuMl": 0.35,
  "precoPorGramaOuMl": 0.40,
  "custoPorPorcaoOuUnidade": 2.92,
  "precoPorPorcaoOuUnidade": 3.33
}
```

Veja todos os campos possíveis na classe `CustosCalculadosResponse`.

---

## Observações

- Todos os valores monetários são retornados como `number` (ponto flutuante).
- Os campos `null` indicam ausência de informação ou configuração.
- O front-end deve usar os campos de status (`status`, etc.) para exibir cores/alertas adequados.
- Para receitas sem custo registrado, a margem pode ser `null`.
- Para receitas sem preço de venda, a margem é `null`.

Se precisar de exemplos de payloads, regras de cálculo ou mais detalhes sobre algum campo, solicite!

