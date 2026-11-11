# 2.6 Decisões de arquitetura (ADRs) — INT-1

## ADR-1 Onde o score vive — **decisão principal (pessoa decide no G2)**
| Opção | Prós | Contras |
|---|---|---|
| A. Módulo dentro de `account-api` | Menos peças; dados locais | Mistura domínio de risco com conta; cresce o serviço mais crítico; não segue "database per microservice" |
| **B. Novo `risk-score-api` + evento Kafka** (recomendado) | Segue o padrão do repo (serviço + banco próprio + Eureka + gateway); isola risco/auditoria; escala e evolui (ML depois) sem tocar conta; permite unidades em paralelo | Um serviço a mais para operar; consistência eventual (≤ 5 s, NFR-2) |
| C. Calcular na hora da consulta (sem persistir) | Sem evento/armazenamento | Não atende histórico (REQ-8), latência pior (NFR-1), cálculo não auditável |

**Decisão proposta:** B. Consequência: account-api passa a produzir Kafka; novo módulo no `pom.xml` raiz e no `docker-compose.yml`.

## ADR-2 Evento magro + busca do perfil (vs. evento com estado completo)
Evento `AccountActivityEvent` leva só `eventId, accountId, type, occurredAt`; o `risk-score-api` busca `GET /api/account/{id}/risk-profile` (lb://account-api, como `transaction-api` → `asset-management-api`). **Por quê:** fonte única de verdade, recálculo sob demanda (REQ-13) usa o mesmo caminho, sem projeção que diverge. Custo: uma chamada síncrona por evento (aceitável no volume atual).

## ADR-3 Publicação do evento sem outbox no MVP
`account-api` publica após salvar (como `transaction-api` faz hoje). Perda de evento é reparada por REQ-13 (recalcular) e idempotência por `(accountId, sourceEventId)`. Outbox transacional fica para "depois" (RISK-4).

## ADR-4 Autorização por escopo no gateway
Gateway mapeia claims `scope` (espaço-separado) e `permissions` (array, RBAC Auth0) para autoridades `SCOPE_*`. Regras: `GET /api/risk-score/**` → `credit:read`; `POST /api/risk-score/**/recalculate` e `PUT /api/account/*/kyc` → `credit:write`; `GET /api/account/*/risk-profile` → `credit:read`; demais rotas inalteradas (autenticado). Mudança aditiva (NFR-8). Serviços internos chamam entre si via `lb://` sem passar pelo gateway (padrão atual).

## ADR-5 Parâmetros no Config Server com versão explícita
`config-files/risk-score-api.properties` com `risk.params.version=v1` e faixas/pesos/taxas aprovados, ligados a `@ConfigurationProperties`. Mudança de parâmetro = commit no config + nova versão + restart/refresh. Nunca constantes no código (regra do playbook).

## ADR-6 Ledger de movimentações no account-api
Coleção `account_movement` gravada no mesmo fluxo do depósito. Contas existentes: `openedAt` e KYC ausentes (PARAM-6 → 0 ponto "dado ausente"); sem backfill inventado.
