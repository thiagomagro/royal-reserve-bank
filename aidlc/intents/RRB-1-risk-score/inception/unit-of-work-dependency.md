# 2.7 DAG de unidades — INT-1

```mermaid
flowchart LR
  C[contract-summary.md<br/>C-1..C-6] --> U1[U-1 account-api]
  C --> U2[U-2 risk-score-api]
  C --> U3[U-3 api-gateway]
  C --> U4[U-4 web]
  U1 --> IT[3.6 Build & Test integrado]
  U2 --> IT
  U3 --> IT
  U4 --> IT
```

Dependências de **runtime** (U-2 consome evento/endpoint de U-1; U-4 chama U-2/U-1 via U-3) são resolvidas pelos contratos: cada unidade testa contra stubs (MockWebServer/EmbeddedKafka no backend, MSW no web). Não há dependência de **código** entre unidades → as 4 rodam **em paralelo** (onda única). Conflitos evitados por posse de arquivos (ver `unit-of-work.md`); `pom.xml` raiz é tocado por U-1 (versão) e U-2 (módulo) em linhas distintas.
