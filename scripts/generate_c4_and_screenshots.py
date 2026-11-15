import os
import time
import shutil
import subprocess
import requests

BASE_DIR = r"c:\Users\eduar\projeto elton\PROJETO 2 UNIDADE"
C4_DIR = os.path.join(BASE_DIR, "docs", "c4")
SCREENSHOTS_DIR = os.path.join(BASE_DIR, "docs", "telas_frontend")
OVERLEAF_FIG_DIR = os.path.join(BASE_DIR, "overleaf", "figuras")
CHROME_PATH = r"C:\Program Files\Google\Chrome\Application\chrome.exe"

os.makedirs(C4_DIR, exist_ok=True)
os.makedirs(SCREENSHOTS_DIR, exist_ok=True)
os.makedirs(OVERLEAF_FIG_DIR, exist_ok=True)

COMMON_C4_CSS = """
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
  font-family: 'Segoe UI', Inter, Roboto, Arial, sans-serif;
  background: #ffffff;
  color: #1e293b;
  width: 1400px;
  height: 860px;
  padding: 24px 32px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.header {
  border-bottom: 2px solid #cbd5e1;
  padding-bottom: 10px;
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.header h1 { font-size: 21px; color: #0f172a; font-weight: 800; }
.header span { font-size: 13px; color: #475569; font-weight: 600; }
.canvas {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-around;
  padding: 14px 0;
}
.row {
  display: flex;
  justify-content: space-around;
  align-items: center;
  gap: 18px;
}
.box {
  border-radius: 10px;
  padding: 12px 15px;
  text-align: center;
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.08);
  width: 270px;
}
.person {
  background: #08427b;
  color: #ffffff;
  border: 2px solid #052e56;
}
.system-main {
  background: #1168bd;
  color: #ffffff;
  border: 3px solid #0b4884;
  width: 390px;
}
.container-box {
  background: #2b7cd3;
  color: #ffffff;
  border: 2px solid #1b5ca3;
}
.db-box {
  background: #1d4ed8;
  color: #ffffff;
  border: 2px solid #1e3a8a;
  border-radius: 18px;
}
.ext-system {
  background: #64748b;
  color: #ffffff;
  border: 2px dashed #334155;
}
.component-box {
  background: #eff6ff;
  color: #0f172a;
  border: 2px solid #3b82f6;
  width: 245px;
}
.code-box {
  background: #f8fafc;
  color: #0f172a;
  border: 1.5px solid #475569;
  text-align: left;
  font-size: 11px;
  width: 255px;
  padding: 9px 11px;
}
.code-box .cls-title {
  font-weight: 800;
  font-size: 12.5px;
  color: #1e3a8a;
  border-bottom: 1px solid #cbd5e1;
  padding-bottom: 4px;
  margin-bottom: 5px;
  text-align: center;
}
.tag {
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.6px;
  opacity: 0.9;
  margin-bottom: 4px;
  display: block;
  font-weight: 700;
}
.title { font-size: 14.5px; font-weight: 800; margin-bottom: 5px; }
.desc { font-size: 11.5px; line-height: 1.35; opacity: 0.95; }
.arrow-row {
  text-align: center;
  font-size: 11.5px;
  font-weight: 700;
  color: #334155;
  padding: 2px 0;
}
.boundary {
  border: 2.5px dashed #475569;
  border-radius: 14px;
  padding: 16px;
  background: #f8fafc;
}
.boundary-title {
  font-size: 13px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 10px;
  text-transform: uppercase;
}
.legend {
  border-top: 1px solid #e2e8f0;
  padding-top: 8px;
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #64748b;
}
"""

def build_c4_html_1():
    return f"""<!DOCTYPE html><html lang="pt-BR"><head><meta charset="utf-8"><style>{COMMON_C4_CSS}</style></head><body>
    <div class="header">
      <h1>Modelo C4 — Nível 1: Diagrama de Contexto do Sistema (System Context)</h1>
      <span>FinFamily • Engenharia de Software (UCSAL)</span>
    </div>
    <div class="canvas">
      <div class="row">
        <div class="box person">
          <span class="tag">[Person • Stakeholder]</span>
          <div class="title">Administrador Familiar (Titular)</div>
          <div class="desc">Cria o grupo familiar, gerencia contas, define orçamentos mensais, registra investimentos e convida membros (RF-002, RF-006, RF-007, RF-008).</div>
        </div>
        <div class="box person">
          <span class="tag">[Person • Stakeholder]</span>
          <div class="title">Membro Familiar (Dependente)</div>
          <div class="desc">Lança receitas/despesas, importa extratos CSV, utiliza o Assistente IA e acompanha relatórios e contas privadas (RF-003, RF-005, RF-012, RN-001).</div>
        </div>
        <div class="box person">
          <span class="tag">[Person • Stakeholder]</span>
          <div class="title">Auditor / Consultor Financeiro</div>
          <div class="desc">Planejador externo com acesso estrito de leitura aos dashboards e relatórios consolidados para consultoria (RF-009, OE4).</div>
        </div>
        <div class="box person">
          <span class="tag">[Person • Stakeholder]</span>
          <div class="title">Administrador do Sistema (Sysadmin)</div>
          <div class="desc">Equipe técnica responsável por monitorar logs de segurança e rotinas de backup diário sem ver saldos descriptografados (UC13, RNF-007).</div>
        </div>
      </div>

      <div class="arrow-row">
        ▼ HTTPS / TLS 1.2+ (JSON REST API • Autenticação JWT + Comandos em Texto/Voz) ▼
      </div>

      <div class="row">
        <div class="box system-main">
          <span class="tag">[Software System]</span>
          <div class="title">Sistema FinFamily (Gestão Financeira e Investimentos Familiar)</div>
          <div class="desc">Centraliza contas correntes, poupanças, cartões de crédito e corretoras; controla orçamentos com alertas em 80% e 100%; concilia extratos CSV sem duplicidade; acompanha carteira de ativos/proventos e integra Assistente Conversacional com IA.</div>
        </div>
      </div>

      <div class="arrow-row">
        ▼ Requisição HTTP JSON (Apenas texto do comando + nomes de contas/categorias — RNF-004) &nbsp;&nbsp;|&nbsp;&nbsp; Importação/Exportação de Arquivos ▼
      </div>

      <div class="row">
        <div class="box ext-system">
          <span class="tag">[External System • HTTP API]</span>
          <div class="title">Serviço de Inteligência Artificial (LLM API / Motor NLP Híbrido)</div>
          <div class="desc">Interpreta comandos em linguagem natural em até 3s devolvendo JSON estruturado (intenção registrar ou consultar) sem receber saldos ou CPF (RF-012, RNF-004).</div>
        </div>
        <div class="box ext-system">
          <span class="tag">[External Artifact • File System]</span>
          <div class="title">Instituições Bancárias (Extratos CSV) & Contabilidade (PDF / XLSX / CSV)</div>
          <div class="desc">Fornecem arquivos de extrato CSV para conciliação automática (RF-005) e recebem relatórios exportados pelo FinFamily (RF-010).</div>
        </div>
      </div>
    </div>
    <div class="legend">
      <span>Notação: Modelo C4 (Simon Brown) — Nível 1: Contexto</span>
      <span>Requisitos Cobertos: RF-001 a RF-012 • RN-001 a RN-006 • RNF-001 a RNF-007</span>
    </div>
    </body></html>"""

def build_c4_html_2():
    return f"""<!DOCTYPE html><html lang="pt-BR"><head><meta charset="utf-8"><style>{COMMON_C4_CSS}</style></head><body>
    <div class="header">
      <h1>Modelo C4 — Nível 2: Diagrama de Contêineres (Container Diagram)</h1>
      <span>Arquitetura em 3 Camadas (Seção 2.5) • Java 21 + Spring Boot 3 + React + H2/PostgreSQL</span>
    </div>
    <div class="canvas">
      <div class="row">
        <div class="box person" style="width:320px;">
          <span class="tag">[Usuários do Núcleo Familiar & Stakeholders]</span>
          <div class="title">Titular, Membro, Auditor e Sysadmin</div>
          <div class="desc">Acessam a interface responsiva (Mobile-First a Ultrawide — RNF-005) pelo navegador do computador ou smartphone.</div>
        </div>
      </div>

      <div class="arrow-row">▼ HTTPS / Navegador Web + Web Speech API (Voz para Texto) ▼</div>

      <div class="boundary">
        <div class="boundary-title"> Fronteira do Sistema FinFamily (Aplicação Monolítica Modular em 3 Camadas)</div>
        <div class="row" style="margin-bottom: 16px;">
          <div class="box container-box" style="width: 420px;">
            <span class="tag">[Container: Single-Page Application • React 18 + Vite + TailwindCSS]</span>
            <div class="title">Interface Web Responsiva (Front-End SPA)</div>
            <div class="desc">Entrega o Painel de Dashboards interativos, Caixa do Assistente Conversacional com IA (com card de confirmação RN-006), gestão de contas, transações, revisão de CSV, orçamentos e investimentos.</div>
          </div>
          <div style="font-weight:800; font-size:12px; color:#1e3a8a;">
            ── Chamadas API REST JSON ──►<br/>
            (Bearer JWT + Criptografia)
          </div>
          <div class="box container-box" style="width: 460px; background:#0f4c81;">
            <span class="tag">[Container: Application Server • Java 21 + Spring Boot 3.3.5]</span>
            <div class="title">Servidor de Aplicação e API REST (Back-End)</div>
            <div class="desc">Concentra todas as regras de negócio (RN-001 a RN-006), autenticação Spring Security + BCrypt + JWT, criptografia AES-256 (RNF-003), cache em memória (@EnableCaching RNF-002), motor de conciliação CSV e agendador @Scheduled.</div>
          </div>
        </div>

        <div class="row">
          <div class="box db-box" style="width: 420px;">
            <span class="tag">[Container: Relational Database • H2 Persistente / PostgreSQL]</span>
            <div class="title">Banco de Dados Relacional (Spring Data JPA / Hibernate)</div>
            <div class="desc">Armazena usuários, grupos familiares, membros, contas (com dados sensíveis em AES-256), árvore de categorias, tags, transações, orçamentos, ativos, operações e logs técnicos.</div>
          </div>
          <div class="box db-box" style="width: 420px; background:#065f46; border-color:#064e3b;">
            <span class="tag">[Container: Disaster Recovery Storage • JSON Snapshots]</span>
            <div class="title">Repositório de Backups Diários (RNF-007) & Exportador OpenPDF / POI</div>
            <div class="desc">Armazena backups automáticos diários com retenção mínima de 30 dias e gera relatórios binários em PDF, Excel (.xlsx) e CSV (RF-010).</div>
          </div>
        </div>
      </div>

      <div class="arrow-row">▼ Requisição HTTP POST (Timeout &lt; 3s — Seção 11.3) ▼</div>

      <div class="row">
        <div class="box ext-system" style="width: 520px;">
          <span class="tag">[External System: LLM Service • HTTP JSON]</span>
          <div class="title">Serviço Externo de IA (Google Gemini API / Ollama Local)</div>
          <div class="desc">Recebe exclusivamente o texto do comando, a data atual e os nomes das contas/categorias para extrair a intenção em JSON.</div>
        </div>
      </div>
    </div>
    <div class="legend">
      <span>Notação: Modelo C4 — Nível 2: Contêineres</span>
      <span>Tecnologias: Java 21 LTS, Spring Boot 3.3.5, React 18, TailwindCSS, H2/PostgreSQL, OpenPDF, Apache POI</span>
    </div>
    </body></html>"""

def build_c4_html_3():
    return f"""<!DOCTYPE html><html lang="pt-BR"><head><meta charset="utf-8"><style>{COMMON_C4_CSS}</style></head><body>
    <div class="header">
      <h1>Modelo C4 — Nível 3: Diagrama de Componentes do Servidor de Aplicação (Back-End)</h1>
      <span>Pacotes br.ucsal.finfamily.controller, .service, .security e .repository</span>
    </div>
    <div class="canvas">
      <div class="boundary">
        <div class="boundary-title">Camada de Apresentação REST & Filtro de Segurança (Controllers & Security)</div>
        <div class="row">
          <div class="box component-box">
            <span class="tag">[RestController]</span>
            <div class="title">AuthController</div>
            <div class="desc">Endpoints /api/auth/login, /register, portabilidade LGPD e direito ao esquecimento (RF-001, RNF-004).</div>
          </div>
          <div class="box component-box">
            <span class="tag">[RestController]</span>
            <div class="title">FinFamilyController</div>
            <div class="desc">Endpoints de Contas, Transações, CSV, Orçamentos, Grupo, Investimentos, IA, Exportação e Sysadmin (RF-002 a RF-012).</div>
          </div>
          <div class="box component-box">
            <span class="tag">[Security Component]</span>
            <div class="title">JwtAuthenticationFilter & JwtTokenProvider</div>
            <div class="desc">Autenticação Stateless JWT e controle de perfil Titular, Membro, Auditor e Sysadmin (OE4).</div>
          </div>
          <div class="box component-box">
            <span class="tag">[JPA AttributeConverter]</span>
            <div class="title">AesAttributeConverter</div>
            <div class="desc">Criptografia simétrica AES-256-GCM transparente para documentos e códigos de conta no banco (RNF-003).</div>
          </div>
        </div>
      </div>

      <div class="arrow-row">▼ Invocação Transacional de Regras de Negócio (@Transactional) ▼</div>

      <div class="boundary">
        <div class="boundary-title">Camada de Serviços e Regras de Negócio (Domain Services)</div>
        <div class="row">
          <div class="box component-box" style="background:#eef2ff; border-color:#4f46e5;">
            <span class="tag">[Service • Core Business]</span>
            <div class="title">FinanceiroService</div>
            <div class="desc">Implementa RN-001 (privacidade), RN-002 (exclusão saldo 0), RN-003 (parcelamento), RN-004 (alertas 80%/100%), RN-005 (proventos) e conciliação CSV SHA-256.</div>
          </div>
          <div class="box component-box" style="background:#eef2ff; border-color:#4f46e5;">
            <span class="tag">[Service • AI & NLP]</span>
            <div class="title">AssistenteIAService</div>
            <div class="desc">Implementa interpretar() e executar() (Figura 5), aplica RN-006 (confirmação do card), protege dados RNF-004 e executa os 50 testes da Meta OE5.</div>
          </div>
          <div class="box component-box" style="background:#eef2ff; border-color:#4f46e5;">
            <span class="tag">[Service • Reports & DR]</span>
            <div class="title">ExportacaoBackupService</div>
            <div class="desc">Gera relatórios em PDF (OpenPDF), Excel (Apache POI) e CSV (RF-010) e executa backup diário @Scheduled com retenção de 30 dias (RNF-007).</div>
          </div>
          <div class="box component-box" style="background:#eef2ff; border-color:#4f46e5;">
            <span class="tag">[Service • Security & Auth]</span>
            <div class="title">AuthService & AuditLogService</div>
            <div class="desc">Gerencia credenciais BCrypt, pacotes de portabilidade LGPD e trilha de auditoria mascarada para o Sysadmin (UC13).</div>
          </div>
        </div>
      </div>

      <div class="arrow-row">▼ Persistência Objeto-Relacional (Spring Data JPA Repositories) ▼</div>

      <div class="row">
        <div class="box db-box" style="width: 95%;">
          <span class="tag">[Data Access Layer • 12 Interfaces JpaRepository]</span>
          <div class="title">UsuarioRepository • GrupoFamiliarRepository • MembroRepository • ContaRepository • CategoriaRepository • TagRepository • TransacaoRepository • OrcamentoRepository • AtivoRepository • OperacaoInvestimentoRepository • NotificacaoRepository • AuditLogRepository</div>
        </div>
      </div>
    </div>
    <div class="legend">
      <span>Notação: Modelo C4 — Nível 3: Componentes</span>
      <span>Arquitetura Limpa e Modular em Spring Boot 3.3.5</span>
    </div>
    </body></html>"""

def build_c4_html_4():
    return f"""<!DOCTYPE html><html lang="pt-BR"><head><meta charset="utf-8"><style>{COMMON_C4_CSS}</style></head><body>
    <div class="header">
      <h1>Modelo C4 — Nível 4: Diagrama de Código e Classes de Domínio Implementadas (Code Diagram)</h1>
      <span>Mapeamento Fiel à Figura 5 da Unidade I (Pacote br.ucsal.finfamily.domain.model)</span>
    </div>
    <div class="canvas">
      <div class="row">
        <div class="box code-box">
          <div class="cls-title">«entity» Usuario</div>
          <div>- id: Long | nome: String</div>
          <div>- email: String | senhaHash: String</div>
          <div>- documentoSensivel: String [AES-256]</div>
          <div>- ativo: Boolean | papelPrincipal: PapelFamiliar</div>
          <hr style="margin:4px 0"/>
          <div>+ autenticar(email, senhaOk): Boolean</div>
        </div>
        <div class="box code-box">
          <div class="cls-title">«entity» GrupoFamiliar</div>
          <div>- id: Long | nome: String</div>
          <div>- titular: Usuario</div>
          <div>- membros: List&lt;Membro&gt;</div>
          <div>- contas: List&lt;Conta&gt;</div>
          <hr style="margin:4px 0"/>
          <div>+ convidar(email, papel): Membro</div>
          <div>+ patrimonioLiquido(): BigDecimal [RN-001]</div>
        </div>
        <div class="box code-box">
          <div class="cls-title">«entity» Membro</div>
          <div>- id: Long | emailConvidado: String</div>
          <div>- papel: PapelFamiliar [TITULAR|MEMBRO|AUDITOR]</div>
          <div>- status: StatusMembro [CONVIDADO|ATIVO|SUSPENSO]</div>
          <hr style="margin:4px 0"/>
          <div>+ aceitarConvite(): void</div>
          <div>+ recusarConvite() / suspender(): void</div>
        </div>
        <div class="box code-box">
          <div class="cls-title">«entity» Conta</div>
          <div>- id: Long | nome: String</div>
          <div>- tipo: TipoConta [4 tipos OE1]</div>
          <div>- saldo: BigDecimal | privada: Boolean</div>
          <div>- diaVencimento: Integer</div>
          <hr style="margin:4px 0"/>
          <div>+ podeSerExcluida(): Boolean [RN-002]</div>
          <div>+ importarCSV(lista, hashes): int [RF-005]</div>
        </div>
      </div>

      <div class="row">
        <div class="box code-box">
          <div class="cls-title">«entity» Transacao</div>
          <div>- id: Long | descricao: String</div>
          <div>- valor: BigDecimal | data: LocalDate</div>
          <div>- tipo: TipoTransacao | parcela: Integer</div>
          <div>- totalParcelas: Integer | recorrente: Boolean</div>
          <div>- hashConciliacao: String [SHA-256 OE2]</div>
          <hr style="margin:4px 0"/>
          <div>+ gerarParcelas(): List&lt;Transacao&gt; [RN-003]</div>
        </div>
        <div class="box code-box">
          <div class="cls-title">«entity» Categoria & Tag</div>
          <div>- id: Long | nome: String</div>
          <div>- pai: Categoria (Árvore RF-004)</div>
          <div>- filhas: List&lt;Categoria&gt;</div>
          <div>- tags: Set&lt;Tag&gt;</div>
          <hr style="margin:4px 0"/>
          <div>+ subcategorias(): List&lt;Categoria&gt;</div>
          <div>+ getCaminhoCompleto(): String</div>
        </div>
        <div class="box code-box">
          <div class="cls-title">«entity» Orcamento</div>
          <div>- id: Long | mes: String (YearMonth)</div>
          <div>- valorPlanejado: BigDecimal</div>
          <div>- valorGasto: BigDecimal</div>
          <div>- alerta80Disparado / alerta100Disparado</div>
          <hr style="margin:4px 0"/>
          <div>+ percentualGasto(): BigDecimal</div>
          <div>+ verificarAlerta(): List&lt;Notificacao&gt; [RN-004]</div>
        </div>
        <div class="box code-box">
          <div class="cls-title">«entity» Notificacao</div>
          <div>- id: Long | titulo: String</div>
          <div>- mensagem: String</div>
          <div>- tipo: TipoNotificacao</div>
          <div>- lida: Boolean | criadaEm: LocalDateTime</div>
          <hr style="margin:4px 0"/>
          <div>+ marcarComoLida(): void [RF-011]</div>
        </div>
      </div>

      <div class="row">
        <div class="box code-box">
          <div class="cls-title">«entity» Ativo</div>
          <div>- ticker: String [PK - ex: PETR4, HGLG11]</div>
          <div>- nome: String | setor: String</div>
          <div>- precoAtual: BigDecimal</div>
          <div>- tipoAtivo: String [ACAO|FII|ETF|RENDA_FIXA]</div>
        </div>
        <div class="box code-box">
          <div class="cls-title">«entity» OperacaoInvestimento</div>
          <div>- id: Long | tipo: TipoOperacao</div>
          <div>- quantidade: BigDecimal | preco: BigDecimal</div>
          <div>- data: LocalDate | ativo: Ativo</div>
          <div>- transacaoGerada: Transacao [RN-005]</div>
          <hr style="margin:4px 0"/>
          <div>+ valorTotal(): BigDecimal [RF-008]</div>
        </div>
        <div class="box code-box">
          <div class="cls-title">«value object» Comando</div>
          <div>- intencao: String [registrar | consultar]</div>
          <div>- valor: BigDecimal | descricao: String</div>
          <div>- data: LocalDate | conta: String</div>
          <div>- categoria: String | periodo: String</div>
          <div>- requerConfirmacao: Boolean [RN-006]</div>
        </div>
        <div class="box code-box">
          <div class="cls-title">«service» AssistenteIAService</div>
          <div>- urlApi: String | timeoutMs: 2800</div>
          <hr style="margin:4px 0"/>
          <div>+ interpretar(grupoId, texto): Comando</div>
          <div>+ executar(grupoId, cmd, confirmado): Map</div>
          <div>+ executarBateria50ComandosTeste(): Map</div>
        </div>
      </div>
    </div>
    <div class="legend">
      <span>Notação: Modelo C4 — Nível 4: Código / Classes UML Implementadas</span>
      <span>100% de Rastreabilidade com o Diagrama de Classes da Unidade I (Figura 5)</span>
    </div>
    </body></html>"""

def render_html_to_png(html_str, output_png, width=1400, height=860):
    temp_html = output_png.replace(".png", ".html")
    with open(temp_html, "w", encoding="utf-8") as f:
        f.write(html_str)
    cmd = [
        CHROME_PATH,
        "--headless",
        "--disable-gpu",
        f"--window-size={width},{height}",
        "--hide-scrollbars",
        f"--screenshot={os.path.abspath(output_png)}",
        os.path.abspath(temp_html)
    ]
    subprocess.run(cmd, check=True, capture_output=True)
    print(f"Created C4 diagram: {output_png} ({os.path.getsize(output_png)} bytes)")
    shutil.copy2(output_png, os.path.join(OVERLEAF_FIG_DIR, os.path.basename(output_png)))

def capture_url_screenshot(url, output_png, width=1440, height=920):
    cmd = [
        CHROME_PATH,
        "--headless",
        "--disable-gpu",
        f"--window-size={width},{height}",
        "--hide-scrollbars",
        "--virtual-time-budget=4500",
        "--run-all-compositor-stages-before-draw",
        f"--screenshot={os.path.abspath(output_png)}",
        url
    ]
    subprocess.run(cmd, check=True, capture_output=True)
    print(f"Captured frontend screenshot: {output_png} ({os.path.getsize(output_png)} bytes)")
    shutil.copy2(output_png, os.path.join(OVERLEAF_FIG_DIR, os.path.basename(output_png)))

def main():
    # 1. Generate 4 C4 Diagrams
    render_html_to_png(build_c4_html_1(), os.path.join(C4_DIR, "c4_nivel1_contexto.png"))
    render_html_to_png(build_c4_html_2(), os.path.join(C4_DIR, "c4_nivel2_conteineres.png"))
    render_html_to_png(build_c4_html_3(), os.path.join(C4_DIR, "c4_nivel3_componentes.png"))
    render_html_to_png(build_c4_html_4(), os.path.join(C4_DIR, "c4_nivel4_codigo.png"))

    # 2. Wait for Spring Boot server on http://localhost:8085
    for _ in range(20):
        try:
            r = requests.get("http://localhost:8085/api/health", timeout=2)
            if r.status_code == 200:
                print("Spring Boot Server is UP:", r.json())
                break
        except Exception:
            time.sleep(1)

    # 3. Capture real screenshots of all screens of the running Frontend (Item 13 of Unidade II)
    screens = [
        ("http://localhost:8085/?login=true", "tela_01_login.png"),
        ("http://localhost:8085/?tab=dashboard&cardia=true", "tela_02_dashboard_ia.png"),
        ("http://localhost:8085/?tab=dashboard&benchmark=true", "tela_03_benchmark_ia_50.png"),
        ("http://localhost:8085/?tab=contas", "tela_04_contas_cartoes.png"),
        ("http://localhost:8085/?tab=transacoes", "tela_05_transacoes_parcelamento.png"),
        ("http://localhost:8085/?tab=importacao", "tela_06_importacao_csv.png"),
        ("http://localhost:8085/?tab=orcamentos", "tela_07_orcamentos_alertas.png"),
        ("http://localhost:8085/?tab=investimentos", "tela_08_investimentos_proventos.png"),
        ("http://localhost:8085/?tab=grupo", "tela_09_grupo_familiar.png"),
        ("http://localhost:8085/?tab=seguranca", "tela_10_seguranca_lgpd_sysadmin.png"),
    ]
    for url, filename in screens:
        capture_url_screenshot(url, os.path.join(SCREENSHOTS_DIR, filename))

if __name__ == "__main__":
    main()
