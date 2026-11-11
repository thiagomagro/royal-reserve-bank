# 2.4 User Stories — INT-1 (Epic RRB-2)

## US-1 [RRB-7] Consultar score e motivos de uma conta (REQ-5, REQ-6, REQ-8, REQ-9, REQ-11, REQ-12, NFR-3, NFR-5)
Como **Analista de Crédito**, quero ver o score, a faixa e a contribuição de cada fator de uma conta, para entender por que o cliente tem aquele risco.
```gherkin
Cenário: score explicado
  Dado uma conta em USD com saldo 3.000, aberta há 14 meses, KYC válido por mais 20 dias e 5 depósitos somando 2.000 USD nos últimos 90 dias
  Quando o analista abre a página de risco da conta
  Então vê score 655, faixa "Médio risco" e modo "Informativo"
  E vê os fatores: Saldo 200 (15.000 BRL, taxa USD 5,00), Volume 140 (10.000 BRL), Frequência 100 (5), Tempo de relacionamento 140 (14 meses), KYC 75 (vence em ≤ 30 dias)
Cenário: usuário sem perfil Crédito
  Dado um usuário sem escopo credit:read
  Quando acessa a página ou a API de score
  Então recebe "acesso negado" (HTTP 403) e nenhum score é exibido
```

## US-2 [RRB-3] Ver histórico de scores (REQ-8, REQ-10, REQ-11)
Como **Analista de Crédito**, quero ver o histórico dos cálculos de uma conta, para acompanhar a evolução durante a observação.
```gherkin
Cenário: histórico ordenado
  Dado uma conta com 3 cálculos de score
  Quando o analista abre o histórico
  Então vê 3 linhas do mais recente para o mais antigo com data, score, faixa, versão de parâmetros e motivo do disparo
```

## US-3 [RRB-5] Registrar KYC da conta (REQ-2, REQ-4)
Como **Analista de Crédito**, quero registrar status e validade do KYC de uma conta, para que o score reflita o cadastro em dia.
```gherkin
Cenário: KYC atualizado recalcula score
  Dado uma conta com KYC ausente e fator KYC 0 "dado ausente"
  Quando o analista registra KYC "VALID" com validade daqui a 365 dias
  Então o KYC fica salvo na conta
  E em até 5 segundos o score é recalculado com fator KYC 150
Cenário: validação
  Quando o analista informa status VALID sem data de validade
  Então recebe erro de validação e nada é salvo
```

## US-4 [RRB-4] Motor consulta score via API (REQ-9, REQ-12, NFR-1, NFR-3)
Como **Motor de liberação de crédito**, quero consultar o último score de uma conta por API, para usá-lo na decisão (em modo informativo).
```gherkin
Cenário: consulta rápida
  Dado uma conta com score calculado
  Quando o motor chama GET /api/risk-score/accounts/{id} com escopo credit:read
  Então recebe 200 com score, band, mode "SHADOW", factors e paramsVersion em p95 ≤ 200 ms
Cenário: conta sem score
  Quando consulta uma conta sem cálculo
  Então recebe 404 RISK_SCORE_NOT_FOUND
```

## US-5 [RRB-6] Score atualizado ao movimentar (REQ-1, REQ-3, REQ-4, NFR-2, NFR-6)
Como **Analista de Crédito**, quero que o score se atualize quando a conta é aberta ou movimentada, para decidir com informação atual.
```gherkin
Cenário: depósito recalcula
  Dado uma conta com score calculado
  Quando é feito um depósito de 100
  Então a movimentação é registrada com data, valor, moeda e tipo DEPOSIT
  E em até 5 segundos existe um novo cálculo com trigger "DEPOSIT"
Cenário: evento duplicado
  Quando o mesmo evento é entregue duas vezes
  Então o score final é o mesmo e não há cálculo duplicado para o mesmo eventId
```

## US-6 [RRB-8] Parâmetros versionados e conversão BRL (REQ-6, REQ-7, NFR-4)
Como **Gestor de Política de Crédito**, quero que pesos, faixas, janela e taxas estejam em configuração versionada e registrados em cada cálculo, para auditar e recalibrar sem mudar código.
```gherkin
Cenário: versão registrada
  Dado os parâmetros na versão "v1"
  Quando um score é calculado
  Então o cálculo registra paramsVersion "v1" e a taxa de câmbio usada
Cenário: moeda sem taxa
  Dado uma conta em HUF sem taxa configurada
  Quando o score é calculado
  Então os fatores Saldo e Volume valem 0 com motivo "taxa ausente"
```

## US-7 [RRB-9] Reprocessar score de uma conta (REQ-13, NFR-6)
Como **Analista de Crédito**, quero pedir o recálculo de uma conta, para corrigir um score desatualizado.
```gherkin
Cenário: recálculo sob demanda
  Quando o analista aciona "Recalcular" (POST /api/risk-score/accounts/{id}/recalculate)
  Então um novo cálculo é gravado com trigger "MANUAL" e retornado
```
