# 2.9 Delivery Plan (bolts) — INT-1

| Bolt | Unidades | Paralelo | Pré-condição | Saída |
|---|---|---|---|---|
| **B-1** | U-1, U-2, U-3, U-4 | Sim (4 sessões filhas) | G2 + PR de spec mergeado | 4 PRs verdes, cada um testado contra os contratos |
| **B-2** | 3.6 Build & Test + 3.7 CI | — | PRs de B-1 abertos e verdes | Stack integrada, E2E gravado, CI para backend, G3 |

**Por que uma onda só:** os contratos (C-1..C-6) eliminam dependência de código; as dependências de runtime são cobertas por stubs e validadas na integração (B-2). Risco: divergência de contrato → mitigado pela revisão de cada PR contra `contract-summary.md` (passo 5 do playbook) antes de B-2.

## Definition of Done (toda unidade)
- PR com título `RRB-<task> U-n: ...`, base `main`, só arquivos da posse da unidade.
- Artefatos `construction/U-n/` (functional-design, nfr) no PR da unidade.
- Testes: backend `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -pl <module> -am clean verify` verde (com evidência no PR); web `npm run lint && npm run typecheck && npm test` + Playwright do fluxo novo.
- Cada REQ da unidade com ≥ 1 teste nomeado no PR; parâmetros de negócio só via config (C-6).
- CI verde; Devin Review sem pendência bloqueante.

## DoD específico
- **U-1:** Testcontainers compatível com Docker atual (fix do `AccountRepositoryTest`); `AccountResponse` aditivo; evento publicado em teste (EmbeddedKafka ou mock do `KafkaTemplate`).
- **U-2:** `RiskScoreCalculator` com testes de tabela cobrindo todas as faixas, dado ausente, taxa ausente e o exemplo de US-1 (= 655 MEDIUM); idempotência por `eventId`; métricas `risk_score_calculations_total`, `risk_score_calculation_seconds`, `risk_score_band_total{band}`.
- **U-3:** testes 401 / 403 / 200 por rota e escopo; rotas existentes inalteradas.
- **U-4:** página `/credit-risk` com estados carregando/403/404/sucesso; Playwright gravado do fluxo (abrir conta → ver score e fatores → registrar KYC → recalcular → histórico).

## Riscos de sequência e dependências externas
- RISK-4 (evento perdido) → REQ-13 recalcular.
- DEP-1 motor de crédito: só consome C-5; nenhum trabalho neste repo.
- Conflito no `pom.xml` raiz (U-1 versão, U-2 módulo) e `docker-compose.yml` (só U-2): resolver no rebase em B-2.
- Porta 3000 duplicada web/Grafana no compose (achado 2.1): fora do escopo, contornar na Operation.
