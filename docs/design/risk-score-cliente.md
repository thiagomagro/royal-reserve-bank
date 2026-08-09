# Risk Score por Cliente — Design Doc

## Problema

**Problema:** hoje o Royal Reserve Bank não tem nenhuma medida de risco associada a quem opera na plataforma; contas e transações são tratadas sem qualquer diferenciação de perfil.

**Resultado esperado:** cada cliente possui um *risk score* consultável e atualizado, que possa ser usado para decisões de negócio (limites, alçadas, alertas, futura recusa de transação).

## Contexto atual (verificado no código)

O sistema é um conjunto de microsserviços Spring Boot 3 / Java 17 (`pom.xml`), com Eureka, Config Server, API Gateway, Kafka e bancos separados por serviço.

Pontos que condicionam diretamente esta demanda:

1. **Não existe entidade "cliente".** O que existe é conta: `account-api/src/main/java/com/royal/reserve/bank/account/api/model/Account.java` tem apenas `id`, `accountNumber`, `accountHolderName`, `balance`, `currency`. Não há CPF/CNPJ, data de nascimento, renda, endereço ou qualquer atributo cadastral. O "cliente" hoje é, na prática, uma string de nome.
2. **Transação não é ligada a conta nem a cliente.** `transaction-api/src/main/java/com/royal/reserve/bank/transaction/api/model/Transaction.java` guarda só `id`, `transactionId` (UUID) e uma lista de `TransactionItems` (`assetCode`, `assetName`, `value` como `int`). O payload de entrada `transaction-api/src/main/java/com/royal/reserve/bank/transaction/api/dto/TransactionRequest.java` contém apenas `transactionItemsDtoList` — não há remetente, destinatário nem valor monetário com moeda. Ou seja, **não é possível hoje reconstruir o histórico transacional de um cliente**.
3. **O evento publicado no Kafka é mínimo.** `transaction-api/src/main/java/com/royal/reserve/bank/transaction/api/service/TransactionService.java` envia `new TransactionEvent(transaction.getTransactionId())` no tópico `notificationTopic`; `notification-api/src/main/java/com/royal/reserve/bank/notification/api/NotificationApiApplication.java` só faz log do `transactionId`. Não há stream de eventos rico para alimentar um score.
4. **Não há consulta de contas por cliente.** `account-api/src/main/java/com/royal/reserve/bank/account/api/service/AccountService.java` — em `getAllAccounts()` — lê tudo do Mongo e cacheia a lista inteira em Redis sob a chave `accounts`; `deleteAccountByAccountHolderName` faz `findAll()` e filtra em Java. `account-api/src/main/java/com/royal/reserve/bank/account/api/repository/AccountRepository.java` é um `MongoRepository` sem nenhum método de busca. Qualquer leitura "por cliente" precisa ser criada.
5. **Gateway e segurança.** Rotas são declaradas em `config-files/api-gateway.properties` (`spring.cloud.gateway.routes[n]`); `api-gateway/src/main/java/com/royal/reserve/bank/api/gateway/config/SecurityConfig.java` exige autenticação em `anyExchange()` exceto `/eureka/**` e `/discovery-server/**`. Um novo serviço precisa de rota nova + registro no Eureka. Não há autorização por escopo/role — apenas "token válido".
6. **Infra e build.** Novo módulo implica entrada em `pom.xml` (`<modules>`), serviço no `docker-compose.yml`, arquivo em `config-files/` e, se usar banco próprio, mais um container. Não há workflows de CI em `.github/` (só templates de issue), então a validação é `mvn verify` local.
7. **Dados de teste são semeados via `CommandLineRunner`** (`account-api/src/main/java/com/royal/reserve/bank/account/api/util/AccountTestData.java`, `asset-management-api/src/main/java/com/royal/reserve/bank/asset/management/api/util/AssetTestData.java`), e `spring.jpa.hibernate.ddl-auto=create-drop` no account/asset (`config-files/account-api.properties`, `config-files/asset-management-api.properties`) — o ambiente é de demonstração, não produtivo.

## Premissas

- O score é **informativo** nesta primeira entrega: nenhum bloqueio automático de transação.
- Escala 0–1000 (maior = mais risco) com faixas BAIXO / MÉDIO / ALTO, definidas pelo negócio.
- "Cliente" será representado pelo titular da conta; enquanto não houver cadastro real, a chave é o `id` da conta em Mongo (não `accountHolderName`, que não é único).
- Não há integração com bureau externo (Serasa/SPC) contratada.

## Perguntas em aberto

1. O score é por **cliente** ou por **conta**? Se um titular puder ter várias contas, é preciso criar a entidade cliente antes.
2. Quais fatores o negócio considera? (saldo, volume/frequência transacional, tempo de casa, KYC, bureau externo?)
3. Existe modelo/regra de risco já definido por Risco/Crédito, ou o time de engenharia propõe uma régua inicial?
4. Qual a latência aceitável: score em tempo real na transação, ou recalculado periodicamente?
5. Haverá bureau externo em fase 2? Isso muda o desenho de compliance (LGPD, contrato, retenção).
6. O score pode ser exposto ao próprio cliente ou é interno? (impacta explicabilidade e auditoria)

## Abordagens

### Abordagem A — Score derivado no `account-api` (cálculo síncrono, sem persistência)

Adicionar ao `account-api` um cálculo de score a partir dos dados que já existem em `Account` (saldo, moeda) e expor `GET /api/account/{id}/risk-score`. Sem novo serviço, sem novo banco, sem novo evento.

- **Escopo:** novo serviço/endpoint dentro de `account-api`; regra de pontuação parametrizável via `config-files/account-api.properties`.
- **Componentes afetados:** `account-api` (service, controller, dto), cache Redis existente.
- **Esforço:** P
- **Dependências:** nenhuma além do que já roda.
- **Limitação honesta:** com apenas saldo e moeda disponíveis, o score é pobre e não reflete comportamento — serve como esqueleto e vitrine, não como risco de verdade.
- **Confiança de sucesso da implantação: 90**

### Abordagem B — Novo microsserviço `risk-score-api` com score persistido e atualizado por evento

Criar módulo `risk-score-api` (banco próprio, seguindo o padrão database-per-service), com entidade `CustomerRiskScore` (customerId, score, faixa, fatores, calculadoAt). O score é calculado sob demanda e recalculado quando chegam eventos. Isso exige **enriquecer a transação e o evento**: incluir `accountId`/`customerId` e valor monetário em `TransactionRequest`/`Transaction` e publicar um evento de transação com esses campos, consumido pelo `risk-score-api` via Kafka (mesmo padrão de `notification-api`). Dados cadastrais vêm do `account-api` por Feign, como `transaction-api/src/main/java/com/royal/reserve/bank/transaction/api/client/AssetManagementClient.java` já faz.

- **Escopo:** novo módulo Maven + rota no gateway + entrada no `docker-compose.yml` + arquivo em `config-files/`; alteração contratual em `transaction-api` (DTO, modelo, evento).
- **Componentes afetados:** `transaction-api`, `account-api` (endpoint de consulta por id), `api-gateway`, `pom.xml`, `docker-compose.yml`, Kafka (novo tópico), coleção Postman.
- **Esforço:** G
- **Dependências:** decisão sobre identidade do cliente (pergunta 1); definição dos fatores (pergunta 2).
- **Confiança de sucesso da implantação: 70**

### Abordagem C — `risk-score-api` em modo batch, sem tocar em `transaction-api`

Mesmo serviço novo da Abordagem B, mas alimentado **apenas** pelo `account-api` (via Feign sobre um endpoint de leitura de conta), recalculando o score em job agendado e persistindo o resultado. Não altera o contrato de transação e não consome Kafka.

- **Escopo:** novo módulo + rota no gateway + job agendado + endpoint de consulta; endpoint novo de leitura por id no `account-api`.
- **Componentes afetados:** novo módulo, `account-api` (endpoint de leitura), `api-gateway`, `pom.xml`, `docker-compose.yml`.
- **Esforço:** M
- **Dependências:** definição da régua inicial de fatores cadastrais.
- **Limitação:** score defasado e cego a comportamento transacional, pela mesma razão da Abordagem A (ponto 2 do contexto).
- **Confiança de sucesso da implantação: 80**

## Trade-offs

| Critério | A — no account-api | B — serviço + eventos | C — serviço batch |
|---|---|---|---|
| Complexidade | Baixa | Alta | Média |
| Risco de entrega | Baixo | Médio-alto (contrato de transação muda) | Médio |
| Impacto em sistemas existentes | Só `account-api` | `transaction-api`, `account-api`, gateway, Kafka, compose | `account-api`, gateway, compose |
| Reversibilidade | Alta (remover endpoint) | Baixa (migração de dados + contrato público alterado) | Média (serviço isolado, desligável) |
| Qualidade do score | Fraca (só saldo/moeda) | Alta (comportamento + cadastro) | Média-fraca (só cadastro) |
| Auditabilidade | Nenhuma (não persiste) | Alta (histórico persistido) | Média |

## Riscos

- **Risco de dado, o principal:** sem ligação transação↔cliente (`transaction-api/src/main/java/com/royal/reserve/bank/transaction/api/model/Transaction.java`, `transaction-api/src/main/java/com/royal/reserve/bank/transaction/api/dto/TransactionRequest.java`), nenhum score baseado em comportamento é possível hoje. Qualquer abordagem que prometa isso depende primeiro dessa mudança de contrato.
- **Contrato público:** alterar `TransactionRequest` afeta a coleção em `postman/postman-collection.json` e qualquer consumidor externo. Exige versionamento ou campo opcional em transição.
- **Compliance / LGPD:** score de risco é dado pessoal sensível por finalidade. Precisa de base legal, política de retenção, log de acesso e — se for exposto ao cliente — explicabilidade dos fatores. **Validar com Jurídico/Privacidade antes de persistir score.**
- **Discriminação algorítmica:** régua baseada em saldo pode gerar viés; recomenda-se revisão por Risco/Crédito e registro dos fatores que compuseram cada score.
- **Segurança:** `api-gateway/src/main/java/com/royal/reserve/bank/api/gateway/config/SecurityConfig.java` só valida token, sem escopos. Um endpoint de score fica acessível a qualquer chamador autenticado — é preciso autorização por role antes de expor em ambiente real. Nota lateral: há um JWT hardcoded em `config-files/api-gateway.properties`, o que reforça que o ambiente atual não é produtivo.
- **Consistência de cache:** `AccountService` invalida a chave `accounts` inteira a cada escrita; incluir score nessa lista amplifica invalidações.
- **Ausência de CI:** sem workflow em `.github/`, a regressão depende de execução local de `mvn verify`.

## Recomendação

Recomendo **A como fatia inicial e B como destino**, executados nessa ordem: entregar já o endpoint de score no `account-api` (barato, reversível, valida a régua e a UX com o negócio) e, em paralelo, tratar como projeto próprio a ligação transação↔cliente, que é o verdadeiro pré-requisito. Ir direto para B sem essa decisão de identidade é o caminho mais provável de retrabalho, e C entrega a mesma qualidade de score que A cobrando o custo de um microsserviço novo. Se o negócio exigir de saída um score comportamental confiável, a resposta honesta é que o sistema atual não suporta e o primeiro épico deve ser o modelo de dados, não o score.

## Perguntas em aberto (resumo para o refinamento)

Ver seção "Perguntas em aberto" acima — as bloqueantes para começar são a **1** (cliente vs. conta) e a **2** (fatores do score).
