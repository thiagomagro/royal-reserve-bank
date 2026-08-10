# Risk Score por Cliente — Design Doc

## Problema

**Problema:** hoje o Royal Reserve Bank não tem nenhuma medida de risco associada a quem opera na plataforma; contas e transações são tratadas sem qualquer diferenciação de perfil.

**Resultado esperado:** cada cliente possui um *risk score* consultável em tempo real por outros serviços (ex.: liberação de crédito) e também recalculado em background, considerando saldo, volume e frequência de transações, tempo de casa e KYC.

## Contexto atual (verificado no código)

O sistema é um conjunto de microsserviços Spring Boot 3 / Java 17 (`pom.xml`), com Eureka, Config Server, API Gateway, Kafka e bancos separados por serviço.

Pontos que condicionam diretamente esta demanda:

1. **Não existe entidade "cliente".** O que existe é conta: `account-api/src/main/java/com/royal/reserve/bank/account/api/model/Account.java` tem apenas `id`, `accountNumber`, `accountHolderName`, `balance`, `currency`. Não há CPF/CNPJ, data de abertura, dados de KYC ou qualquer outro atributo cadastral. O "cliente" hoje é, na prática, uma string de nome — e `accountHolderName` não é único, portanto **não há como agrupar as contas de um mesmo cliente**.
2. **Transação não é ligada a conta nem a cliente.** `transaction-api/src/main/java/com/royal/reserve/bank/transaction/api/model/Transaction.java` guarda só `id`, `transactionId` (UUID) e uma lista de `TransactionItems` (`assetCode`, `assetName`, `value` como `int`). O payload de entrada `transaction-api/src/main/java/com/royal/reserve/bank/transaction/api/dto/TransactionRequest.java` contém apenas `transactionItemsDtoList` — não há remetente, destinatário nem valor monetário com moeda. Ou seja, **não é possível hoje reconstruir o histórico transacional de um cliente**.
3. **O evento publicado no Kafka é mínimo.** `transaction-api/src/main/java/com/royal/reserve/bank/transaction/api/service/TransactionService.java` envia `new TransactionEvent(transaction.getTransactionId())` no tópico `notificationTopic`; `notification-api/src/main/java/com/royal/reserve/bank/notification/api/NotificationApiApplication.java` só faz log do `transactionId`. Não há stream de eventos rico para alimentar um score.
4. **Não há consulta de contas por cliente.** `account-api/src/main/java/com/royal/reserve/bank/account/api/service/AccountService.java` — em `getAllAccounts()` — lê tudo do Mongo e cacheia a lista inteira em Redis sob a chave `accounts`; `deleteAccountByAccountHolderName` faz `findAll()` e filtra em Java. `account-api/src/main/java/com/royal/reserve/bank/account/api/repository/AccountRepository.java` é um `MongoRepository` sem nenhum método de busca. Qualquer leitura "por cliente" precisa ser criada.
5. **Não há conversão de moeda.** Cada conta tem sua própria `Currency` (`Account.java`; os dados de teste em `account-api/src/main/java/com/royal/reserve/bank/account/api/util/AccountTestData.java` usam EUR, GBP e HUF) e não existe nenhum serviço, tabela ou cliente de câmbio no repositório. Somar saldos das contas de um cliente exige uma moeda base e uma fonte de taxas que hoje não existem.
6. **Não há nenhum job agendado.** Não existe uso de `@Scheduled`/`@EnableScheduling` em nenhum módulo — o modo background precisa ser construído do zero (ou delegado a um agendador externo).
7. **Gateway e segurança.** Rotas são declaradas em `config-files/api-gateway.properties` (`spring.cloud.gateway.routes[n]`); `api-gateway/src/main/java/com/royal/reserve/bank/api/gateway/config/SecurityConfig.java` exige autenticação em `anyExchange()` exceto `/eureka/**` e `/discovery-server/**`. Um novo serviço precisa de rota nova + registro no Eureka. Não há autorização por escopo/role — apenas "token válido".
8. **Infra e build.** Novo módulo implica entrada em `pom.xml` (`<modules>`), serviço no `docker-compose.yml`, arquivo em `config-files/` e, se usar banco próprio, mais um container. Não há workflows de CI em `.github/` (só templates de issue), então a validação é `mvn verify` local.
9. **Dados de teste são semeados via `CommandLineRunner`** (`AccountTestData.java`, `asset-management-api/src/main/java/com/royal/reserve/bank/asset/management/api/util/AssetTestData.java`), e `spring.jpa.hibernate.ddl-auto=create-drop` no account/asset (`config-files/account-api.properties`, `config-files/asset-management-api.properties`) — o ambiente é de demonstração, não produtivo.

## Decisões do negócio (respondidas)

- **Granularidade:** o score é **por cliente**, agregando todas as contas dele.
- **Fatores:** saldo, volume de transações, frequência de transações, tempo de casa e KYC.
- **Régua:** o time técnico propõe a régua inicial (abaixo), a ser validada por Risco/Crédito.
- **Modos de operação:** os dois. (a) **Realtime**, consultável por serviços como liberação de crédito; (b) **background**, recalculando periodicamente e podendo incorporar fatores econômicos internos do banco.
- **Bureau externo:** fora de escopo por enquanto.
- **Exposição:** score **interno** apenas, não exposto ao cliente final.

### Consequência direta: nenhum dos fatores pedidos existe hoje

| Fator pedido | Dado necessário | Existe hoje? |
|---|---|---|
| Saldo (agregado do cliente) | agrupamento conta→cliente + moeda base | Não (itens 1 e 5) |
| Volume de transações | valor monetário na transação + dono da transação | Não (item 2) |
| Frequência de transações | data/hora da transação + dono da transação | Não (item 2 — `Transaction` não tem timestamp) |
| Tempo de casa | data de abertura do cliente/conta | Não (item 1) |
| KYC | status e validade de KYC | Não (item 1) |

Portanto existe uma **Fase 0 obrigatória de modelo de dados**, anterior a qualquer cálculo de score:

- criar entidade `Customer` no `account-api` (documento/identificação, data de abertura, status de KYC) e passar `Account` a referenciar `customerId`;
- incluir na transação o `customerId` (ou `accountId`) de origem/destino, o valor monetário com moeda e o timestamp;
- enriquecer o evento publicado no Kafka com esses campos (hoje só `transactionId`).

Sem a Fase 0 é possível entregar apenas um score de fachada; com ela, qualquer das abordagens abaixo funciona.

## Régua inicial proposta (para validação de Risco/Crédito)

Escala **0–1000, maior = mais risco**; faixas: **BAIXO 0–299**, **MÉDIO 300–599**, **ALTO 600–1000**. Score = soma ponderada de quatro componentes, cada um normalizado em 0–1000:

| Componente | Peso | Régua proposta |
|---|---|---|
| KYC | 30% | verificado e vigente = 0; incompleto/pendente = 500; ausente ou expirado = 1000 |
| Tempo de casa | 20% | > 24 meses = 0; 6–24 meses = 400; < 6 meses = 1000 |
| Saldo agregado (moeda base) | 20% | acima do teto definido = 0; entre piso e teto = interpolação linear; abaixo do piso = 1000 |
| Comportamento transacional (90 dias) | 30% | 0 quando volume e frequência estão dentro da faixa esperada do próprio cliente; sobe conforme o desvio (ex.: valor ou frequência acima de 3× a mediana histórica = 1000); cliente sem histórico = 500 |

Pontos que o negócio precisa fechar: piso/teto de saldo, moeda base e a faixa esperada de comportamento. Todos os pesos e limiares devem ser parametrizáveis via Config Server (`config-files/`), não constantes em código, para permitir recalibração sem deploy. Cada score deve persistir os **fatores que o compuseram**, tanto para auditoria quanto porque score sem explicação não é acionável por Risco.

## Abordagens

Todas assumem a Fase 0 concluída.

### Abordagem A — Score dentro do `account-api`

O `account-api` — que passará a ser o dono do cadastro de cliente — expõe `GET /api/customer/{id}/risk-score` (realtime) e mantém um `@Scheduled` de recálculo, persistindo o resultado junto ao cliente em Mongo. Os dados transacionais vêm do `transaction-api` via Feign ou de um consumo do tópico Kafka.

- **Escopo:** novo pacote de risco no `account-api`, endpoint, job agendado, consumo de eventos.
- **Componentes afetados:** `account-api` (+ dependência Kafka/Feign nova), `config-files/account-api.properties`, cache Redis existente.
- **Esforço:** M
- **Dependências:** Fase 0.
- **Risco de desenho:** acopla cadastro e motor de risco no mesmo serviço; o recálculo em background compete por recursos com o caminho de leitura de contas, e a invalidação atual da chave `accounts` (item 4) fica mais frequente.
- **Confiança de sucesso da implantação: 75**

### Abordagem B — Novo microsserviço `risk-score-api` com score persistido, endpoint realtime e recálculo por evento + job

Módulo novo `risk-score-api` com banco próprio (padrão database-per-service), entidade `CustomerRiskScore` (`customerId`, `score`, faixa, fatores, `calculatedAt`) e três caminhos: (1) `GET /api/risk-score/{customerId}` devolvendo o score persistido — leitura barata, adequada a consulta por serviços de crédito; (2) consumo do evento de transação enriquecido no Kafka, recalculando o cliente afetado; (3) job de recálculo geral, onde entram os fatores econômicos internos do banco. Dados cadastrais vêm do `account-api` por Feign, no mesmo padrão de `transaction-api/.../client/AssetManagementClient.java`.

- **Escopo:** novo módulo Maven + rota no gateway + serviço no `docker-compose.yml` + arquivo em `config-files/` + novo tópico Kafka; endpoint de leitura de cliente no `account-api`.
- **Componentes afetados:** novo módulo, `account-api`, `transaction-api` (publicação do evento enriquecido — já contemplada na Fase 0), `api-gateway`, `pom.xml`, `docker-compose.yml`, coleção Postman.
- **Esforço:** G
- **Dependências:** Fase 0; definição de piso/teto e moeda base.
- **Confiança de sucesso da implantação: 70**

### Abordagem C — `risk-score-api` apenas em batch, com leitura do score materializado

Mesmo serviço da Abordagem B, sem consumo de eventos: o score é recalculado somente pelo job e a consulta devolve o último valor materializado.

- **Escopo:** subconjunto da Abordagem B (sem consumidor Kafka).
- **Componentes afetados:** novo módulo, `account-api`, `api-gateway`, `pom.xml`, `docker-compose.yml`.
- **Esforço:** M
- **Dependências:** Fase 0.
- **Limitação:** atende "consulta rápida", mas **não atende o requisito de realtime** — um cliente que acabou de transacionar é avaliado com dados velhos, exatamente o cenário em que a liberação de crédito mais precisa do score atualizado.
- **Confiança de sucesso da implantação: 80** (confiança alta de implantar, cobertura funcional parcial)

## Trade-offs

| Critério | A — no account-api | B — serviço + eventos + job | C — serviço batch |
|---|---|---|---|
| Complexidade | Média | Alta | Média |
| Risco de entrega | Médio | Médio-alto | Médio |
| Impacto em sistemas existentes | Concentrado no `account-api` | Espalhado (gateway, Kafka, compose, 2 serviços) | Moderado |
| Reversibilidade | Média (código dentro de serviço crítico) | Alta (serviço isolado, desligável) | Alta |
| Atende realtime | Sim | Sim | **Não** |
| Atende background | Sim | Sim | Sim |
| Isolamento de falha | Baixo (risco derruba cadastro) | Alto | Alto |
| Auditabilidade | Média | Alta (fatores persistidos + histórico) | Alta |
| Evolução (fatores econômicos, bureau futuro) | Ruim (engorda o `account-api`) | Boa | Boa |

## Riscos

- **Risco de dado, o principal:** os cinco fatores pedidos dependem de dados que não existem (tabela acima). A Fase 0 é o verdadeiro caminho crítico; qualquer cronograma que a ignore vai entregar score sem lastro.
- **Contrato público:** incluir cliente, valor e timestamp em `TransactionRequest` afeta `postman/postman-collection.json` e qualquer consumidor externo. Exige campo opcional em transição ou versionamento de rota.
- **Migração de dados:** contas existentes não têm `customerId` nem data de abertura; é preciso decidir o backfill (e o "tempo de casa" de clientes legados fica sem base real).
- **Câmbio:** sem fonte de taxas (item 5), o saldo agregado de clientes multimoeda é incalculável ou usa taxa fixa — o que distorce o score. Decisão explícita do negócio.
- **Compliance / LGPD:** score de risco é dado pessoal com finalidade específica. Mesmo sendo interno, precisa de base legal, política de retenção, log de acesso e trilha dos fatores. **Validar com Jurídico/Privacidade antes de persistir score.**
- **Discriminação algorítmica:** régua com peso alto em saldo e tempo de casa penaliza cliente novo e de baixa renda. Precisa de revisão por Risco/Crédito e de registro dos fatores de cada score. Recomenda-se rodar em modo *shadow* (calculando sem consumidor) antes de qualquer uso em decisão de crédito.
- **Segurança:** `SecurityConfig.java` só valida token, sem escopos. Como o score é interno, expor a rota no gateway sem autorização por role deixa o dado acessível a qualquer chamador autenticado — é preciso autorização por role, ou não publicar a rota no gateway e permitir apenas chamada serviço-a-serviço via Eureka. Nota lateral: há um JWT hardcoded em `config-files/api-gateway.properties`, o que reforça que o ambiente atual não é produtivo.
- **Carga do job:** o recálculo em background varre todos os clientes; com `AccountService.getAllAccounts()` no formato atual (item 4), o job precisa de paginação própria em vez de reusar essa leitura.
- **Ausência de CI:** sem workflow em `.github/`, a regressão depende de execução local de `mvn verify`.

## Recomendação

Recomendo a **Abordagem B**, precedida da Fase 0 como épico próprio e priorizado. O requisito de ter os dois modos — realtime para crédito e background com fatores econômicos internos — exige score persistido, atualização por evento e job de recálculo, e colocar esse motor dentro do `account-api` (A) acopla risco a cadastro num serviço crítico e dificulta a evolução para bureau externo. A Abordagem C fica como fallback de escopo reduzido, ciente de que não atende o realtime pedido.

## Perguntas em aberto

1. **Moeda base e fonte de taxas de câmbio** para o saldo agregado (item 5 do contexto) — bloqueante para o componente de saldo.
2. **Piso e teto de saldo** e a **faixa esperada de comportamento transacional** da régua inicial.
3. Quais são os **fatores econômicos internos do banco** que devem entrar no recálculo em background, e de onde vêm?
4. **Periodicidade** do recálculo em background (diário? horário?) e SLA de latência aceitável no endpoint realtime.
5. **Backfill:** como tratar "tempo de casa" e KYC de contas já existentes sem data de abertura?
6. De onde vem o **status de KYC** — será cadastrado no próprio `account-api` ou há sistema externo de origem?
7. Quem são os **consumidores autorizados** do score, para desenhar a autorização (role no gateway vs. chamada interna)?
