# ReviewLens

API de análise de sentimento de avaliações de delivery, com dashboard.

> Transforma milhares de avaliações de clientes em um painel que mostra, em segundos, o que está dando errado no delivery.

![Dashboard](reviewlens-api\docs\dashboard.png)

---

## Storytelling 

### 1. O Problema
Um restaurante de delivery recebe milhares de avaliações em texto todos os dias. Ler e classificar cada uma à mão é impossível, e o resultado é que a gestão acaba reagindo ao comentário mais recente ou mais irritado, em vez de enxergar o padrão. A pergunta que realmente importa ("o problema agora é o tempo de entrega ou a temperatura da comida?") fica sem resposta, ou chega tarde demais.

### 2. A Solução
Em vez de treinar um modelo próprio, usei um LLM (Google Gemini) como classificador e concentrei o esforço de engenharia no que fica ao redor dele. A API recebe a avaliação, a IA devolve sentimento e tópico em um JSON restrito a valores pré-definidos, e tudo é guardado de forma estruturada para virar gráfico. Como uma IA pode errar o formato ou ficar fora do ar, tratei o Gemini como uma dependência que pode falhar, e não como algo infalível: a regra de fallback fica em um único ponto do código (`ClassificadorService`).

| Escolha | Por quê |
|---|---|
| Java + Spring Boot | Arquitetura em camadas (MVC) com injeção de dependências, validação, tratamento global de erros e agendamento de tarefas já integrados |
| Google Gemini | Classifica texto em português sem eu precisar treinar um modelo, e aceita um schema que força a resposta a usar só os valores permitidos |
| PostgreSQL | Agregações (`GROUP BY`) para as estatísticas e restrição de unicidade que impede avaliações duplicadas do mesmo pedido |
| JavaScript puro + Chart.js | Painel leve, servido pelo próprio Spring: um só processo, sem etapa de build e sem CORS |

### 3. O Resultado
**O que o projeto entrega hoje:**

- Uma avaliação enviada ao `POST /api/reviews` volta, na própria resposta, com sentimento e tópico, e já entra nos gráficos do painel.
- O alerta de entrega é automático: quando mais de 50% das avaliações de entrega do mês são negativas, a barra de Entrega fica vermelha e a gestão vê o problema sem ler nenhum comentário.
- Nenhuma avaliação se perde: com o Gemini fora do ar, a API responde `202`, guarda a avaliação como `PENDENTE` e um job a classifica sozinho quando o serviço volta. Validei derrubando o Gemini de propósito.
- As 10 regras de negócio estão implementadas e rastreáveis até a classe responsável (veja a [tabela de regras](#regras-de-negócio)).
- **[N]** testes automatizados, mais um script de teste ponta a ponta, cobrem validação, sanitização, fallback, reprocessamento e o cálculo do alerta.
- Tempo de resposta do `POST` de cerca de **[X] segundos**, já incluindo a chamada ao Gemini.

## Funcionalidades

- `POST /api/reviews`: recebe uma avaliação, classifica com o Gemini e salva.
- `GET /api/reviews/stats`: estatísticas consolidadas (sentimentos, tópicos e alerta de entrega).
- Dashboard em `http://localhost:8080` com gráfico de pizza (sentimentos), barras de tópicos e alerta visual.
- Resiliência: se o Gemini falhar ou demorar, a avaliação é guardada como `PENDENTE` e reprocessada automaticamente.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Back-end | Java, Spring Boot 4, Maven |
| IA | API do Google Gemini (saída estruturada em JSON) |
| Banco | PostgreSQL, Spring Data JPA |
| Front-end | HTML, CSS, JavaScript puro e Chart.js |
| Testes | JUnit 5, Mockito, AssertJ, MockMvc |
| Ferramentas | VS Code, pgAdmin, Postman, Git e GitHub |

## Arquitetura

```
Cliente ──POST──► ReviewController ──► ReviewService ──► ClassificadorService ──► GeminiService ──► API Gemini
                        │                    │
                        ▼                    ▼
          GlobalExceptionHandler      ReviewRepository ──► PostgreSQL
                                             ▲
Dashboard ──GET──► EstatisticasController ──► EstatisticasService
                                             ▲
                  ReprocessamentoService (@Scheduled) reprocessa as avaliações PENDENTE
```

Padrão MVC em camadas (`controller`, `service`, `repository`, `model`, `dto`, `exception`, `config`), com injeção de dependências do Spring.

## Como executar

### Pré-requisitos
- JDK 17 ou superior
- PostgreSQL
- Uma chave da API do Gemini (Google AI Studio)

### 1. Banco de dados
Crie o banco (as tabelas são criadas automaticamente na primeira execução):

```sql
CREATE DATABASE reviewlens_delivery;
```

### 2. Variáveis de ambiente
As credenciais **nunca** ficam no código. Defina:

```powershell
# Windows (PowerShell), para a sessão atual
$env:DB_PASSWORD='senha_do_postgres'
$env:GEMINI_API_KEY='sua_chave_do_gemini'
```

```bash
# Linux/macOS
export DB_PASSWORD='senha_do_postgres'
export GEMINI_API_KEY='sua_chave_do_gemini'
```

### 3. Subir a aplicação
```bash
cd reviewlens-api
./mvnw spring-boot:run        # no Windows: .\mvnw.cmd spring-boot:run
```

Abra `http://localhost:8080`.

### Configurações (`application.properties`)

| Propriedade | Padrão | Descrição |
|---|---|---|
| `gemini.api.model` | `gemini-3.5-flash` | Modelo do Gemini. Os nomes mudam com frequência, confira a documentação |
| `gemini.api.timeout-segundos` | `30` | Tempo máximo de espera pelo Gemini |
| `reviewlens.reprocessamento.intervalo-ms` | `60000` | Intervalo do job de reprocessamento |
| `reviewlens.reprocessamento.tamanho-lote` | `20` | Avaliações reprocessadas por ciclo |
| `reviewlens.reprocessamento.max-tentativas` | `5` | Tentativas antes de marcar como `ERRO` |

## API

### `POST /api/reviews`

```json
{ "idPedido": 123, "texto": "A comida chegou fria e atrasada" }
```

Resposta (`201 Created`):
```json
{
  "id": 1,
  "idPedido": 123,
  "texto": "A comida chegou fria e atrasada",
  "sentimento": "NEGATIVO",
  "topico": "TEMPERATURA",
  "status": "PROCESSADO",
  "dataCriacao": "2026-11-14T10:30:00"
}
```

| Código | Quando |
|---|---|
| `201` | Avaliação classificada e salva |
| `202` | Recebida, mas o Gemini estava indisponível: ficou `PENDENTE` |
| `400` | Texto vazio, só com espaços, fora de 10 a 1000 caracteres, `idPedido` ausente ou JSON inválido |
| `409` | Já existe avaliação para esse `idPedido` |
| `503` | Banco de dados indisponível |
| `500` | Erro inesperado (mensagem genérica, sem detalhes internos) |

Os erros seguem o formato:
```json
{ "timestamp": "...", "status": 400, "erro": "Bad Request", "mensagens": ["O texto da avaliação deve ter entre 10 e 1000 caracteres."] }
```

### `GET /api/reviews/stats`

```json
{
  "totalProcessadas": 9,
  "sentimentos": { "POSITIVO": 4, "NEGATIVO": 4, "NEUTRO": 1 },
  "topicos": { "SABOR": 2, "ENTREGA": 5, "TEMPERATURA": 0, "EMBALAGEM": 1, "PRECO": 1, "OUTROS": 0 },
  "entregaMes": { "mes": "2026-11", "total": 4, "negativas": 3, "percentualNegativo": 75.0, "alertaCritico": true }
}
```

## Regras de negócio

| Regra | Descrição | Onde está |
|---|---|---|
| RN-01 | Texto vazio, nulo ou só espaços retorna 400 | `ReviewRequest` (`@NotBlank`) |
| RN-02 | Texto entre 10 e 1000 caracteres, validado no controller | `ReviewRequest` (`@Size`), `@Valid` |
| RN-03 | Sanitização contra injeção de prompt (HTML, símbolos, quebras de linha) | `TextoSanitizer` |
| RN-04 | Só sentimentos e tópicos do domínio (enums) | `Sentimento`, `Topico`, schema gerado dos enums |
| RN-05 | Resposta inválida da IA: salva `NEUTRO`/`OUTROS` e registra erro no log | `GeminiService`, `ClassificadorService` |
| RN-06 | Gemini fora do ar ou acima de 30 s: salva `PENDENTE`, retorna 202 e um job reprocessa | `GeminiConfig`, `ClassificadorService`, `ReprocessamentoService` |
| RN-07 | Sem duplicidade por pedido | `unique` em `id_pedido` e `existsByIdPedido` |
| RN-08 | Auditoria com `data_criacao` automática | `@CreationTimestamp` |
| RN-09 | Estatísticas contam só avaliações `PROCESSADO` | `EstatisticasService` |
| RN-10 | Mais de 50% de entregas negativas no mês pinta a barra de Entrega de vermelho | `EstatisticasService`, `dashboard.js` |

### Decisões de projeto
- **Fallback da RN-05 salvo como `ERRO`:** as avaliações com fallback guardam `NEUTRO`/`OUTROS`, mas ficam fora das estatísticas (RN-09), para um palpite com defeito não distorcer os gráficos.
- **Limite de tentativas no job:** depois de 5 falhas, a avaliação `PENDENTE` vira `ERRO`, para não reenviar para sempre e gastar a cota do Gemini.
- **Alerta da RN-10:** calculado no back-end, sobre as avaliações de **Entrega do mês atual**. "Mais de 50%" é estrito: exatamente 50% não dispara. O front-end só obedece ao campo `alertaCritico`.

## Testes

```bash
./mvnw test
```

Cobrem a sanitização, a interpretação da resposta do Gemini, o fallback, o reprocessamento, o fluxo do `POST`, as validações HTTP, os códigos de erro e o cálculo do alerta. O teste `ReviewlensApiApplicationTests` sobe a aplicação inteira e exige o banco e as variáveis de ambiente.

Há também um teste ponta a ponta contra a aplicação rodando:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\teste-e2e.ps1
```

## Segurança

- Chaves e senhas somente em variáveis de ambiente.
- Erros padronizados, sem stack trace para o cliente.
- Consultas parametrizadas (sem concatenar SQL) e `textContent` no front-end.

**Limitações conhecidas:** a API não tem autenticação nem limite de requisições (o `POST` consome a cota do Gemini); `ddl-auto=update` e `show-sql` são para desenvolvimento.

## Próximos passos
- Autenticação e limite de requisições por IP (Spring Security).
- Migrações de banco com Flyway.
- Filtro de comentários por categoria e sentimento no dashboard.
- Execução em contêiner (Docker).

## Autoria
Desenvolvido por Mayane Lins. [link do repositório]
