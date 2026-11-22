# Roteiro do Vídeo Individual (Item 15) e Guia de Apresentação Semestral (Até 20 Minutos)

**Projeto:** FinFamily — Sistema de Gestão Financeira e Controle de Investimentos Familiar  
**Disciplina:** Tópicos Avançados em Programação — UCSAL  
**Professor:** Msc. Elton Figueiredo da Silva  
**Equipe:** Matheus Ramos de Oliveira, Alexandre Villarino e Silva, Eduardo Rocha Santos, Daniel Teixeira Melo, Marcos Vinicius Sousa Batista, Vinicius Freitas de Oliveira Souza  
**Papéis Scrum:** ScrumMaster — Matheus Ramos de Oliveira; Desenvolvedores — Alexandre Villarino e Silva, Daniel Teixeira Melo, Eduardo Rocha Santos, Marcos Vinicius Sousa Batista e Vinicius Freitas de Oliveira Souza  
**Gestão do projeto (Trello):** https://trello.com/b/EwMCmp64  

---

## Parte 1: Regras Críticas da Apresentação em Sala (Roteiro do Prof. Elton)

1. **Tempo Limite Estrito (Até 20 Minutos):**
   - A apresentação inteira deve durar entre **16 e 18 minutos** (deixando 2 minutos de margem de segurança).
   - **Atenção à penalidade:** A cada 20 segundos ultrapassados além de 20 minutos, o time perde **0,3 ponto de forma acumulativa**.
2. **Sorteio dos Apresentadores e Papel do ScrumMaster:**
   - No dia da apresentação, **até dois DEVs do time serão definidos por sorteio** para apresentar.
   - **Regra do ScrumMaster:** O ScrumMaster **não pode apresentar**, **não pode tirar dúvidas do time** no momento da apresentação e **não pode responder à pergunta** realizada pelo ScrumMaster selecionado. Portanto, todos os DEVs devem dominar o código e o sistema completo!
3. **Critérios de Avaliação (80% Professor + 20% ScrumMasters):**
   - Apresentação visual e fluida do sistema funcionando;
   - Domínio conceitual e técnico da arquitetura (Modelo C4, Spring Boot, React, Banco de Dados, IA);
   - Resposta precisa à pergunta realizada após a apresentação.

---

## Parte 2: Roteiro para o Vídeo Explicativo Individual (Item 15 da II Unidade — 4 a 6 minutos)

Cada integrante deve gravar seu vídeo individual (OBS Studio, Loom ou gravador nativo do Windows `Win + G`) seguindo esta estrutura:

### 1. Introdução e Contexto (0:00 – 0:45)
- *"Olá, sou [Seu Nome], integrante da equipe do projeto FinFamily na disciplina de Tópicos Avançados em Programação, orientada pelo Prof. Elton Figueiredo na UCSAL."*
- *"O FinFamily resolve o problema da dispersão das informações financeiras familiares — contas em múltiplos bancos, cartões de crédito com diferentes vencimentos, planilhas abandonadas e investimentos sem consolidação."*

### 2. Arquitetura e Modelo C4 (0:45 – 1:45)
- Mostrar rapidamente os 4 diagramas em `docs/c4/`:
  - **Nível 1 (Contexto):** 4 perfis (`Titular`, `Membro`, `Auditor` e `Sysadmin`), o núcleo `FinFamily` e a integração HTTP com o Serviço de IA.
  - **Nível 2 (Contêineres):** Arquitetura em 3 camadas — Front-End SPA em **React 18 + Vite + TailwindCSS**, Back-End em **Java 21 + Spring Boot 3.3.5** e Banco Relacional **H2 / PostgreSQL**.
  - **Nível 3 (Componentes):** Controllers REST (`AuthController`, `FinFamilyController`), Serviços de Negócio (`FinanceiroService`, `AssistenteIAService`, `ExportacaoBackupService`), Segurança (`JwtAuthenticationFilter`, `AesAttributeConverter`) e 12 interfaces `JpaRepository`.
  - **Nível 4 (Código):** As 12 classes de domínio implementadas fielmente ao Diagrama de Classes da Unidade I (`Usuario`, `GrupoFamiliar`, `Membro`, `Conta`, `Transacao`, `Categoria`, `Tag`, `Orcamento`, `Ativo`, `OperacaoInvestimento`, `Notificacao` e `Comando`).

### 3. Demonstração do Sistema Funcionando — Front-End + Back-End (1:45 – 4:00)
1. **Assistente Conversacional com IA (RF-012 e RN-006):**
   - Na tela inicial, clicar ou digitar: `"Registrar gasto de R$ 45,90 na padaria hoje no cartão de crédito"`.
   - Mostrar que o sistema **não grava direto**: ele exibe o **Card de Confirmação Editável (RN-006)** e informa que nenhum saldo ou CPF foi enviado à IA (**RNF-004**). Clicar em **"Confirmar e Gravar Lançamento"**.
   - Em seguida, perguntar: `"Quanto eu já gastei com Uber este mês?"` e mostrar que o próprio FinFamily filtra e calcula o total no banco local.
   - Clicar no botão **"Testar 50 Comandos IA (Meta OE5)"** e mostrar os **50 comandos (25 registros + 25 consultas)** atingindo **100% de acerto** (acima da meta de 80%) com tempo médio de 610 ms (abaixo do limite de 3 segundos): 24 comandos interpretados pelo Google Gemini e 26 pelo motor local de contingência, acionado quando o serviço externo excedeu o tempo ou o limite de uso do plano gratuito.
2. **Contas, Privacidade e Exclusão (RF-002, OE1, RN-001, RN-002):**
   - Mostrar os 4 tipos de conta (`CORRENTE`, `POUPANCA`, `CARTAO_CREDITO`, `CORRETORA`).
   - Mostrar o `Cartão Universitário do Filho (Privado)` que possui a flag `Privada` (**RN-001**) e não entra no Patrimônio Líquido Global da família.
   - Clicar em **"Testar Bloqueio RN-002"** em uma conta com saldo diferente de zero para mostrar o bloqueio da regra de negócio.
3. **Importação de Extrato CSV sem Duplicidade (RF-005 e Meta OE2):**
   - Na aba **Importar CSV**, clicar em **"Validar CSV e Descartar Duplicadas"**, revisar as categorias sugeridas e confirmar. Clicar uma segunda vez para provar que gera **0 duplicidades (OE2)**.
4. **Orçamentos (RF-006 / RN-004), Investimentos com Proventos (RF-008 / RN-005) e Exportação (RF-010):**
   - Mostrar as barras de orçamento com os alertas de **80% (Alimentação)** e **100% (Transporte)**.
   - Na aba **Investimentos**, lançar um `PROVENTO` de `PETR4` e mostrar que ele gera automaticamente uma `RECEITA` na conta de destino (**RN-005**).
   - Na aba **Relatórios & Exportar**, baixar o relatório em **PDF**, **Excel (.xlsx)** e **CSV**.

### 4. Explicação do Código Back-End em Java (4:00 – 5:00)
- Abrir no IDE (IntelliJ / VS Code) os arquivos:
  - `Conta.java` (métodos `podeSerExcluida()` da RN-002 e `gerarHashConciliacao()` da OE2);
  - `Transacao.java` (método `gerarParcelas()` da RN-003);
  - `Orcamento.java` (método `verificarAlerta()` da RN-004);
  - `AssistenteIAService.java` (métodos `interpretar()` e `executar()` da RN-006);
  - `AesAttributeConverter.java` (criptografia AES-256-GCM da RNF-003).

---

## Parte 3: Banco de Perguntas e Respostas para a Arguição do Professor e ScrumMasters

1. **Pergunta: Como o sistema garante que importar o mesmo arquivo CSV duas vezes não duplica lançamentos (Meta OE2)?**
   - **Resposta:** Na classe `Conta.java` (`gerarHashConciliacao`), cada linha do CSV gera um hash criptográfico **SHA-256** determinístico combinando `contaId + data + valorNormalizado + descricaoNormalizada`. Antes de exibir para revisão e antes de persistir no `FinanceiroService`, o `TransacaoRepository.existsByHashConciliacao(hash)` verifica se o hash já existe no banco, descartando 100% das duplicatas.
2. **Pergunta: Como funciona a integração com Inteligência Artificial (RF-012) e como a privacidade (RNF-004) e a segurança contra gravações indevidas (RN-006) são garantidas?**
   - **Resposta:** No `AssistenteIAService`, o método `montarPromptSeguroRnf004()` envia apenas o texto digitado/falado, a data atual e os nomes das contas e categorias — jamais envia saldos, transações ou CPF (**RNF-004**). Além disso, pela **RN-006**, o método `interpretar()` é `readOnly` e apenas devolve o objeto `Comando` para montar o card na tela; a gravação no banco só ocorre quando o usuário clica em confirmar, acionando `executar(..., confirmado=true)`.
3. **Pergunta: Como foram implementadas as Regras de Negócio RN-001, RN-002, RN-003, RN-004 e RN-005 no Back-End?**
   - **RN-001 (Privacidade):** Em `GrupoFamiliar.patrimonioLiquido()` e `FinanceiroService.obterDashboardConsolidado()`, filtramos `.filter(c -> !Boolean.TRUE.equals(c.getPrivada()))`.
   - **RN-002 (Exclusão de Conta):** Em `Conta.podeSerExcluida()`, verificamos `saldo.compareTo(BigDecimal.ZERO) == 0`. Se for diferente de zero, `FinanceiroService.excluirConta()` lança `IllegalStateException` (HTTP 422).
   - **RN-003 (Parcelamento):** Em `Transacao.gerarParcelas()`, dividimos o valor total pelo número de parcelas com `RoundingMode.DOWN`, ajustamos a diferença de centavos na 1ª parcela para soma exata e projetamos as datas futuras com `data.plusMonths(i - 1)`.
   - **RN-004 (Alertas 80% e 100%):** Em `Orcamento.verificarAlerta()`, calculamos `percentualGasto()` e criamos objetos `Notificacao` dos tipos `ORCAMENTO_80` e `ORCAMENTO_100`.
   - **RN-005 (Proventos):** Em `FinanceiroService.registrarOperacaoInvestimento()`, quando `tipo == TipoOperacao.PROVENTO`, o serviço invoca automaticamente `registrarTransacao(..., TipoTransacao.RECEITA, ...)` na conta de destino e vincula a transação gerada à operação.
