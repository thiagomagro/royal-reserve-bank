# 1.4 Scope Definition — INT-1

## Em escopo (MVP)
- Dados de base na conta: data de abertura, KYC (status + validade) editável manualmente, registro de movimentações por conta.
- Cálculo do score 0–1000 + faixa Baixo/Médio/Alto por regras com pesos/limiares em configuração.
- Recálculo automático a cada movimentação (evento) e histórico de cada cálculo com fatores e versão dos parâmetros.
- API de consulta do último score (com fatores) para sistemas internos.
- Tela no internet banking, só perfil Crédito: score, faixa, motivos por fator e histórico.
- Modo informativo (shadow): nada bloqueia ou aprova crédito.

## Fora de escopo
- Decisão automática de crédito ou integração que bloqueie operações.
- Entidade Cliente agregando várias contas.
- Integração KYC externa / bureaus (Serasa, SCR).
- Modelo estatístico/ML.
- Visão do score pelo próprio cliente.

## Depois
- Entidade Cliente e score por cliente multi-conta.
- Calibração dos pesos com dados observados no shadow.
- Score do cliente visível no app, com direito de revisão.
- Débitos/saques como movimentação, quando existirem.

## Fatia MVP que prova valor primeiro
Conta com abertura + KYC + depósitos → score calculado e explicado → Crédito vê na tela e motor consulta via API.
