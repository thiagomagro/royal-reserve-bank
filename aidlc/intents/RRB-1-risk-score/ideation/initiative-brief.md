# Initiative Brief — RRB-1 Score de risco por cliente

**Problema:** crédito pessoal lança no próximo trimestre e o banco trata toda conta igual, sem medida de risco.

**Resultado:** score 0–1000 + faixa (Baixo/Médio/Alto) por cliente, explicável fator a fator, recalculado a cada movimentação, consultável pelo motor de crédito e visível ao perfil Crédito no internet banking. Informativo (shadow) no início.

**Sucesso:** Crédito usa o score na política da nova linha (após período de observação).

**Escopo:** ver `scope.md` (MVP = dados de base + cálculo por regras configuráveis + evento + histórico + API + tela Crédito).

**Top 3 riscos**
1. RISK-1: contas sem dados de base → score pouco informativo.
2. RISK-2: pesos sem base histórica → shadow + recalibração.
3. RISK-3: LGPD art. 20 → explicação sempre junto ao score.

**Decisões do G1**
- ASSUMPTION-2 confirmada: movimentação = depósitos agora; débitos quando existirem.
- ASSUMPTION-4 ajustada: saldo convertido para BRL por tabela de taxas fixa e configurável, taxa usada registrada nos fatores.

**Em aberto (para G2)**
- Tabela de pesos/faixas/taxas e regra para dado ausente: proposta na Inception, aprovada pelo Crédito.
