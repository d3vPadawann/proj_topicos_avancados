import os
import zipfile
import subprocess
import base64

BASE_DIR = r"c:\Users\eduar\projeto elton\PROJETO 2 UNIDADE"
OVERLEAF_DIR = os.path.join(BASE_DIR, "overleaf")
FIG_DIR = os.path.join(OVERLEAF_DIR, "figuras")
CHROME_PATH = r"C:\Program Files\Google\Chrome\Application\chrome.exe"

def img_b64(filename):
    path = os.path.join(FIG_DIR, filename)
    with open(path, "rb") as f:
        data = base64.b64encode(f.read()).decode("ascii")
    return f"data:image/png;base64,{data}"

def create_overleaf_zip():
    zip_path = os.path.join(BASE_DIR, "FinFamily_Unidade2_Overleaf.zip")
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.write(os.path.join(OVERLEAF_DIR, "main.tex"), "main.tex")
        for fname in os.listdir(FIG_DIR):
            if fname.endswith(".png") and (fname.startswith("logo_") or fname.startswith("c4_") or fname.startswith("tela_")):
                zf.write(os.path.join(FIG_DIR, fname), f"figuras/{fname}")
    print(f"Created Overleaf ZIP: {zip_path} ({os.path.getsize(zip_path)} bytes)")

def build_academic_pdf():
    html_path = os.path.join(OVERLEAF_DIR, "TopicosAvancadosDocUnidade2.html")
    pdf_path = os.path.join(BASE_DIR, "TopicosAvancadosDocUnidade2.pdf")

    html = f"""<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<title>FinFamily - Documentação Completa da Unidade II (UCSAL)</title>
<style>
  @page {{
    size: A4;
    margin: 22mm 20mm 22mm 20mm;
  }}
  body {{
    font-family: "Latin Modern Roman", "Computer Modern", Georgia, "Times New Roman", serif;
    font-size: 11.5pt;
    line-height: 1.55;
    color: #111111;
    margin: 0;
    padding: 0;
    text-align: justify;
  }}
  .titlepage {{
    text-align: center;
    page-break-after: always;
    padding-top: 10px;
  }}
  .logo {{
    width: 175px;
    margin-bottom: 24px;
  }}
  .doc-title {{
    font-size: 19pt;
    font-weight: bold;
    line-height: 1.35;
    margin: 18px 0 22px 0;
  }}
  .authors {{
    font-size: 12pt;
    line-height: 1.65;
    margin-bottom: 18px;
  }}
  .affil {{
    font-size: 10.5pt;
    line-height: 1.45;
    color: #222;
    margin-bottom: 18px;
  }}
  .abstract-box {{
    text-align: justify;
    margin: 18px 24px;
    font-size: 10.5pt;
  }}
  .abstract-title {{
    text-align: center;
    font-weight: bold;
    font-size: 12pt;
    margin-bottom: 8px;
  }}
  h1 {{
    font-size: 14.5pt;
    font-weight: bold;
    margin-top: 22px;
    margin-bottom: 10px;
    text-align: left;
    page-break-after: avoid;
  }}
  h2 {{
    font-size: 12.5pt;
    font-weight: bold;
    margin-top: 16px;
    margin-bottom: 8px;
    text-align: left;
    page-break-after: avoid;
  }}
  p {{
    margin: 0 0 10px 0;
    text-indent: 1.25cm;
  }}
  p.noindent {{
    text-indent: 0;
  }}
  ul, ol {{
    margin: 6px 0 12px 24px;
  }}
  li {{
    margin-bottom: 4px;
  }}
  .figure {{
    text-align: center;
    margin: 14px 0 18px 0;
    page-break-inside: avoid;
  }}
  .figure img {{
    max-width: 100%;
    border: 1px solid #cbd5e1;
    border-radius: 4px;
  }}
  .caption {{
    font-size: 10pt;
    margin-top: 6px;
    text-align: center;
    font-style: italic;
  }}
  table {{
    width: 100%;
    border-collapse: collapse;
    margin: 12px 0 18px 0;
    font-size: 9.8pt;
    page-break-inside: avoid;
  }}
  th, td {{
    border-top: 1px solid #333;
    border-bottom: 1px solid #ccc;
    padding: 6px 8px;
    text-align: left;
    vertical-align: top;
  }}
  th {{
    border-top: 2px solid #000;
    border-bottom: 1.5px solid #000;
    font-weight: bold;
    background: #f8fafc;
  }}
  tr:last-child td {{
    border-bottom: 2px solid #000;
  }}
  pre {{
    font-family: Consolas, "Courier New", monospace;
    font-size: 8.2pt;
    line-height: 1.38;
    background: #f8fafc;
    border: 1px solid #cbd5e1;
    padding: 8px 10px;
    border-radius: 4px;
    white-space: pre-wrap;
    word-wrap: break-word;
    text-align: left;
    margin: 8px 0 12px 0;
    page-break-inside: avoid;
  }}
</style>
</head>
<body>

<div class="titlepage">
  <img class="logo" src="{img_b64('logo_ucsal.png')}" alt="UCSAL Logo"/>
  <div class="doc-title">
    FinFamily: Sistema de Gestão Financeira e Controle de Investimentos Familiar — Implementação da Unidade II
  </div>
  <div class="authors">
    <strong>Matheus Ramos de Oliveira</strong><sup>*1</sup>,
    <strong>Alexandre Villarino e Silva</strong><sup>†1</sup>,<br/>
    <strong>Eduardo Rocha Santos</strong><sup>‡1</sup>,
    <strong>Daniel Teixeira Melo</strong><sup>§1</sup>,
    <strong>Marcos Vinicius Sousa Batista</strong><sup>¶1</sup>,
    <strong>Elton Figueiredo da Silva</strong><sup>‖1*</sup>
  </div>
  <div class="affil">
    <sup>1</sup>Bacharelado em Engenharia de Software — Escola de Tecnologias<br/>
    Universidade Católica do Salvador (UCSAL)<br/>
    Av. Prof. Pinto de Aguiar, 2589 Pituaçu, CEP: 41740-090 — Salvador/BA, Brasil<br/>
    <i>{{matheusramos.oliveira, eduardorocha.santos, danielteixeira.melo, marcos.batista, alexandrevillarino.silva}}@ucsal.edu.br</i><br/>
    <i>elton.figueiredo@pro.ucsal.edu.br</i><br/><br/>
    Novembro 2026
  </div>

  <div class="abstract-box">
    <div class="abstract-title">Resumo</div>
    <p class="noindent">
      Este documento apresenta os artefatos de implementação da Unidade II do projeto <strong>FinFamily</strong>, desenvolvido na disciplina de Tópicos Avançados em Programação da Universidade Católica do Salvador (UCSAL). Seguindo estritamente o Roteiro do Trabalho Semestral e a modelagem da Unidade I, o sistema foi implementado em arquitetura de três camadas utilizando <strong>Java 21 LTS</strong> com <strong>Spring Boot 3.3.5</strong> no Back-End, <strong>React 18</strong>, <strong>Vite</strong> e <strong>TailwindCSS</strong> no Front-End e banco de dados relacional <strong>H2 Database / PostgreSQL</strong>. São apresentados: o repositório Git versionado (Item 11), os quatro níveis do <strong>Modelo C4</strong> — Contexto, Contêineres, Componentes e Código (Item 12), as telas reais do Front-End em execução (Item 13), o código-fonte do servidor Back-End (Item 14) e a validação automatizada dos 12 requisitos funcionais (RF-001 a RF-012), das 6 regras de negócio (RN-001 a RN-006), dos 7 requisitos não funcionais (RNF-001 a RNF-007) e da bateria de 50 comandos em linguagem natural do Assistente Conversacional com IA (meta OE5 superada com 100% de acerto e latência inferior a 10ms).
    </p>
    <p class="noindent" style="margin-top:8px;">
      <strong>Palavras-chaves:</strong> Gestão Financeira, Controle de Investimentos, Java, Spring Boot, React, Modelo C4, Inteligência Artificial.
    </p>
  </div>
</div>

<h1>1 &nbsp; Introdução e Entregáveis da Unidade II</h1>
<p>
  Na Unidade I foram definidos o escopo, o problema da fragmentação das informações financeiras familiares, os objetivos específicos mensuráveis (OE1 a OE6), os doze requisitos funcionais (RF-001 a RF-012), as seis regras de negócio (RN-001 a RN-006), os sete requisitos não funcionais (RNF-001 a RNF-007), os quatro fluxos de processos em BPMN 2.0, o diagrama de classes em UML e o diagrama de casos de uso.
</p>
<p>
  Na Unidade II, o sistema foi integralmente construído, testado e documentado para atender aos itens 11 a 15 do Roteiro do Trabalho Semestral: Repositório Versionado (Item 11), Modelo C4 Completo (Item 12), Telas do Front-End (Item 13), Código do Back-End (Item 14) e Roteiro do Vídeo Explicativo (Item 15).
</p>

<h1>2 &nbsp; Repositório Versionado e Arquitetura Tecnológica (Item 11)</h1>
<p>
  O repositório Git oficial encontra-se versionado no GitHub em <a href="https://github.com/d3vPadawann/proj_topicos_avancados.git">https://github.com/d3vPadawann/proj_topicos_avancados.git</a>, estruturado em módulos independentes (<code>backend/</code>, <code>frontend/</code>, <code>docs/c4/</code>, <code>docs/telas_frontend/</code> e <code>overleaf/</code>). Na abertura da Unidade II, conforme previsto na Seção 2.5 do artigo anterior, foi registrada a escolha final das tecnologias de implementação (Tabela 1).
</p>

<table>
  <caption>Tabela 1 – Stack tecnológica implementada na Unidade II</caption>
  <thead>
    <tr>
      <th>Camada</th>
      <th>Tecnologia Implementada</th>
      <th>Requisitos Atendidos</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Servidor Back-End</td>
      <td>Java 21 LTS + Spring Boot 3.3.5 + Spring Data JPA + Spring Security + Spring Cache</td>
      <td>RF-001 a RF-012; RN-001 a RN-006; RNF-001 a RNF-007</td>
    </tr>
    <tr>
      <td>Aplicação Front-End</td>
      <td>React 18.3 + Vite 5.4 + TailwindCSS 3.4 + Web Speech API</td>
      <td>RNF-005 (Mobile-First a Ultrawide); UC01 a UC13</td>
    </tr>
    <tr>
      <td>Banco de Dados Relacional</td>
      <td>H2 Database (persistente em arquivo + memória para testes) e suporte a PostgreSQL</td>
      <td>Persistência ACID de 12 entidades de domínio + AuditLog</td>
    </tr>
    <tr>
      <td>Serviço de IA (RF-012)</td>
      <td>Arquitetura Híbrida (Cliente HTTP LLM Gemini/Ollama + Motor NLP Financeiro Nativo)</td>
      <td>RF-012, RN-006 (confirmação em card), RNF-004 (privacidade) e OE5 (100% em 50 comandos)</td>
    </tr>
    <tr>
      <td>Relatórios e Exportação</td>
      <td>OpenPDF 1.3.39 (PDF), Apache POI 5.2.5 (Excel .xlsx) e Exportador CSV UTF-8</td>
      <td>RF-009 e RF-010 (Exportação em PDF, CSV e Excel)</td>
    </tr>
  </tbody>
</table>

<h1>3 &nbsp; Modelo C4: Contexto, Contêineres, Componentes e Código (Item 12)</h1>
<p>
  As Figuras 1 a 4 apresentam a arquitetura de software do FinFamily modelada nos quatro níveis do Modelo C4 (BROWN, 2018).
</p>

<h2>3.1 &nbsp; Nível 1: Diagrama de Contexto</h2>
<div class="figure">
  <img src="{img_b64('c4_nivel1_contexto.png')}" alt="C4 Nível 1 Contexto"/>
  <div class="caption">Figura 1 – Modelo C4 (Nível 1): Diagrama de Contexto do Sistema FinFamily</div>
</div>

<h2>3.2 &nbsp; Nível 2: Diagrama de Contêineres</h2>
<div class="figure">
  <img src="{img_b64('c4_nivel2_conteineres.png')}" alt="C4 Nível 2 Contêineres"/>
  <div class="caption">Figura 2 – Modelo C4 (Nível 2): Diagrama de Contêineres (Front-End SPA, API Spring Boot e Banco Relacional)</div>
</div>

<h2>3.3 &nbsp; Nível 3: Diagrama de Componentes</h2>
<div class="figure">
  <img src="{img_b64('c4_nivel3_componentes.png')}" alt="C4 Nível 3 Componentes"/>
  <div class="caption">Figura 3 – Modelo C4 (Nível 3): Diagrama de Componentes do Servidor de Aplicação Spring Boot</div>
</div>

<h2>3.4 &nbsp; Nível 4: Diagrama de Código e Classes de Domínio</h2>
<div class="figure">
  <img src="{img_b64('c4_nivel4_codigo.png')}" alt="C4 Nível 4 Código"/>
  <div class="caption">Figura 4 – Modelo C4 (Nível 4): Diagrama de Código mapeando as 12 classes de domínio e o serviço AssistenteIA</div>
</div>

<h1>4 &nbsp; Telas do Front-End em Execução (Item 13)</h1>
<p>
  As Figuras 5 a 14 apresentam as capturas de tela reais do Front-End React conectado ao Back-End Spring Boot, cobrindo todos os doze requisitos funcionais e as seis regras de negócio.
</p>

<div class="figure">
  <img src="{img_b64('tela_01_login.png')}" alt="Tela Login"/>
  <div class="caption">Figura 5 – Tela de Autenticação JWT/BCrypt e Seletor dos 4 Perfis de Stakeholders (RF-001, OE4)</div>
</div>

<div class="figure">
  <img src="{img_b64('tela_02_dashboard_ia.png')}" alt="Tela Dashboard e IA"/>
  <div class="caption">Figura 6 – Painel Consolidado e Assistente Conversacional com IA exibindo Card de Confirmação Obrigatória (RF-009, RF-012, RN-006, RN-001)</div>
</div>

<div class="figure">
  <img src="{img_b64('tela_03_benchmark_ia_50.png')}" alt="Tela Benchmark 50 Comandos IA"/>
  <div class="caption">Figura 7 – Validação da Meta OE5: Bateria de 50 Comandos de Teste do Assistente de IA com 100% de Acerto</div>
</div>

<div class="figure">
  <img src="{img_b64('tela_04_contas_cartoes.png')}" alt="Tela Contas e Cartões"/>
  <div class="caption">Figura 8 – Gestão de Contas e Cartões nos 4 Tipos (OE1), Conta Privada do Filho (RN-001) e Bloqueio de Exclusão (RN-002)</div>
</div>

<div class="figure">
  <img src="{img_b64('tela_05_transacoes_parcelamento.png')}" alt="Tela Transações"/>
  <div class="caption">Figura 9 – Lançamento de Transações com Parcelamento Automático (RN-003) e Recorrência (RF-003)</div>
</div>

<div class="figure">
  <img src="{img_b64('tela_06_importacao_csv.png')}" alt="Tela Importação CSV"/>
  <div class="caption">Figura 10 – Importação de Extratos CSV com Conciliação SHA-256 Anti-Duplicidade e Revisão (RF-005, OE2)</div>
</div>

<div class="figure">
  <img src="{img_b64('tela_07_orcamentos_alertas.png')}" alt="Tela Orçamentos"/>
  <div class="caption">Figura 11 – Definição de Orçamentos Mensais com Gatilhos de Alerta em 80% e 100% (RF-006, RN-004, OE3)</div>
</div>

<div class="figure">
  <img src="{img_b64('tela_08_investimentos_proventos.png')}" alt="Tela Investimentos"/>
  <div class="caption">Figura 12 – Controle de Investimentos por Ticker e Reflexo Automático de Proventos como Receita (RF-008, RN-005)</div>
</div>

<div class="figure">
  <img src="{img_b64('tela_09_grupo_familiar.png')}" alt="Tela Grupo Familiar"/>
  <div class="caption">Figura 13 – Gestão do Grupo Familiar: Convite, Aceite e Suspensão de Membros (RF-007, OE4)</div>
</div>

<div class="figure">
  <img src="{img_b64('tela_10_seguranca_lgpd_sysadmin.png')}" alt="Tela Segurança LGPD e Sysadmin"/>
  <div class="caption">Figura 14 – Painel de Portabilidade LGPD (RNF-004), Backup Diário (RNF-007) e Logs de Segurança Sysadmin (UC13)</div>
</div>

<h1>5 &nbsp; Código-Fonte do Servidor Back-End em Java (Item 14)</h1>
<p>
  O código do Back-End foi organizado nos pacotes <code>domain.enums</code>, <code>domain.model</code>, <code>repository</code>, <code>security</code>, <code>service</code>, <code>controller</code> e <code>config</code> dentro de <code>backend/src/main/java/br/ucsal/finfamily</code>. Os trechos a seguir destacam a implementação das regras de negócio RN-001 a RN-006 e da criptografia AES-256 (RNF-003).
</p>

<h2>5.1 &nbsp; Privacidade (RN-001), Exclusão de Conta (RN-002) e Conciliação CSV (OE2)</h2>
<pre>
// 1. RN-001 (Privacidade no Patrimonio Global) em GrupoFamiliar.java:
public BigDecimal patrimonioLiquido() {{
    return contas.stream()
            .filter(c -&gt; !Boolean.TRUE.equals(c.getPrivada()))
            .map(c -&gt; c.getSaldo() != null ? c.getSaldo() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}}

// 2. RN-002 (Restricao de Exclusao de Conta com Saldo != 0) e OE2 (Hash SHA-256 CSV) em Conta.java:
public Boolean podeSerExcluida() {{
    return this.saldo == null || this.saldo.compareTo(BigDecimal.ZERO) == 0;
}}

public static String gerarHashConciliacao(Long contaId, LocalDate data, BigDecimal valor, String descricao) {{
    String descNorm = descricao == null ? "" : descricao.trim().toLowerCase().replaceAll("\\s+", " ");
    String valorNorm = valor == null ? "0.00" : valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    String base = (contaId != null ? contaId : 0L) + "|" + data + "|" + valorNorm + "|" + descNorm;
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    return HexFormat.of().formatHex(md.digest(base.getBytes(StandardCharsets.UTF_8)));
}}
</pre>

<h2>5.2 &nbsp; Parcelamento Automático (RN-003) e Gatilhos de Orçamento (RN-004)</h2>
<pre>
// 3. RN-003 (Divisao Automatica de Parcelas e Projecao Futura) em Transacao.java:
public List&lt;Transacao&gt; gerarParcelas() {{
    BigDecimal valorBase = this.valor.divide(BigDecimal.valueOf(numParcelas), 2, RoundingMode.DOWN);
    BigDecimal difCentavos = this.valor.subtract(valorBase.multiply(BigDecimal.valueOf(numParcelas)));
    for (int i = 1; i &lt;= numParcelas; i++) {{
        Transacao p = new Transacao();
        p.setDescricao(this.descricao + " (" + i + "/" + numParcelas + ")");
        p.setValor(i == 1 ? valorBase.add(difCentavos) : valorBase);
        p.setData(this.data.plusMonths(i - 1));
        p.setParcela(i);
        p.setTotalParcelas(numParcelas);
        parcelasGeradas.add(p);
    }}
    return parcelasGeradas;
}}

// 4. RN-004 (Gatilhos de Alerta de Orcamento em 80% e 100%) em Orcamento.java:
public List&lt;Notificacao&gt; verificarAlerta() {{
    BigDecimal percentual = percentualGasto();
    if (percentual.compareTo(BigDecimal.valueOf(80)) &gt;= 0 &amp;&amp; !Boolean.TRUE.equals(this.alerta80Disparado)) {{
        this.alerta80Disparado = true;
        notificacoes.add(criarAlerta80(percentual));
    }}
    if (percentual.compareTo(BigDecimal.valueOf(100)) &gt;= 0 &amp;&amp; !Boolean.TRUE.equals(this.alerta100Disparado)) {{
        this.alerta100Disparado = true;
        notificacoes.add(criarAlerta100(percentual));
    }}
    return notificacoes;
}}
</pre>

<h2>5.3 &nbsp; Proventos (RN-005), Assistente IA (RN-006) e Criptografia AES-256 (RNF-003)</h2>
<pre>
// 5. RN-005 (Reflexo Automatico de Provento como Receita) em FinanceiroService.java:
if (tipo == TipoOperacao.PROVENTO) {{
    List&lt;Transacao&gt; geradas = registrarTransacao(
        grupo.getId(), "Provento / Dividendo " + ativo.getTicker(),
        op.valorTotal(), op.getData(), TipoTransacao.RECEITA,
        contaDestino.getId(), null, catProventos.getId(), null, 1, false, null, titularId, "PROVENTO"
    );
    op.setTransacaoGerada(geradas.get(0));
}}

// 6. RN-006 (Confirmacao Obrigatoria do Card do Assistente de IA) em AssistenteIAService.java:
if ("registrar".equalsIgnoreCase(comando.getIntencao())) {{
    if (!confirmadoPeloUsuario) {{
        throw new IllegalStateException("RN-006 Violada: Confirmacao explicita do card exigida.");
    }}
    return gravarLancamentoConfirmado(grupoId, comando, autorId);
}}
</pre>

<h1>6 &nbsp; Validação Automatizada das Metas (OE1 a OE6)</h1>
<p>
  A suíte de testes automatizados JUnit 5 (<code>FinFamilySystemTests</code> e <code>FinFamilyApiAndPerformanceTests</code>) validou todas as metas definidas na Tabela 2 da Unidade I, alcançando 100% de aprovação (Tabela 2).
</p>

<table>
  <caption>Tabela 2 – Comprovação das metas dos objetivos específicos na Unidade II</caption>
  <thead>
    <tr>
      <th>ID</th>
      <th>Objetivo Específico</th>
      <th>Meta Definida (Unidade I)</th>
      <th>Resultado Verificado nos Testes (Unidade II)</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>OE1</td>
      <td>Centralizar as contas da família em uma única tela.</td>
      <td>4 tipos (corrente, poupança, cartão e corretora).</td>
      <td><strong>100% Atendido:</strong> Os 4 tipos de conta foram implementados e testados.</td>
    </tr>
    <tr>
      <td>OE2</td>
      <td>Importar extratos sem duplicar lançamentos.</td>
      <td>0 duplicidades ao importar o mesmo arquivo duas vezes.</td>
      <td><strong>100% Atendido:</strong> 0 duplicidades verificadas via conciliação SHA-256.</td>
    </tr>
    <tr>
      <td>OE3</td>
      <td>Controlar o orçamento com alertas.</td>
      <td>Alertas gerados em 80% e 100% em 100% dos testes.</td>
      <td><strong>100% Atendido:</strong> Gatilhos <code>ORCAMENTO_80</code> e <code>ORCAMENTO_100</code> validados.</td>
    </tr>
    <tr>
      <td>OE4</td>
      <td>Permitir a colaboração da família com segurança.</td>
      <td>Perfis com permissões distintas (Titular, Membro, Auditor e Sysadmin).</td>
      <td><strong>100% Atendido:</strong> Controle de acesso e perfis implementados e testados.</td>
    </tr>
    <tr>
      <td>OE5</td>
      <td>Agilizar lançamentos e consultas com assistente IA.</td>
      <td>&ge; 80% em 50 comandos de teste e resposta &lt; 3s.</td>
      <td><strong>100% de Acerto (50/50 comandos)</strong> e tempo médio &lt; 10ms.</td>
    </tr>
    <tr>
      <td>OE6</td>
      <td>Entregar o produto mínimo viável (MVP).</td>
      <td>12 de 12 requisitos funcionais até o fim da Unidade II.</td>
      <td><strong>12 de 12 Requisitos Funcionais</strong> + 6 RNs + 7 RNFs implementados.</td>
    </tr>
  </tbody>
</table>

</body>
</html>"""

    with open(html_path, "w", encoding="utf-8") as f:
        f.write(html)

    cmd = [
        CHROME_PATH,
        "--headless",
        "--disable-gpu",
        "--no-pdf-header-footer",
        "--virtual-time-budget=6000",
        "--run-all-compositor-stages-before-draw",
        f"--print-to-pdf={os.path.abspath(pdf_path)}",
        os.path.abspath(html_path)
    ]
    subprocess.run(cmd, check=True, capture_output=True)
    print(f"Created Academic PDF: {pdf_path} ({os.path.getsize(pdf_path)} bytes)")

if __name__ == "__main__":
    create_overleaf_zip()
    build_academic_pdf()
