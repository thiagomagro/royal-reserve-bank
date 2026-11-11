# 1.3 Feasibility & Constraints — INT-1

## O que o sistema tem `[código]`
- `account-api` (Spring Boot, MongoDB, Redis cache): `Account{id, accountNumber, accountHolderName, balance, currency}`; `POST/GET/DELETE /api/account`, `POST /api/account/{id}/deposit`.
- `transaction-api` (PostgreSQL) publica `TransactionEvent{transactionId}` no Kafka `notificationTopic`; consumido por `notification-api`.
- Kafka, Config Server (parâmetros externalizados), Eureka, API Gateway (Auth0/OAuth2 em `SecurityConfig`).
- `web` (React + TanStack Query, mocks MSW, Playwright): Dashboard, Contas, Ativos, Nova transação, Configurações.

## O que falta para entregar
| Necessidade | Situação | Caminho provável |
|---|---|---|
| Tempo de relacionamento | Conta não tem data de abertura | Campo `openedAt` na conta (backfill para contas existentes) |
| KYC em dia | Inexistente | Campos `kycStatus` + `kycValidUntil`, edição manual |
| Volume/frequência de movimentação | Transações não ligadas à conta e sem data; só depósito movimenta saldo | Registrar movimentações por conta (data, valor, tipo) a partir do depósito |
| Recalcular ao movimentar | Não há evento de movimentação de conta | Evento de domínio (Kafka) na movimentação → recálculo |
| Score + histórico + fatores | Inexistente | Componente de score (novo serviço ou módulo; decidido em 2.6 ADR) |
| Consulta pelo motor | Inexistente | Endpoint de leitura do último score via gateway |
| Visão do Crédito com motivos | Inexistente | Tela no `web` com score, faixa e contribuição por fator |
| Perfil Crédito | Gateway valida token, sem papéis | Autorização por papel/escopo `credit` (detalhe em 2.1) |
| Saldo comparável entre moedas | `Currency` na conta, sem câmbio | Tabela de taxas → BRL em configuração; taxa usada nos fatores |
| Pesos/limiares configuráveis | Config Server existe | Propriedades do score no Config Server, nunca constantes |

**Veredito:** viável no stack atual, sem tecnologia nova. Maior esforço: dados de base (abertura, KYC, movimentações) que hoje não existem.

## Restrições
- LGPD art. 20: decisão automatizada exige explicação e revisão → score explicável por fator; não decide sozinho no MVP.
- BACEN (Res. CMN 4.557 / gestão de risco de crédito): rastreabilidade do cálculo → histórico com versão dos parâmetros.
- Minimização de dados: score visível só ao perfil Crédito; sem dados sensíveis (raça, gênero etc.) como fator.
- Prazo: linha de crédito no próximo trimestre `[demanda]`.

## RAID
| ID | Tipo | Descrição | Mitigação |
|---|---|---|---|
| RISK-1 | Risco | Contas existentes sem data de abertura/KYC/histórico geram score pouco informativo | Regra explícita para dado ausente (pontuação neutra/penalizada, a aprovar) e backfill |
| RISK-2 | Risco | Pesos iniciais sem base histórica podem não discriminar risco | Shadow mode + histórico; recalibrar com Crédito |
| RISK-3 | Risco | Uso do score sem explicação violaria LGPD art. 20 | Fatores e contribuições sempre retornados junto ao score |
| RISK-4 | Risco | Recalcular por evento pode ficar inconsistente se evento se perder | Recálculo idempotente + recálculo sob demanda/reprocessamento |
| ASSUMPTION-1 | Premissa | 1 conta = 1 cliente (titular) no MVP | Aprovada pelo PO (3a) |
| ASSUMPTION-2 | Premissa | Movimentação = depósitos agora; débitos/saques quando existirem | Confirmada pelo PO no G1 |
| ASSUMPTION-3 | Premissa | Tabela inicial de pesos/faixas proposta pelo Devin, aprovada pelo Crédito no G2 | Aprovada pelo PO (2a) |
| ASSUMPTION-4 | Decisão (G1, ajustada) | Saldo convertido para BRL por tabela de taxas fixa e configurável (sem integração externa no MVP); taxa usada registrada nos fatores do score | Ajustada pelo PO no G1 |
| ISSUE-1 | Issue | Gateway não tem papéis de usuário | Tratar em 2.1/2.6 |
| DEP-1 | Dependência | Motor de liberação de crédito (consumidor) fora deste repo | Contrato de API em 2.8 |
