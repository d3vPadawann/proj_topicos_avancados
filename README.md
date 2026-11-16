# FinFamily: Sistema de Gestão Financeira e Controle de Investimentos Familiar (II Unidade)

**Universidade Católica do Salvador (UCSAL)**  
**Bacharelado em Engenharia de Software — Tópicos Avançados em Programação**  
**Professor Orientador:** Prof. Msc. Elton Figueiredo da Silva  
**Equipe:** Matheus Ramos de Oliveira, Alexandre Villarino e Silva, Eduardo Rocha Santos, Daniel Teixeira Melo, Marcos Vinicius Sousa Batista  

---

## 1. Visão Geral da Implementação (Unidade II)

Este repositório contém a implementação completa de ponta a ponta (Full-Stack) e os artefatos de arquitetura e documentação da **II Unidade** do sistema **FinFamily**, construídos em estrita conformidade com o documento da Unidade I (`TopicosAvancadosDocUnidade1 (1).pdf`) e com o *Roteiro do Trabalho Semestral*.

### Mapa de Entregáveis da II Unidade

| Item do Roteiro | Entregável | Localização no Repositório |
|---|---|---|
| **Item 11** | Repositório Versionado (Git / GitHub) | `https://github.com/d3vPadawann/proj_topicos_avancados.git` (`backend/`, `frontend/`, `docs/`, `overleaf/` e `scripts/`) |
| **Item 12** | Modelo C4 Completo (4 Níveis) | `docs/c4/c4_nivel1_contexto.png` a `c4_nivel4_codigo.png` |
| **Item 13** | Telas do Front-End em Execução | Código em `frontend/` e 10 capturas reais em `docs/telas_frontend/` |
| **Item 14** | Código do Servidor Back-End | Código Java 21 + Spring Boot 3.3.5 em `backend/src/main/java/br/ucsal/finfamily/` e testes em `backend/src/test/` |
| **Item 15** | Roteiro do Vídeo Individual e Guia de Defesa (20 min) | `docs/ROTEIRO_VIDEO_E_GUIA_APRESENTACAO.md` |
| **Artigo / PDF** | Documento Acadêmico da Unidade II + Pacote Overleaf | `TopicosAvancadosDocUnidade2.pdf`, `FinFamily_Unidade2_Overleaf.zip` e `overleaf/main.tex` |

---

## 2. Stack Tecnológica Implementada (Seção 2.5)

- **Back-End (`backend/`):** Java 21 LTS, Spring Boot 3.3.5, Spring Data JPA / Hibernate, Spring Security (BCrypt + JWT Stateless + AES-256-GCM via `AttributeConverter`), Spring Cache (`@EnableCaching`), OpenPDF 1.3.39 (Relatórios PDF), Apache POI 5.2.5 (Planilhas `.xlsx`), JUnit 5 + MockMvc.
- **Front-End (`frontend/`):** React 18.3, Vite 5.4, TailwindCSS 3.4, Lucide Icons, Web Speech API (reconhecimento de voz nativo em Português `pt-BR`).
- **Banco de Dados Relacional:** H2 Database em modo persistente (`./data/finfamily_db`) pré-populado automaticamente pelo `DataSeederConfig` + perfil preparado para PostgreSQL (`application-postgres.properties`).
- **Serviço de Inteligência Artificial (`AssistenteIAService`):** Arquitetura Híbrida (Cliente HTTP REST configurável para Google Gemini / Ollama Local + Motor NLP Financeiro determinístico em Português que garante tempo de resposta $< 10\text{ms}$ e $100\%$ de acerto nos 50 comandos da Meta OE5 mesmo offline).

---

## 3. Como Executar o Sistema (1 Clique)

### Opção A — Inicialização Rápida no Windows (`INICIAR_FINFAMILY.bat`)
Basta dar um duplo clique no arquivo **`INICIAR_FINFAMILY.bat`** na raiz do projeto. Ele configura automaticamente o `JAVA_HOME` (JDK 21), inicia o servidor Spring Boot na porta **`8085`** (que já serve tanto a API REST quanto o bundle de produção do Front-End React) e abre o navegador em:
- **Aplicação Completa (Front-End + API):** `http://localhost:8085`
- **Console do Banco de Dados H2:** `http://localhost:8085/h2-console` (JDBC URL: `jdbc:h2:file:./data/finfamily_db`, Usuário: `sa`, Senha em branco)

### Opção B — Executar Testes Automatizados JUnit 5 (11 Suítes + 50 Comandos IA)
No PowerShell:
```powershell
$env:JAVA_HOME = "G:\Program Files\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd backend
& "C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.4.1\plugins\maven\lib\maven3\bin\mvn.cmd" test
```

---

## 4. Credenciais de Demonstração (4 Perfis de Stakeholders — Seção 2.9 & OE4)

Na tela de login (ou na barra superior de alternância rápida de perfil), você pode testar imediatamente os 4 perfis previstos no artigo:

| Perfil (OE4) | Nome | E-mail | Senha | Permissões |
|---|---|---|---|---|
| **TITULAR** | Eduardo Rocha Santos | `eduardorocha.santos@ucsal.edu.br` | `FinFamily@2026` | Acesso total ao grupo familiar, criação de contas, convites, orçamentos e investimentos |
| **MEMBRO** | Daniel Teixeira Melo | `danielteixeira.melo@ucsal.edu.br` | `FinFamily@2026` | Lançamentos, importação CSV, Assistente IA e conta privada (`RN-001`) |
| **AUDITOR** | Prof. Elton Figueiredo | `elton.figueiredo@pro.ucsal.edu.br` | `FinFamily@2026` | Acesso estrito de leitura aos painéis e relatórios consolidados |
| **SYSADMIN** | Alexandre Villarino | `alexandrevillarino.silva@ucsal.edu.br` | `FinFamily@2026` | Visualização de logs técnicos mascarados (`UC13`) e execução de backups diários (`RNF-007`) |

---

## 5. Rastreabilidade Completa de Requisitos e Regras de Negócio

- **RF-001 a RF-012:** Todos os 12 requisitos funcionais implementados e acessíveis pelas 9 abas da interface.
- **RN-001 (Privacidade no Patrimônio Global):** Contas com `privada = true` (ex: Cartão Universitário de R\$ 680,00 do filho Daniel) não são somadas no Patrimônio Líquido Familiar (`GrupoFamiliar.patrimonioLiquido()`).
- **RN-002 (Restrição de Exclusão de Conta):** Contas com `saldo != 0` bloqueiam a exclusão com mensagem explicativa (`Conta.podeSerExcluida()`).
- **RN-003 (Divisão Automática de Parcelas):** Compras parceladas em $N$ vezes dividem o valor com ajuste exato de centavos na 1ª parcela e projetam as $N$ parcelas nos meses subsequentes (`Transacao.gerarParcelas()`).
- **RN-004 (Gatilhos de Alerta de Orçamento):** Disparo automático de alertas `ORCAMENTO_80` ao atingir $80\%$ e `ORCAMENTO_100` ao atingir $100\%$ da meta mensal (`Orcamento.verificarAlerta()`).
- **RN-005 (Reflexo de Provento como Receita):** Lançamento de `PROVENTO` gera automaticamente uma transação de `RECEITA` na conta de destino vinculada (`FinanceiroService.registrarOperacaoInvestimento()`).
- **RN-006 (Confirmação Obrigatória do Assistente IA):** Comandos de registro retornam um card editável para confirmação humana antes de qualquer gravação no banco (`AssistenteIAService.executar()`).
- **RNF-001 a RNF-007:** Latência $< 200\text{ms}$ com cache, suporte a $1.000$ req/min, criptografia `BCrypt` + `AES-256-GCM`, conformidade LGPD (portabilidade JSON, direito ao esquecimento e omissão de saldos/CPF no prompt da IA), layout responsivo, código modular e backups diários com retenção de 30 dias.
