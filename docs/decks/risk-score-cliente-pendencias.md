# Pendências de dados — deck Risk Score por Cliente

Nada no deck foi estimado: todo número tem origem no design doc. O que falta na fonte e, portanto, ficou fora do deck:

1. **Nenhum indicador de negócio quantificado.** A fonte não traz volume de clientes, perda esperada por inadimplência, ganho estimado com o score nem baseline atual. O slide de impacto fala de cobertura de fatores e auditabilidade, não de retorno financeiro.
2. **Nenhum custo ou prazo.** Não há estimativa de custo, headcount ou cronograma. Os únicos indicadores de esforço da fonte são "M/G" por abordagem e a confiança de implantação (75 / 70 / 80).
3. **Régua não validada.** Pesos (KYC 30%, tempo de casa 20%, saldo 20%, comportamento 30%) e limiares são proposta técnica pendente de confirmação escrita de Risco/Crédito — em especial os valores neutros de cliente legado (400) e KYC ausente (500).
4. **Role de admin.** Não existe no provedor de identidade; chamado aberto pelo negócio. Sem ela não há rota no gateway, logo não há prazo para a exposição da consulta.
5. **Unificação de titulares legados.** Sem critério definido de quem valida a fusão de contas do mesmo titular; até lá, um cliente por conta existente.
6. **Fatores econômicos internos.** Fora desta implementação e sem definição de quais seriam.
7. **Boletim oficial do BCB.** Proposta é o de fechamento, ainda não confirmado pelo negócio.
