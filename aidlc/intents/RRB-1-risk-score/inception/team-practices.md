# 2.2 — Práticas observadas (RRB-1)

Base: `6f7ec3c`, execução em 11/11/2026 UTC. Leitura estimada: 2 min. Não altera produção nem prescreve convenções ausentes.

## Build e testes

Na raiz, JDK 17 (`JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`). Para cada módulo, o comando de build/test é:

```sh
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -pl <módulo> -am clean verify
```

| Módulo | Cobertura/configuração observada |
|---|---|
| `account-api` | JUnit Jupiter/Mockito; teste de repositório inicia Mongo Testcontainers, mesmo sob `unit/`. |
| `transaction-api` | Testes unitários e classes de integração `*IT`. |
| `asset-management-api` | Testes unitários e controllers de integração `*IT`. |
| `notification-api` | Teste unitário de evento, `NotificationApiApplicationIT`; dependência `spring-kafka-test`. |
| `api-gateway` | `ApiGatewayApplicationIT`. |
| `config-server` | `ConfigServerApplicationIT`. |
| `discovery-server` | `DiscoveryServerApplicationIT`. |

Módulos: `pom.xml:28-35`; framework herdado: `pom.xml:43-46`; Testcontainers 1.18.0: `pom.xml:20,69-74`, `account-api/pom.xml:28-36`; Mockito/Jupiter: `account-api/src/test/java/com/royal/reserve/bank/account/api/unit/service/AccountServiceTest.java:9-30`; Mongo iniciado em `account-api/src/test/java/com/royal/reserve/bank/account/api/unit/repository/AccountRepositoryTest.java:37-47`.

**`verify` não executa `*IT`**: Surefire padrão, sem Failsafe configurado (`pom.xml:96-120`, `account-api/pom.xml:1-47`). Para integração de contas, comando explícito (não executado aqui):

```sh
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -pl account-api surefire:test -Dtest=AccountControllerIT
```

Esse `AccountControllerIT` usa `@WebMvcTest` e serviço mockado, não Mongo real (`account-api/src/test/java/com/royal/reserve/bank/account/api/integration/controller/AccountControllerIT.java:30-37`). Já testes de contexto completo dependem da configuração/infraestrutura (`account-api/src/test/java/com/royal/reserve/bank/account/api/integration/AccountApiApplicationIT.java:11-16`, `account-api/src/main/resources/application.properties:1-3`). Ausência de testes padrão nos módulos só com `*IT` não prova integração saudável.

Web (`web/package.json:6-16`):

```sh
source ~/.nvm/nvm.sh
cd web
npm ci
npm run lint && npm run typecheck && npm test
npm run format:check
npm run build
npm run test:e2e
```

Vitest/jsdom + Testing Library; MSW rejeita requests sem handler e reseta store/localStorage (`web/vitest.config.ts:4-12`, `web/src/test/setup.ts:1-12`). Playwright inicia Vite, Chromium, sem retries (`web/playwright.config.ts:6-25`); dev usa MSW (`web/.env.development:1`). Não equivale a teste contra backend/Auth0 reais.

## Evidência: comandos solicitados, uma execução cada

Backend: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -pl account-api -am clean verify` — **exit 1**, antes de qualquer edição:

```text
AccountRepositoryTest.shouldCreateAccount » ExceptionInInitializer
Tests run: 28, Failures: 0, Errors: 1, Skipped: 0
royal-reserve-bank ................................. SUCCESS [  1.612 s]
account-api ........................................ FAILURE [  7.467 s]
BUILD FAILURE
Total time:  9.505 s
```

Causa registrada pelo Testcontainers ao inicializar o repositório:

```text
UnixSocketClientProviderStrategy: failed with exception BadRequestException (Status 400: {"message":"client version 1.32 is too old. Minimum supported API version is 1.40, please upgrade your client to a newer version"}
Could not find a valid Docker environment. Please check configuration.
```

Docker CLI responde: `29.9.2 API=1.56 Min=1.40`; JDK 17/socket existem. Bloqueio é compatibilidade do cliente usado pelo teste, não ausência do daemon. Não houve 429/pull; mirror.gcr.io não foi aplicado. Sem mudar dependências/daemon e sem repetir build: registrar falha é o escopo. Log integral: `/home/ubuntu/rrb-account-verify.log` (local, não versionado).

Web: `source ~/.nvm/nvm.sh && cd web && npm run lint && npm run typecheck && npm test` — **exit 0**:

```text
web/public/mockServiceWorker.js
  1:1  warning  Unused eslint-disable directive (no problems were reported)
✖ 1 problem (0 errors, 1 warning)
> tsc -b --noEmit
Test Files  4 passed (4)
     Tests  11 passed (11)
  Duration  2.11s (environment 59%, setup 15%, tests 15%, transform 5%, import 5%, worker 1%)
```

Log: `/home/ubuntu/rrb-web-check.log`. Bundle web, Playwright, demais módulos e `*IT` não executados nesta etapa documental.

## CI, estilo e colaboração

Único workflow encontrado: `.github/workflows/web.yml:3-36`; push/PR só se tocar web/workflow. Node 22 + npm 12, install, lint, Prettier, typecheck, Vitest, build e Playwright. Nenhum workflow backend encontrado; documentos AI-DLC não acionam esse filtro.

Java: pacotes controller/service/repository/model/dto/config, Lombok (`@Data`, `@Builder`, construtores), DTOs separados, quatro espaços e Javadoc em inglês; `package-info.java` descreve pacotes. Exemplos: `account-api/src/main/java/com/royal/reserve/bank/account/api/model/Account.java:13-28`, `account-api/src/main/java/com/royal/reserve/bank/account/api/dto/AccountRequest.java:11-22`, `account-api/src/main/java/com/royal/reserve/bank/account/api/model/package-info.java:1-4`. Não há enforcement Java de estilo nos plugins raiz (`pom.xml:96-120`). Web oferece ESLint/Prettier/TypeScript (`web/package.json:11-15`).

Histórico local completo (`git rev-parse --is-shallow-repository`: `false`; `git log -12`): merges de PR #20/#22 com branches `devin/<timestamp>-<descrição>` (`fc12532`, `debd269`); commits misturam `feat:`, `chore:`, `web:` e `RRB-1 aidlc:` (`c705ab9`, `6a86689`, `187278c`, `6f7ec3c`). Branch documental atual: `aidlc/RRB-1-spec`. São práticas observadas, não evidência de política obrigatória de aprovação/squash/Conventional Commits.
