# 2.8 Contratos — INT-1

Convenções: JSON, datas ISO-8601 UTC (`Instant`) ou `YYYY-MM-DD` (`LocalDate`), valores monetários como número decimal. Erros: `{ "code": "<CODE>", "message": "<texto>" }`. Todas as rotas externas passam pelo gateway (`:8080`); chamadas serviço→serviço usam `lb://<service>` direto.

## C-1 `PUT /api/account/{id}/kyc` (account-api, U-1) — authz `credit:write`
Request `{ "status": "VALID" | "PENDING" | "EXPIRED", "validUntil": "2027-11-11" }` (`validUntil` obrigatório para `VALID`).
200 → `AccountResponse` (abaixo). 400 `KYC_INVALID`. 404 `ACCOUNT_NOT_FOUND`. Publica C-4 `KYC_UPDATED`.

`AccountResponse` (aditivo, NFR-8): campos atuais + `"openedAt": "2026-11-11T13:00:00Z" | null`, `"kyc": { "status": "VALID", "validUntil": "2027-11-11", "updatedAt": "..." } | null`.

## C-2 `GET /api/account/{id}/movements?days=90` (account-api, U-1) — authz `credit:read`
200 → `[ { "id": "...", "accountId": "...", "type": "DEPOSIT", "amount": 100.00, "currency": "USD", "occurredAt": "..." } ]` (mais recente primeiro). 404 `ACCOUNT_NOT_FOUND`.
`POST /api/account/{id}/deposit` (existente) passa a gravar a movimentação e publicar C-4 `DEPOSIT`; `POST /api/account` grava `openedAt=now` e publica `ACCOUNT_OPENED` (resposta inalterada).

## C-3 `GET /api/account/{id}/risk-profile?windowDays=90` (account-api, U-1; consumido por U-2) — externo: authz `credit:read` no gateway; interno: U-2 chama `lb://account-api` direto (rede interna, sem gateway)
200 →
```json
{ "accountId": "a1", "accountHolderName": "Ana", "balance": 3000.00, "currency": "USD",
  "openedAt": "2025-09-11T00:00:00Z", "kyc": { "status": "VALID", "validUntil": "2026-12-01" },
  "movements": { "windowDays": 90, "count": 5, "totalAmount": 2000.00 },
  "asOf": "2026-11-11T13:00:00Z" }
```
`openedAt`/`kyc` podem ser `null`. 404 `ACCOUNT_NOT_FOUND`.

## C-4 Evento `AccountActivityEvent` — Kafka `accountActivityTopic` (produtor U-1, consumidor U-2)
Key = `accountId`. Value JSON `{ "eventId": "uuid", "accountId": "a1", "type": "ACCOUNT_OPENED" | "DEPOSIT" | "KYC_UPDATED", "occurredAt": "..." }`.
Type mapping (JsonSerializer/JsonDeserializer): token `accountActivity` → produtor `spring.kafka.producer.properties.spring.json.type.mapping=accountActivity:com.royal.reserve.bank.account.api.event.AccountActivityEvent`; consumidor `spring.kafka.consumer.properties.spring.json.type.mapping=accountActivity:com.royal.reserve.bank.risk.score.api.event.AccountActivityEvent`, `group-id=risk-score-api`.
Consumidor: busca C-3 e grava cálculo com `trigger = type`, `sourceEventId = eventId`; mesmo `(accountId, eventId)` não gera segundo cálculo.

## C-5 risk-score-api (U-2) — via gateway rota `/api/risk-score/**` (U-3)
`RiskScoreResponse`:
```json
{ "accountId": "a1", "score": 655, "band": "MEDIUM", "bandLabel": "Médio risco", "mode": "SHADOW",
  "paramsVersion": "v1", "trigger": "DEPOSIT", "calculatedAt": "2026-11-11T13:00:01Z",
  "factors": [
    { "code": "BALANCE",   "label": "Saldo",                  "rawValue": "15000.00 BRL", "points": 200, "maxPoints": 300, "reasonCode": "OK", "reason": "Saldo 3000.00 USD = 15000.00 BRL (taxa USD 5.00)", "details": { "currency": "USD", "fxRate": 5.00, "valueBrl": 15000.00 } },
    { "code": "VOLUME",    "label": "Volume movimentado",     "rawValue": "10000.00 BRL", "points": 140, "maxPoints": 200, "reasonCode": "OK", "reason": "...", "details": { "windowDays": 90, "fxRate": 5.00 } },
    { "code": "FREQUENCY", "label": "Frequência",             "rawValue": "5",            "points": 100, "maxPoints": 150, "reasonCode": "OK", "reason": "...", "details": { "windowDays": 90 } },
    { "code": "TENURE",    "label": "Tempo de relacionamento","rawValue": "14 meses",     "points": 140, "maxPoints": 200, "reasonCode": "OK", "reason": "...", "details": {} },
    { "code": "KYC",       "label": "KYC",                    "rawValue": "VALID até 2026-12-01", "points": 75, "maxPoints": 150, "reasonCode": "OK", "reason": "KYC vence em 20 dias", "details": {} } ] }
```
`band`: `LOW|MEDIUM|HIGH`; `bandLabel`: `Baixo risco|Médio risco|Alto risco`. `reasonCode`: `OK | MISSING_DATA` (reason "dado ausente") `| MISSING_FX_RATE` (reason "taxa ausente"). `trigger`: `ACCOUNT_OPENED|DEPOSIT|KYC_UPDATED|MANUAL`.

| Método | Path | Authz | Respostas |
|---|---|---|---|
| GET | `/api/risk-score/accounts/{accountId}` | `credit:read` | 200 `RiskScoreResponse` (último); 404 `RISK_SCORE_NOT_FOUND` |
| GET | `/api/risk-score/accounts/{accountId}/history?limit=20` | `credit:read` | 200 `RiskScoreResponse[]` (mais recente primeiro; `[]` se nenhum) |
| POST | `/api/risk-score/accounts/{accountId}/recalculate` | `credit:write` | 200 `RiskScoreResponse` (`trigger=MANUAL`); 404 `ACCOUNT_NOT_FOUND`; 503 `ACCOUNT_API_UNAVAILABLE` |

Gateway: sem token → 401; token sem escopo → 403 `{ "code": "FORBIDDEN" }`.

## C-6 Configuração (Config Server)
`config-files/risk-score-api.properties` (U-2) — valores **aprovados v1**:
```properties
spring.application.name=risk-score-api
server.port=0
spring.data.mongodb.uri=mongodb://localhost:27017/risk-score-api-mongo
spring.kafka.bootstrap-servers=localhost:9092
risk.params.version=v1
risk.params.window-days=90
risk.params.kyc-expiring-days=30
risk.params.fx-rates.BRL=1.00
risk.params.fx-rates.USD=5.00
risk.params.fx-rates.EUR=5.50
risk.params.fx-rates.GBP=6.40
# faixas: primeira de baixo para cima cujo min <= valor
risk.params.factors.balance.max-points=300
risk.params.factors.balance.bands=0:0,1000:100,10000:200,50000:300
risk.params.factors.volume.max-points=200
risk.params.factors.volume.bands=0:0,0.01:70,5000:140,20000:200
risk.params.factors.frequency.max-points=150
risk.params.factors.frequency.bands=0:0,1:50,4:100,12:150
risk.params.factors.tenure.max-points=200
risk.params.factors.tenure.bands=0:0,6:70,12:140,36:200
risk.params.factors.kyc.valid-points=150
risk.params.factors.kyc.expiring-points=75
risk.params.factors.kyc.invalid-points=0
risk.params.classification=0:HIGH,400:MEDIUM,700:LOW
```
`api-gateway.properties` (U-3): nova rota `risk-score-api` → `lb://risk-score-api`, `Path=/api/risk-score/**`; `security.scopes.claims=scope,permissions`.
`account-api.properties` (U-1): `spring.kafka.bootstrap-servers`, produtor JSON com o type mapping de C-4.
Web (U-4): MSW handlers para C-1, C-2, C-5 com dados que reproduzem o exemplo de US-1 e respostas 403/404.
