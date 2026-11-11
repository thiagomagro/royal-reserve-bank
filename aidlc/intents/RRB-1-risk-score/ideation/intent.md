# INT-1 — Score de risco por cliente

**Problema:** o banco vai lançar crédito pessoal no próximo trimestre e hoje trata toda conta igual, sem saber quem é cliente de maior ou menor risco. `[demanda]`

**Quem se beneficia:** Diretoria/time de Crédito (política da nova linha) e o motor de liberação de crédito (consulta na decisão). `[demanda]`

**Resultado desejado:** um score de risco por cliente que `[demanda]`
- sistemas internos consultam na hora da decisão;
- o time de Crédito vê no internet banking, com o *porquê* do score (explicável);
- é atualizado conforme o cliente movimenta a conta.

**Fatores sugeridos pelo negócio:** saldo, volume e frequência de movimentação, tempo de relacionamento, KYC em dia. `[demanda]`

**Restrição de uso:** no início o score **não decide crédito sozinho**; primeiro observar os resultados. `[demanda]`

**Métrica de sucesso:** Crédito consegue usar o score na política da nova linha de crédito. `[demanda]` (critério mensurável a confirmar com o PO `[premissa]`)

## Stakeholders
| Papel | Quem | Interesse |
|---|---|---|
| Decide / patrocina | Diretoria de Crédito | Política da nova linha |
| Usuário | Time de Crédito (analistas) | Ver score e motivos no internet banking |
| Consumidor sistêmico | Motor de liberação de crédito | Consultar score na decisão |
| Afetado | Compliance / Privacidade (LGPD, BACEN) | Explicabilidade, uso de dados pessoais, decisão automatizada |
| Executa | Engenharia RRB | account-api, transaction-api, web |
| Afetado | Clientes | Tratamento justo; direito de revisão (LGPD art. 20) `[premissa]` |

## Observações do código (detalhe em feasibility)
- `Account` tem só `accountNumber`, `accountHolderName`, `balance`, `currency`: não há entidade cliente, data de abertura nem KYC. `[código]`
- Transações (`transaction-api`) são compras de ativos sem vínculo com conta nem data; só depósitos (`POST /api/account/{id}/deposit`) movimentam saldo. `[código]`
