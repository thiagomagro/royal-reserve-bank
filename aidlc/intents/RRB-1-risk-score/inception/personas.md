# Personas — INT-1

| Persona | Quem | Precisa | Acesso |
|---|---|---|---|
| **Analista de Crédito** | Time de Crédito | Ver score, faixa, motivos e histórico de uma conta; registrar KYC | Internet banking, escopos `credit:read` + `credit:write` |
| **Gestor de Política de Crédito** | Diretoria de Crédito | Parâmetros (pesos, faixas, taxas) versionados e rastreáveis; observar o score em shadow | Config Server (via Engenharia), leitura `credit:read` |
| **Motor de liberação de crédito** | Sistema interno (fora do repo) | Consultar último score + fatores na decisão, rápido | API via gateway, credencial de serviço `credit:read` |
| **Operador da conta** | Usuário atual do internet banking | Seguir criando contas e depositando sem mudança | Escopos atuais; **sem** acesso a score |
