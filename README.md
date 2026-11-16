# ReviewLens

API de análise de sentimento de avaliações de delivery, com dashboard.

Clientes de restaurantes geram milhares de avaliações em texto, e lê-las uma a uma é inviável. O ReviewLens recebe cada avaliação, usa o **Google Gemini** para extrair o **sentimento** e o **tópico** abordado, guarda tudo de forma estruturada e mostra o resultado em um painel que indica, por exemplo, se o problema atual é o tempo de entrega ou a temperatura da comida.

![Dashboard](\API-ReviewLens\reviewlens-api\docs\dashboard.png)

---

## Storytelling 

### 1. O Problema
Restaurantes e operações de delivery lidam diariamente com um volume massivo de avaliações textuais de clientes. O principal desafio de **negócio** era a impossibilidade de ler e categorizar manualmente esse volume a tempo de corrigir falhas operacionais críticas (como comida fria ou atrasos). Do ponto de vista **técnico**, o desafio era construir uma integração robusta com uma IA externa (Google Gemini) capaz de processar dados sem travar a aplicação, garantindo que instabilidades ou falhas de rede da API não gerassem perda de dados ou erros 500 para o usuário final.

### 2. A Solução
Para resolver isso, escolhi **Java com Spring Boot** pela sua maturidade corporativa, tipagem estática e facilidade na construção de arquiteturas em camadas bem definidas. O **PostgreSQL** foi adotado para garantir consistência relacional e controle de duplicidade por pedido. A arquitetura foi desenhada com foco em resiliência: caso a IA falhe ou demore a responder, o sistema utiliza um mecanismo de *fallback* e um job assíncrono agendado (`@Scheduled`) para reprocessar os dados de forma transparente, mantendo a estabilidade do sistema.

### 3. O Resultado
O projeto entregou uma API REST totalmente funcional que automatiza 100% da triagem de *feedback*, convertendo texto livre em métricas estruturadas de sentimento e tópicos de negócio. Como resultado prático, o sistema alcançou alta resiliência (com tratamento de exceções customizado e reprocessamento automático de falhas), um dashboard interativo em JavaScript puro que sinaliza criticidade operacional (mudando a cor do gráfico de entregas para vermelho quando os problemas superam 50%), além de uma suíte completa de testes automatizados (unitários e de integração).

---

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
| `gemini.api.timeout-segundos` | `10` | Tempo máximo de espera pelo Gemini |
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
