# 2.3 Requirements — INT-1 Score de risco por cliente

Convenção do MVP: 1 conta = 1 cliente (ASSUMPTION-1). Score é **informativo** (shadow), nunca bloqueia/aprova crédito.

## Funcionais
| ID | Requisito (testável) |
|---|---|
| REQ-1 | A conta registra **data de abertura** (`openedAt`), preenchida na criação; contas existentes sem data seguem a regra de dado ausente (PARAM-6). |
| REQ-2 | A conta registra **KYC**: status (`VALID`/`PENDING`/`EXPIRED`) e data de validade, editáveis manualmente por usuário autorizado. |
| REQ-3 | Cada **movimentação** da conta (hoje: depósito — ASSUMPTION-2) é registrada com data/hora, valor, moeda e tipo, consultável por conta. |
| REQ-4 | Toda movimentação ou alteração de KYC publica um **evento de domínio** que dispara o recálculo do score da conta. |
| REQ-5 | O score é calculado por **regras de pontos por fator** (saldo, volume, frequência, tempo de relacionamento, KYC), resultando em valor **0–1000** e **faixa** (Baixo/Médio/Alto risco). |
| REQ-6 | O **saldo é convertido para BRL** por tabela de taxas fixa e configurável antes da avaliação; a taxa usada fica registrada nos fatores. |
| REQ-7 | Pesos, faixas de pontuação, limiares de classificação, janela de movimentação e taxas ficam em **configuração versionada** (Config Server), nunca em constante; cada cálculo registra a versão dos parâmetros. |
| REQ-8 | Cada cálculo grava um **histórico** imutável: score, faixa, fatores (valor bruto, pontos, peso, motivo legível), versão dos parâmetros e timestamp. |
| REQ-9 | **API de consulta** retorna o último score de uma conta com fatores e motivos, para sistemas internos (motor de crédito). |
| REQ-10 | **API de histórico** retorna os cálculos anteriores de uma conta, do mais recente para o mais antigo. |
| REQ-11 | **Tela no internet banking** (perfil Crédito) mostra score, faixa, contribuição de cada fator com motivo e o histórico. |
| REQ-12 | O resultado traz a marcação **informativo** (`mode: SHADOW`) e nenhum fluxo existente muda de comportamento com base no score. |
| REQ-13 | É possível **recalcular sob demanda** uma conta (reprocessamento), de forma idempotente. |

## Não funcionais
| ID | Requisito |
|---|---|
| NFR-1 | Latência: consulta do último score p95 ≤ 200 ms local (lê score persistido, sem recálculo). |
| NFR-2 | Atualização: score refletido em ≤ 5 s após a movimentação (p95). |
| NFR-3 | Autorização: APIs de score, KYC e tela exigem papel/escopo **Crédito** (`credit:read`; edição de KYC `credit:write`); demais usuários recebem 403. Motor usa credencial de serviço com `credit:read`. |
| NFR-4 | Auditabilidade (BACEN): todo cálculo é reproduzível a partir de entradas + versão de parâmetros gravadas. |
| NFR-5 | Privacidade/LGPD: explicação por fator sempre junto ao score (art. 20); sem dados sensíveis como fator; score não exposto ao cliente nem em logs. |
| NFR-6 | Resiliência: perda/duplicação de evento não corrompe o score (recálculo idempotente; REQ-13 como reparo). |
| NFR-7 | Observabilidade: métricas de cálculos/s, latência de cálculo, distribuição por faixa e falhas, expostas no Prometheus existente. |
| NFR-8 | Compatibilidade: mudanças em APIs existentes de conta são aditivas (campos opcionais), sem quebrar web/clientes atuais. |

## Parâmetros de negócio — **APROVADOS pelo PO (Crédito) em 2026-11-11** (versão de parâmetros `v1`)
Convenção aprovada: **quanto maior o score, menor o risco** (padrão de mercado). Soma máxima = 1000.

**PARAM-1 Saldo em BRL — peso 300**
| Saldo (BRL) | < 1.000 | 1.000–9.999 | 10.000–49.999 | ≥ 50.000 |
|---|---|---|---|---|
| Pontos | 0 | 100 | 200 | 300 |

**PARAM-2 Volume movimentado na janela (BRL) — peso 200**
| Volume | 0 | < 5.000 | 5.000–19.999 | ≥ 20.000 |
|---|---|---|---|---|
| Pontos | 0 | 70 | 140 | 200 |

**PARAM-3 Frequência (nº de movimentações na janela) — peso 150**
| Qtde | 0 | 1–3 | 4–11 | ≥ 12 |
|---|---|---|---|---|
| Pontos | 0 | 50 | 100 | 150 |

**PARAM-4 Tempo de relacionamento — peso 200**
| Meses | < 6 | 6–11 | 12–35 | ≥ 36 |
|---|---|---|---|---|
| Pontos | 0 | 70 | 140 | 200 |

**PARAM-5 KYC — peso 150**
| Situação | Válido (> 30 dias p/ vencer) | Válido, vence em ≤ 30 dias | Pendente / vencido / ausente |
|---|---|---|---|
| Pontos | 150 | 75 | 0 |

**PARAM-6 Dado ausente:** fator com dado ausente (ex.: abertura em conta antiga) vale **0 ponto** e é marcado "dado ausente" no motivo (conservador).

**PARAM-7 Janela de movimentação:** últimos **90 dias**.

**PARAM-8 Faixas:** 0–399 **Alto risco** · 400–699 **Médio risco** · 700–1000 **Baixo risco**.

**PARAM-9 Taxas → BRL (fixas, configuráveis):** BRL 1,00 · USD 5,00 · EUR 5,50 · GBP 6,40. Moeda sem taxa: saldo e volume valem 0 ponto com motivo "taxa ausente".
