import React, { useState, useEffect, useMemo } from 'react';
import {
  Wallet,
  CreditCard,
  TrendingUp,
  ArrowUpRight,
  ArrowDownRight,
  PieChart,
  Users,
  Bell,
  Upload,
  FileSpreadsheet,
  FileText,
  Download,
  Bot,
  Mic,
  MicOff,
  CheckCircle2,
  AlertTriangle,
  ShieldCheck,
  Lock,
  Plus,
  Trash2,
  FolderTree,
  Tag as TagIcon,
  LogOut,
  UserCheck,
  Search,
  RefreshCw,
  ChevronRight,
  Sparkles,
  Check,
  X,
  Database,
  Eye,
  EyeOff,
  Calendar,
  Repeat,
  Layers,
  Award
} from 'lucide-react';
import { api } from './api';

const fmtBRL = (val) => {
  const n = Number(val || 0);
  return n.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
};

const EXTRATO_EXEMPLO_CSV = `data;descricao;valor;tipo
2026-11-10;Supermercado Pao de Acucar Salvador;-385.40;DESPESA
2026-11-11;Uber Viagem Campus UCSAL Pituacu;-29.90;DESPESA
2026-11-12;Padaria Delicatessen;-45.90;DESPESA
2026-11-13;Farmacia Santana;-92.50;DESPESA
2026-11-14;Pix Recebido Reembolso;-0.00;RECEITA
2026-11-14;Restaurante Senac Pelourinho;-128.00;DESPESA`;

const PERFIS_DEMO = [
  {
    email: 'eduardorocha.santos@ucsal.edu.br',
    nome: 'Eduardo Rocha Santos',
    papel: 'TITULAR',
    rotulo: 'Administrador Familiar (Titular)',
    cor: 'bg-blue-600 text-white',
    desc: 'Acesso total de leitura e escrita, gerencia contas, membros, orçamentos e investimentos.'
  },
  {
    email: 'danielteixeira.melo@ucsal.edu.br',
    nome: 'Daniel Teixeira Melo',
    papel: 'MEMBRO',
    rotulo: 'Membro Familiar (Filho Universitário)',
    cor: 'bg-emerald-600 text-white',
    desc: 'Registra transações, importa extratos, usa o Assistente IA e possui cartão privado (RN-001).'
  },
  {
    email: 'elton.figueiredo@pro.ucsal.edu.br',
    nome: 'Prof. Elton Figueiredo',
    papel: 'AUDITOR',
    rotulo: 'Auditor / Consultor Financeiro',
    cor: 'bg-amber-600 text-white',
    desc: 'Acesso estrito de leitura aos dashboards e relatórios para consultoria financeira.'
  },
  {
    email: 'alexandrevillarino.silva@ucsal.edu.br',
    nome: 'Alexandre Villarino',
    papel: 'SYSADMIN',
    rotulo: 'Administrador do Sistema (Sysadmin)',
    cor: 'bg-purple-700 text-white',
    desc: 'Equipe técnica responsável por logs de segurança (UC13) e backups (RNF-007), sem ver saldos.'
  }
];

// Mostra qual modelo de IA interpretou o comando (ex.: "GEMINI (gemini-3.5-flash-lite)")
function SeloModeloIA({ comando }) {
  if (!comando) return null;
  const provedor = String(comando.provedorIA || '');
  const contingencia = provedor.startsWith('CONTINGENCIA');
  const casamento = provedor.match(/^(\w+)\s*\((.+)\)$/);
  const servico = casamento ? casamento[1] : provedor;
  const modelo = casamento ? casamento[2] : '';
  return (
    <span className="flex flex-wrap items-center gap-1.5">
      <span
        className={`px-2 py-0.5 rounded-full text-[11px] font-semibold ${
          contingencia ? 'bg-slate-200 text-slate-700' : 'bg-indigo-100 text-indigo-800'
        }`}
      >
        {contingencia
          ? 'IA indisponível — motor local de contingência'
          : `IA: ${servico}${modelo ? ` • Modelo: ${modelo}` : ''}`}
      </span>
      {comando.tempoRespostaMs != null && (
        <span className="text-xs text-slate-500">em {comando.tempoRespostaMs}ms</span>
      )}
    </span>
  );
}

// Sessao do usuario guardada no navegador junto com o token JWT (RF-001)
function lerSessaoSalva() {
  try {
    const token = localStorage.getItem('finfamily_token');
    const dados = localStorage.getItem('finfamily_usuario');
    return token && dados ? JSON.parse(dados) : null;
  } catch {
    return null;
  }
}

function salvarSessao(resp) {
  try {
    localStorage.setItem('finfamily_token', resp.token);
    const { token, ...dadosUsuario } = resp;
    localStorage.setItem('finfamily_usuario', JSON.stringify(dadosUsuario));
  } catch {
    // navegador sem armazenamento: a sessao vale ate recarregar a pagina
  }
}

function apagarSessao() {
  try {
    localStorage.removeItem('finfamily_token');
    localStorage.removeItem('finfamily_usuario');
  } catch {
    // ignora
  }
}

export default function App() {
  // Sessao e Autenticacao (RF-001): sem login, nenhum dado e carregado
  const [usuario, setUsuario] = useState(lerSessaoSalva);
  const [mostrarTelaLogin, setMostrarTelaLogin] = useState(() => !lerSessaoSalva());
  const [modoCadastro, setModoCadastro] = useState(false);
  const [formAuth, setFormAuth] = useState({
    nome: '',
    email: '',
    senha: '',
    nomeGrupo: 'Família FinFamily UCSAL',
    documentoSensivel: '123.456.789-00'
  });

  // Navegacao entre Modulos
  const [abaAtiva, setAbaAtiva] = useState(() =>
    lerSessaoSalva()?.papel === 'SYSADMIN' ? 'seguranca' : 'dashboard'
  );
  const [carregando, setCarregando] = useState(false);
  const [feedback, setFeedback] = useState(null); // { tipo: 'sucesso' | 'erro' | 'aviso', texto: '' }

  // Estados de Dados do Backend
  const [dashboard, setDashboard] = useState(null);
  const [contas, setContas] = useState([]);
  const [categoriasArvore, setCategoriasArvore] = useState([]);
  const [categoriasPlanas, setCategoriasPlanas] = useState([]);
  const [tags, setTags] = useState([]);
  const [transacoes, setTransacoes] = useState([]);
  const [orcamentos, setOrcamentos] = useState([]);
  const [membros, setMembros] = useState([]);
  const [carteira, setCarteira] = useState(null);
  const [notificacoes, setNotificacoes] = useState([]);
  const [logsSysadmin, setLogsSysadmin] = useState([]);
  const [ultimoBackup, setUltimoBackup] = useState(null);
  const [pacoteLgpd, setPacoteLgpd] = useState(null);

  // Assistente Conversacional com IA (RF-012, RN-006, OE5)
  const [textoComandoIA, setTextoComandoIA] = useState('');
  const [ouvindoVoz, setOuvindoVoz] = useState(false);
  const [cardConfirmacaoIA, setCardConfirmacaoIA] = useState(null);
  const [resultadoConsultaIA, setResultadoConsultaIA] = useState(null);
  const [benchmark50, setBenchmark50] = useState(null);
  const [executandoBenchmark, setExecutandoBenchmark] = useState(false);

  // Filtros de Transacoes
  const [filtroTransacao, setFiltroTransacao] = useState({
    busca: '',
    contaId: '',
    categoriaId: '',
    tipo: '',
    periodo: 'mes_atual'
  });

  // Formularios dos Modulos
  const [novaConta, setNovaConta] = useState({
    nome: '',
    instituicao: '',
    tipo: 'CORRENTE',
    saldo: '0.00',
    limiteCredito: '0.00',
    diaVencimento: '10',
    privada: false,
    codigoIdentificadorSensivel: ''
  });

  const [novaTransacao, setNovaTransacao] = useState({
    descricao: '',
    valor: '',
    data: new Date().toISOString().slice(0, 10),
    tipo: 'DESPESA',
    contaId: '',
    categoriaId: '',
    totalParcelas: 1,
    recorrente: false,
    frequenciaRecorrencia: 'MENSAL'
  });

  // Importacao CSV (RF-005, BPMN Figura 1)
  const [csvContaId, setCsvContaId] = useState('');
  const [csvConteudo, setCsvConteudo] = useState(EXTRATO_EXEMPLO_CSV);
  const [csvAnalise, setCsvAnalise] = useState(null);
  const [csvUltimoResultado, setCsvUltimoResultado] = useState(null);

  // Categorias e Tags (RF-004)
  const [novaCategoria, setNovaCategoria] = useState({
    nome: '',
    cor: '#3B82F6',
    tipoPadrao: 'DESPESA',
    paiId: ''
  });
  const [novaTag, setNovaTag] = useState({ nome: '', cor: '#6366F1' });

  // Orcamentos (RF-006, RN-004)
  const [novoOrcamento, setNovoOrcamento] = useState({
    categoriaId: '',
    mes: new Date().toISOString().slice(0, 7),
    valorPlanejado: ''
  });

  // Grupo Familiar (RF-007)
  const [novoMembro, setNovoMembro] = useState({
    nome: '',
    email: '',
    papel: 'MEMBRO'
  });

  // Investimentos (RF-008, RN-005)
  const [novaOperacaoInv, setNovaOperacaoInv] = useState({
    ticker: 'PETR4',
    tipo: 'COMPRA',
    quantidade: '100',
    preco: '38.45',
    data: new Date().toISOString().slice(0, 10),
    contaCorretoraId: '',
    contaDestinoProventoId: '',
    nomeAtivo: ''
  });

  const [mostrarPrivadasNoCard, setMostrarPrivadasNoCard] = useState(false);

  const isSomenteLeituraAuditor = usuario?.papel === 'AUDITOR';
  const isSysadmin = usuario?.papel === 'SYSADMIN';
  const isTitular = usuario?.papel === 'TITULAR';

  const exibirMensagem = (tipo, texto) => {
    setFeedback({ tipo, texto });
    setTimeout(() => {
      setFeedback((atual) => (atual?.texto === texto ? null : atual));
    }, 6500);
  };

  const carregarTudo = async (papel = usuario?.papel) => {
    if (!papel) return;
    setCarregando(true);
    try {
      if (papel === 'SYSADMIN') {
        // O Sysadmin so acessa logs tecnicos e backup (UC13), nunca dados financeiros
        setLogsSysadmin(await api.getLogsSysadmin());
        return;
      }
      const [
        dashData,
        contasData,
        catArvData,
        catPlanData,
        tagsData,
        transData,
        orcData,
        membData,
        cartData,
        notifData
      ] = await Promise.all([
        api.getDashboard(1),
        api.getContas(1),
        api.getCategoriasArvore(1),
        api.getCategoriasPlanas(1),
        api.getTags(1),
        api.getTransacoes({ grupoId: 1, ...filtroTransacao }),
        api.getOrcamentos(1),
        api.getMembros(1),
        api.getCarteira(1),
        api.getNotificacoes(1)
      ]);

      setDashboard(dashData);
      setContas(contasData);
      setCategoriasArvore(catArvData);
      setCategoriasPlanas(catPlanData);
      setTags(tagsData);
      setTransacoes(transData);
      setOrcamentos(orcData);
      setMembros(membData);
      setCarteira(cartData);
      setNotificacoes(notifData);

      if (contasData.length > 0 && !novaTransacao.contaId) {
        setNovaTransacao((prev) => ({ ...prev, contaId: contasData[0].id }));
        setCsvContaId(String(contasData[0].id));
        const corretora = contasData.find((c) => c.tipo === 'CORRETORA') || contasData[0];
        setNovaOperacaoInv((prev) => ({
          ...prev,
          contaCorretoraId: corretora.id,
          contaDestinoProventoId: contasData[0].id
        }));
      }
      if (catPlanData.length > 0 && !novaTransacao.categoriaId) {
        setNovaTransacao((prev) => ({ ...prev, categoriaId: catPlanData[0].id }));
        setNovoOrcamento((prev) => ({ ...prev, categoriaId: catPlanData[0].id }));
      }
    } catch (err) {
      exibirMensagem('erro', 'Erro ao sincronizar dados com o servidor: ' + err.message);
    } finally {
      setCarregando(false);
    }
  };

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const tabParam = params.get('tab');
    if (tabParam) {
      setAbaAtiva(tabParam);
    }
    if (params.get('login') === 'true') {
      setMostrarTelaLogin(true);
    }
    const aoExpirar = () => encerrarSessao('Sua sessão expirou. Faça login novamente.');
    window.addEventListener('finfamily:sessao-expirada', aoExpirar);
    if (!usuario) {
      return () => window.removeEventListener('finfamily:sessao-expirada', aoExpirar);
    }
    carregarTudo().then(() => {
      if (params.get('cardia') === 'true') {
        const cmdPadrao = 'Registrar gasto de R$ 45,90 na padaria hoje no cartão de crédito';
        setTextoComandoIA(cmdPadrao);
        api.interpretarIA(cmdPadrao, 1).then((c) => setCardConfirmacaoIA(c)).catch(() => {});
      }
      if (params.get('benchmark') === 'true') {
        api.executarBenchmark50(1).then((b) => setBenchmark50(b)).catch(() => {});
      }
    });
    return () => window.removeEventListener('finfamily:sessao-expirada', aoExpirar);
  }, []);

  const encerrarSessao = (mensagem) => {
    apagarSessao();
    setUsuario(null);
    setModoCadastro(false);
    setFormAuth((f) => ({ ...f, senha: '' }));
    setMostrarTelaLogin(true);
    if (mensagem) exibirMensagem('aviso', mensagem);
  };

  const entrarComSessao = (resp, mensagem) => {
    salvarSessao(resp);
    setUsuario(resp);
    setMostrarTelaLogin(false);
    setFormAuth((f) => ({ ...f, senha: '' }));
    if (resp.papel === 'SYSADMIN') {
      setAbaAtiva('seguranca');
    } else if (abaAtiva === 'seguranca') {
      setAbaAtiva('dashboard');
    }
    exibirMensagem('sucesso', mensagem);
    carregarTudo(resp.papel);
  };

  // OE4: trocar de perfil exige autenticacao de verdade; o botao apenas preenche o e-mail
  const trocarPerfilDemo = (perfil) => {
    setModoCadastro(false);
    setFormAuth((f) => ({ ...f, email: perfil.email, senha: '' }));
    setMostrarTelaLogin(true);
    exibirMensagem('aviso', `Digite a senha para entrar como ${perfil.nome} (${perfil.papel}).`);
  };

  const baixarExportacao = async (formato) => {
    const nomes = { pdf: 'finfamily_relatorio.pdf', excel: 'finfamily_relatorio.xlsx', csv: 'finfamily_relatorio.csv' };
    try {
      await api.baixar(`/exportacao/${formato}`, nomes[formato]);
    } catch (err) {
      exibirMensagem('erro', err.message);
    }
  };

  const handleLoginOuCadastro = async (e) => {
    e.preventDefault();
    try {
      const resp = modoCadastro
        ? await api.register(formAuth)
        : await api.login(formAuth.email, formAuth.senha);
      entrarComSessao(resp, `Bem-vindo(a), ${resp.nome}! Autenticação JWT + BCrypt concluída (RF-001).`);
    } catch (err) {
      exibirMensagem('erro', err.message);
    }
  };

  // =========================================================================
  // HANDLERS DO ASSISTENTE CONVERSACIONAL COM IA (RF-012, RN-006, OE5)
  // =========================================================================

  const handleInterpretarComandoIA = async (e, comandoDireto) => {
    if (e) e.preventDefault();
    const texto = comandoDireto !== undefined ? comandoDireto : textoComandoIA;
    if (!texto || !texto.trim()) return;

    try {
      setCardConfirmacaoIA(null);
      setResultadoConsultaIA(null);
      const cmd = await api.interpretarIA(texto, 1);

      if (cmd.intencao === 'registrar') {
        // RN-006: Exibe o card preenchido para confirmacao obrigatoria do usuario!
        setCardConfirmacaoIA(cmd);
        exibirMensagem('aviso', 'RN-006: Confira os dados no card abaixo e clique em Confirmar para gravar o lançamento.');
      } else if (cmd.intencao === 'consultar') {
        // RN-006: Consultas apenas leem dados no banco local do FinFamily
        const resConsulta = await api.executarIA({ ...cmd, confirmado: true, grupoId: 1 });
        setResultadoConsultaIA({ comando: cmd, resposta: resConsulta });
      } else {
        setResultadoConsultaIA({
          comando: cmd,
          resposta: {
            mensagem: cmd.mensagemAssistente,
            quantidadeLancamentos: 0,
            valorTotalCalculado: 0,
            transacoesEncontradas: []
          }
        });
      }
    } catch (err) {
      exibirMensagem('erro', err.message);
    }
  };

  const handleConfirmarCardIA = async () => {
    if (!cardConfirmacaoIA) return;
    if (isSomenteLeituraAuditor) {
      exibirMensagem('erro', 'Perfil AUDITOR possui permissão estrita de leitura (OE4).');
      return;
    }
    try {
      const resp = await api.executarIA({
        ...cardConfirmacaoIA,
        confirmado: true,
        grupoId: 1,
        autorId: usuario?.usuarioId
      });
      setCardConfirmacaoIA(null);
      setTextoComandoIA('');
      exibirMensagem('sucesso', resp.mensagem);
      carregarTudo();
    } catch (err) {
      exibirMensagem('erro', err.message);
    }
  };

  const iniciarReconhecimentoVoz = () => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
      const exemploVoz = 'Registrar gasto de R$ 45,90 na padaria hoje no cartão de crédito';
      setTextoComandoIA(exemploVoz);
      exibirMensagem('aviso', 'Web Speech API simulada com comando de voz padrão: "' + exemploVoz + '"');
      handleInterpretarComandoIA(null, exemploVoz);
      return;
    }
    const recognition = new SpeechRecognition();
    recognition.lang = 'pt-BR';
    recognition.interimResults = false;
    setOuvindoVoz(true);
    recognition.onresult = (event) => {
      const transcricao = event.results[0][0].transcript;
      setTextoComandoIA(transcricao);
      setOuvindoVoz(false);
      handleInterpretarComandoIA(null, transcricao);
    };
    recognition.onerror = () => setOuvindoVoz(false);
    recognition.onend = () => setOuvindoVoz(false);
    recognition.start();
  };

  const handleExecutarBenchmark50 = async () => {
    setExecutandoBenchmark(true);
    try {
      const relatorio = await api.executarBenchmark50(1);
      setBenchmark50(relatorio);
      exibirMensagem(
        'sucesso',
        `Meta OE5 validada: ${relatorio.comandosCorretos}/${relatorio.totalComandosTestados} comandos corretos (${relatorio.taxaAcertoPercentual}%) em média de ${relatorio.tempoMedioMs}ms!`
      );
    } catch (err) {
      exibirMensagem('erro', err.message);
    } finally {
      setExecutandoBenchmark(false);
    }
  };

  // =========================================================================
  // HANDLERS DOS DEMAIS MODULOS
  // =========================================================================

  const handleCriarConta = async (e) => {
    e.preventDefault();
    if (!isTitular) {
      exibirMensagem('erro', 'Apenas o Administrador Familiar (Titular) pode criar ou excluir contas (UC02).');
      return;
    }
    try {
      await api.createConta({ ...novaConta, grupoId: 1, proprietarioId: usuario.usuarioId });
      setNovaConta({
        nome: '',
        instituicao: '',
        tipo: 'CORRENTE',
        saldo: '0.00',
        limiteCredito: '0.00',
        diaVencimento: '10',
        privada: false,
        codigoIdentificadorSensivel: ''
      });
      exibirMensagem('sucesso', 'Conta cadastrada com sucesso (RF-002 / RN-001)!');
      carregarTudo();
    } catch (err) {
      exibirMensagem('erro', err.message);
    }
  };

  const handleAlternarPrivacidadeConta = async (conta) => {
    try {
      await api.updateConta(conta.id, {
        ...conta,
        privada: !conta.privada,
        grupoId: 1
      });
      exibirMensagem(
        'sucesso',
        `RN-001: Conta '${conta.nome}' marcada como ${!conta.privada ? 'PRIVADA (oculta do Patrimônio Global)' : 'COMPARTILHADA (visível no Patrimônio Global)'}.`
      );
      carregarTudo();
    } catch (err) {
      exibirMensagem('erro', err.message);
    }
  };

  const handleExcluirConta = async (conta) => {
    try {
      await api.deleteConta(conta.id);
      exibirMensagem('sucesso', `Conta '${conta.nome}' excluída com sucesso (RN-002 atendida: saldo igual a zero).`);
      carregarTudo();
    } catch (err) {
      exibirMensagem('erro', err.message);
    }
  };

  const handleCriarTransacao = async (e) => {
    e.preventDefault();
    if (isSomenteLeituraAuditor) {
      exibirMensagem('erro', 'O perfil AUDITOR possui acesso estrito de leitura (OE4).');
      return;
    }
    try {
      const criadas = await api.createTransacao({
        ...novaTransacao,
        grupoId: 1,
        autorId: usuario.usuarioId
      });
      setNovaTransacao((prev) => ({ ...prev, descricao: '', valor: '', totalParcelas: 1, recorrente: false }));
      exibirMensagem(
        'sucesso',
        criadas.length > 1
          ? `RN-003: Despesa parcelada dividida automaticamente em ${criadas.length} lançamentos futuros!`
          : 'Lançamento financeiro registrado com sucesso (RF-003)!'
      );
      carregarTudo();
    } catch (err) {
      exibirMensagem('erro', err.message);
    }
  };

  const handleFiltrarTransacoes = async (novosFiltros) => {
    const atualizado = { ...filtroTransacao, ...novosFiltros };
    setFiltroTransacao(atualizado);
    try {
      const lista = await api.getTransacoes({ grupoId: 1, ...atualizado });
      setTransacoes(lista);
    } catch (err) {
      exibirMensagem('erro', err.message);
    }
  };

  const handleAnalisarCsv = async () => {
    try {
      const res = await api.analisarCsv({
        grupoId: 1,
        contaId: Number(csvContaId || (contas[0] && contas[0].id) || 1),
        conteudoCsv: csvConteudo
      });
      setCsvAnalise(res);
      setCsvUltimoResultado(null);
      exibirMensagem(
        'sucesso',
        `Extrato validado: ${res.quantidadeNovas} transações inéditas para revisão e ${res.quantidadeDuplicadasDescartadas} duplicadas descartadas (OE2)!`
      );
    } catch (err) {
      exibirMensagem('erro', err.message);
    }
  };

  const handleConfirmarCsv = async () => {
    if (!csvAnalise) return;
    try {
      const res = await api.confirmarCsv({
        grupoId: 1,
        contaId: csvAnalise.contaId,
        autorId: usuario.usuarioId,
        transacoes: csvAnalise.transacoesParaRevisao
      });
      setCsvUltimoResultado(res);
      setCsvAnalise(null);
      exibirMensagem(
        'sucesso',
        `Importação concluída: ${res.transacoesGravadas} transações gravadas com 0 duplicidades (OE2)!`
      );
      carregarTudo();
    } catch (err) {
      exibirMensagem('erro', err.message);
    }
  };

  const handleSalvarOrcamento = async (e) => {
    e.preventDefault();
    try {
      await api.saveOrcamento({
        grupoId: 1,
        categoriaId: Number(novoOrcamento.categoriaId),
        mes: novoOrcamento.mes,
        valorPlanejado: novoOrcamento.valorPlanejado
      });
      setNovoOrcamento((prev) => ({ ...prev, valorPlanejado: '' }));
      exibirMensagem('sucesso', 'Meta de orçamento mensal salva e gatilhos de 80% / 100% verificados (RF-006 / RN-004)!');
      carregarTudo();
    } catch (err) {
      exibirMensagem('erro', err.message);
    }
  };

  const handleRegistrarInvestimento = async (e) => {
    e.preventDefault();
    try {
      const op = await api.registrarOperacaoInvestimento({
        ...novaOperacaoInv,
        grupoId: 1
      });
      exibirMensagem(
        'sucesso',
        op.tipo === 'PROVENTO'
          ? `RN-005: Provento de ${op.ativo.ticker} (${fmtBRL(op.quantidade * op.preco)}) lançado e refletido automaticamente como RECEITA na conta de destino!`
          : `Operação de ${op.tipo} do ativo ${op.ativo.ticker} registrada e carteira recalculada (RF-008)!`
      );
      carregarTudo();
    } catch (err) {
      exibirMensagem('erro', err.message);
    }
  };

  const handleConvidarMembro = async (e) => {
    e.preventDefault();
    try {
      await api.convidarMembro({ ...novoMembro, grupoId: 1 });
      setNovoMembro({ nome: '', email: '', papel: 'MEMBRO' });
      exibirMensagem('sucesso', 'Convite enviado com status CONVIDADO conforme BPMN Figura 2 (RF-007)!');
      carregarTudo();
    } catch (err) {
      exibirMensagem('erro', err.message);
    }
  };

  const naoLidasCount = useMemo(
    () => notificacoes.filter((n) => !n.lida).length,
    [notificacoes]
  );

  // =========================================================================
  // RENDERIZACAO DA TELA DE LOGIN / CADASTRO (RF-001)
  // =========================================================================

  if (mostrarTelaLogin) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-950 to-slate-900 flex items-center justify-center p-4">
        <div className="max-w-4xl w-full grid md:grid-cols-2 bg-white rounded-2xl shadow-2xl overflow-hidden border border-slate-200">
          {/* Coluna Esquerda: Institucional FinFamily */}
          <div className="bg-gradient-to-br from-blue-700 to-indigo-900 text-white p-8 flex flex-col justify-between">
            <div>
              <div className="flex items-center gap-3 mb-6">
                <div className="w-11 h-11 rounded-xl bg-white/15 backdrop-blur flex items-center justify-center">
                  <Wallet className="w-6 h-6 text-white" />
                </div>
                <div>
                  <h1 className="text-2xl font-extrabold tracking-tight">FinFamily</h1>
                  <p className="text-xs text-blue-200">Gestão Financeira e Investimentos Familiar</p>
                </div>
              </div>

              <h2 className="text-xl font-bold mb-3 leading-snug">
                Centralize contas, cartões, orçamentos e investimentos da família em um só lugar.
              </h2>
              <p className="text-sm text-blue-100 leading-relaxed mb-6">
                Projeto da II Unidade de Tópicos Avançados de Programação — Engenharia de Software (UCSAL).
              </p>

              <div className="space-y-3 text-xs text-blue-100">
                <div className="flex items-center gap-2 bg-white/10 p-2.5 rounded-lg">
                  <Sparkles className="w-4 h-4 text-amber-300 shrink-0" />
                  <span><strong>RF-012 & RN-006:</strong> Assistente Conversacional com IA (Texto e Voz) com confirmação em card.</span>
                </div>
                <div className="flex items-center gap-2 bg-white/10 p-2.5 rounded-lg">
                  <ShieldCheck className="w-4 h-4 text-emerald-300 shrink-0" />
                  <span><strong>RNF-003 & RNF-004:</strong> Senhas em Hash BCrypt, dados sensíveis em AES-256 e conformidade LGPD.</span>
                </div>
              </div>
            </div>

            <div className="mt-6 pt-4 border-t border-white/15 text-xs text-blue-200">
              Acesso Rápido aos 4 Perfis de Stakeholders (Seção 2.9):
              <div className="grid grid-cols-2 gap-2 mt-2">
                {PERFIS_DEMO.map((p) => (
                  <button
                    key={p.papel}
                    onClick={() => trocarPerfilDemo(p)}
                    className="text-left px-2.5 py-2 rounded-lg bg-white/10 hover:bg-white/20 transition font-medium text-white"
                  >
                    <div className="text-[11px] font-bold">{p.papel}</div>
                    <div className="text-[10px] text-blue-200 truncate">{p.nome}</div>
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Coluna Direita: Formulario RF-001 */}
          <div className="p-8 flex flex-col justify-center">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-xl font-bold text-slate-900">
                {modoCadastro ? 'Criar Conta Familiar (RF-001)' : 'Autenticação no Sistema (RF-001)'}
              </h3>
              {usuario && (
                <button
                  onClick={() => setMostrarTelaLogin(false)}
                  className="text-xs text-slate-500 hover:text-slate-800 underline"
                >
                  Voltar ao Painel
                </button>
              )}
            </div>

            <form onSubmit={handleLoginOuCadastro} className="space-y-4">
              {modoCadastro && (
                <>
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Nome Completo</label>
                    <input
                      type="text"
                      required
                      value={formAuth.nome}
                      onChange={(e) => setFormAuth({ ...formAuth, nome: e.target.value })}
                      className="w-full px-3.5 py-2 rounded-lg border border-slate-300 text-sm"
                      placeholder="Ex: Eduardo Rocha Santos"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Nome do Grupo Familiar</label>
                    <input
                      type="text"
                      value={formAuth.nomeGrupo}
                      onChange={(e) => setFormAuth({ ...formAuth, nomeGrupo: e.target.value })}
                      className="w-full px-3.5 py-2 rounded-lg border border-slate-300 text-sm"
                      placeholder="Ex: Família Santos"
                    />
                  </div>
                </>
              )}

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">E-mail Institucional / Pessoal</label>
                <input
                  type="email"
                  required
                  value={formAuth.email}
                  onChange={(e) => setFormAuth({ ...formAuth, email: e.target.value })}
                  className="w-full px-3.5 py-2 rounded-lg border border-slate-300 text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Senha (armazenada em Hash BCrypt)</label>
                <input
                  type="password"
                  required
                  value={formAuth.senha}
                  onChange={(e) => setFormAuth({ ...formAuth, senha: e.target.value })}
                  className="w-full px-3.5 py-2 rounded-lg border border-slate-300 text-sm"
                />
              </div>

              <button
                type="submit"
                className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg text-sm shadow transition"
              >
                {modoCadastro ? 'Cadastrar e Iniciar Sessão JWT' : 'Entrar com E-mail e Senha'}
              </button>
            </form>

            <div className="mt-4 text-center">
              <button
                onClick={() => setModoCadastro(!modoCadastro)}
                className="text-xs text-blue-600 hover:underline font-medium"
              >
                {modoCadastro ? 'Já possui conta? Faça login' : 'Novo no FinFamily? Cadastre sua família'}
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // =========================================================================
  // LAYOUT PRINCIPAL RESPONSIVO (RNF-005: MOBILE-FIRST A ULTRAWIDE)
  // =========================================================================

  const itensMenu = [
    { id: 'dashboard', label: 'Painel & Assistente IA', icon: Bot, badge: 'RF-012' },
    { id: 'contas', label: 'Contas e Cartões', icon: CreditCard, badge: 'RF-002' },
    { id: 'transacoes', label: 'Transações', icon: ArrowUpRight, badge: 'RF-003' },
    { id: 'importacao', label: 'Importar CSV', icon: Upload, badge: 'RF-005' },
    { id: 'orcamentos', label: 'Orçamentos', icon: PieChart, badge: 'RF-006' },
    { id: 'investimentos', label: 'Investimentos', icon: TrendingUp, badge: 'RF-008' },
    { id: 'categorias', label: 'Categorias & Tags', icon: FolderTree, badge: 'RF-004' },
    { id: 'grupo', label: 'Grupo Familiar', icon: Users, badge: 'RF-007' },
    { id: 'relatorios', label: 'Relatórios & Exportar', icon: FileSpreadsheet, badge: 'RF-010' },
    { id: 'notificacoes', label: 'Alertas', icon: Bell, badge: naoLidasCount > 0 ? String(naoLidasCount) : 'RF-011' },
    { id: 'seguranca', label: 'LGPD & Sysadmin', icon: ShieldCheck, badge: 'UC13' }
  ].filter((item) => !isSysadmin || item.id === 'seguranca'); // Sysadmin nao acessa dados financeiros (Secao 2.9)

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      {/* Barra Superior Executiva */}
      <header className="bg-slate-900 text-white border-b border-slate-800 sticky top-0 z-30">
        <div className="max-w-[1600px] mx-auto px-4 py-2.5 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-blue-600 flex items-center justify-center shadow-inner">
              <Wallet className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-extrabold text-lg tracking-tight">FinFamily</span>
                <span className="text-[10px] uppercase tracking-wider px-2 py-0.5 rounded-full bg-blue-500/20 text-blue-300 border border-blue-400/30 font-semibold">
                  UCSAL • II Unidade
                </span>
              </div>
              <p className="text-[11px] text-slate-400 hidden sm:block">
                {dashboard?.nomeGrupo || 'Família FinFamily UCSAL'} • Visão Holística de Patrimônio e Investimentos
              </p>
            </div>
          </div>

          {/* Seletor Rapido de Perfis (OE4: Titular, Membro, Auditor, Sysadmin) */}
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-[11px] text-slate-400 hidden lg:inline">Alternar Perfil (OE4):</span>
            {PERFIS_DEMO.map((p) => {
              const ativo = usuario?.papel === p.papel;
              return (
                <button
                  key={p.papel}
                  onClick={() => trocarPerfilDemo(p)}
                  className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition flex items-center gap-1.5 ${
                    ativo
                      ? 'bg-blue-600 text-white shadow ring-2 ring-blue-400'
                      : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                  }`}
                  title={p.desc}
                >
                  <UserCheck className="w-3.5 h-3.5" />
                  <span>{p.papel}</span>
                </button>
              );
            })}

            <button
              onClick={() => setAbaAtiva('notificacoes')}
              className="relative p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200"
              title="Central de Alertas e Notificações (RF-011)"
            >
              <Bell className="w-4 h-4" />
              {naoLidasCount > 0 && (
                <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-rose-500 text-white text-[10px] font-bold flex items-center justify-center">
                  {naoLidasCount}
                </span>
              )}
            </button>

            <button
              onClick={() => encerrarSessao('Sessão encerrada.')}
              className="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs text-slate-200 flex items-center gap-1"
              title={usuario ? `Conectado como ${usuario.nome} (${usuario.papel})` : ''}
            >
              <LogOut className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Sair</span>
            </button>
          </div>
        </div>

        {/* Navegacao Horizontal de Modulos */}
        <div className="bg-slate-800/80 border-t border-slate-800">
          <div className="max-w-[1600px] mx-auto px-4 flex items-center gap-1 overflow-x-auto py-1.5">
            {itensMenu.map((item) => {
              const Icon = item.icon;
              const ativo = abaAtiva === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => setAbaAtiva(item.id)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap flex items-center gap-1.5 transition ${
                    ativo
                      ? 'bg-blue-600 text-white shadow-sm'
                      : 'text-slate-300 hover:bg-slate-700/70 hover:text-white'
                  }`}
                >
                  <Icon className="w-3.5 h-3.5" />
                  <span>{item.label}</span>
                  <span
                    className={`text-[10px] px-1.5 py-0.2 rounded ${
                      ativo ? 'bg-blue-800 text-blue-100' : 'bg-slate-700 text-slate-400'
                    }`}
                  >
                    {item.badge}
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      </header>

      {/* Banner de Feedback e Status de Perfil */}
      <div className="max-w-[1600px] w-full mx-auto px-4 pt-3">
        {isSomenteLeituraAuditor && (
          <div className="mb-3 bg-amber-50 border border-amber-300 text-amber-900 px-4 py-2.5 rounded-xl text-xs flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Eye className="w-4 h-4 text-amber-600 shrink-0" />
              <span>
                <strong>Modo Auditor / Consultor Financeiro Ativo (OE4 & Seção 2.9):</strong> Você possui acesso estrito de leitura aos dashboards e relatórios para consultoria. Operações de escrita estão bloqueadas.
              </span>
            </div>
          </div>
        )}

        {feedback && (
          <div
            className={`mb-3 px-4 py-3 rounded-xl text-xs font-medium flex items-center justify-between shadow-sm border ${
              feedback.tipo === 'erro'
                ? 'bg-rose-50 border-rose-300 text-rose-900'
                : feedback.tipo === 'aviso'
                ? 'bg-amber-50 border-amber-300 text-amber-900'
                : 'bg-emerald-50 border-emerald-300 text-emerald-900'
            }`}
          >
            <div className="flex items-center gap-2">
              {feedback.tipo === 'erro' ? (
                <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
              ) : (
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              )}
              <span>{feedback.texto}</span>
            </div>
            <button onClick={() => setFeedback(null)} className="text-slate-500 hover:text-slate-800">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>

      {/* Conteudo Principal por Aba */}
      <main className="max-w-[1600px] w-full mx-auto px-4 py-3 flex-1 space-y-5">
        {/* ===================================================================
            ABA 1: DASHBOARD PRINCIPAL + ASSISTENTE CONVERSACIONAL IA (RF-009, RF-012)
           =================================================================== */}
        {abaAtiva === 'dashboard' && (
          <>
            {/* Secao do Assistente Conversacional com IA (RF-012, RN-006, Secao 11) */}
            <section className="bg-gradient-to-r from-slate-900 via-blue-950 to-indigo-950 text-white rounded-2xl p-5 shadow-lg border border-blue-800/40">
              <div className="flex flex-wrap items-center justify-between gap-2 mb-3">
                <div className="flex items-center gap-2.5">
                  <div className="w-9 h-9 rounded-xl bg-blue-500/20 border border-blue-400/30 flex items-center justify-center">
                    <Bot className="w-5 h-5 text-blue-300" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h2 className="text-base font-bold">
                        Assistente Conversacional com Inteligência Artificial (RF-012)
                      </h2>
                      <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-400/30 font-semibold">
                        RN-006 & RNF-004 Ativos
                      </span>
                    </div>
                    <p className="text-xs text-blue-200">
                      Digite ou dite um comando para registrar gastos (com confirmação em card) ou consultar despesas em linguagem natural.
                    </p>
                  </div>
                </div>

                <button
                  onClick={handleExecutarBenchmark50}
                  disabled={executandoBenchmark}
                  className="px-3 py-1.5 rounded-lg bg-amber-500/20 hover:bg-amber-500/30 border border-amber-400/40 text-amber-200 text-xs font-semibold flex items-center gap-1.5 transition"
                >
                  <Award className="w-4 h-4 text-amber-300" />
                  <span>{executandoBenchmark ? 'Testando 50 Comandos...' : 'Testar 50 Comandos IA (Meta OE5)'}</span>
                </button>
              </div>

              <form onSubmit={(e) => handleInterpretarComandoIA(e)} className="flex flex-col sm:flex-row gap-2">
                <div className="relative flex-1">
                  <input
                    type="text"
                    value={textoComandoIA}
                    onChange={(e) => setTextoComandoIA(e.target.value)}
                    placeholder='Ex: "Registrar gasto de R$ 45,90 na padaria hoje no cartão de crédito" ou "Quanto eu já gastei com Uber este mês?"'
                    className="w-full pl-4 pr-11 py-3 rounded-xl bg-white/10 border border-white/20 text-white placeholder-blue-200/60 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
                  />
                  <button
                    type="button"
                    onClick={iniciarReconhecimentoVoz}
                    className={`absolute right-2.5 top-2.5 p-1.5 rounded-lg transition ${
                      ouvindoVoz ? 'bg-rose-500 text-white animate-pulse' : 'bg-white/10 hover:bg-white/20 text-blue-200'
                    }`}
                    title="Ditar comando por voz (Web Speech API - Seção 11.2)"
                  >
                    {ouvindoVoz ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
                  </button>
                </div>
                <button
                  type="submit"
                  className="px-5 py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-sm flex items-center justify-center gap-2 shadow transition"
                >
                  <Sparkles className="w-4 h-4" />
                  <span>Interpretar com IA</span>
                </button>
              </form>

              {/* Sugestoes Rapidas da Secao 11.1 do Artigo */}
              <div className="mt-3 flex flex-wrap items-center gap-1.5 text-xs">
                <span className="text-blue-300/80 text-[11px] mr-1">Exemplos do Artigo:</span>
                {[
                  'Registrar gasto de R$ 45,90 na padaria hoje no cartão de crédito',
                  'Quanto eu já gastei com Uber este mês?',
                  'Quanto gastamos com Alimentação este mês?',
                  'Lançar compra de R$ 1.500,00 de notebook em 10x no cartão de crédito'
                ].map((ex, i) => (
                  <button
                    key={i}
                    type="button"
                    onClick={() => {
                      setTextoComandoIA(ex);
                      handleInterpretarComandoIA(null, ex);
                    }}
                    className="px-2.5 py-1 rounded-lg bg-white/10 hover:bg-white/20 text-blue-100 text-[11px] transition"
                  >
                    “{ex}”
                  </button>
                ))}
              </div>

              {/* CARD DE CONFIRMACAO OBRIGATORIA DO ASSISTENTE IA (RN-006) */}
              {cardConfirmacaoIA && (
                <div className="mt-4 bg-white text-slate-900 rounded-xl p-4 shadow-xl border-2 border-amber-400">
                  <div className="flex flex-wrap items-center justify-between gap-2 pb-3 mb-3 border-b border-slate-200">
                    <div className="flex items-center gap-2">
                      <span className="px-2.5 py-1 rounded-full bg-amber-100 text-amber-800 font-bold text-xs">
                        RN-006: Confirmação Obrigatória do Lançamento
                      </span>
                      <SeloModeloIA comando={cardConfirmacaoIA} />
                    </div>
                    <span className="text-[11px] text-emerald-700 font-medium flex items-center gap-1">
                      <Lock className="w-3.5 h-3.5" /> RNF-004: Zero saldos ou CPF enviados ao modelo
                    </span>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-3 text-xs">
                    <div>
                      <label className="font-semibold text-slate-600 block mb-1">Descrição</label>
                      <input
                        type="text"
                        value={cardConfirmacaoIA.descricao || ''}
                        onChange={(e) => setCardConfirmacaoIA({ ...cardConfirmacaoIA, descricao: e.target.value })}
                        className="w-full px-2.5 py-1.5 border border-slate-300 rounded-lg"
                      />
                    </div>
                    <div>
                      <label className="font-semibold text-slate-600 block mb-1">Valor (R$)</label>
                      <input
                        type="number"
                        step="0.01"
                        value={cardConfirmacaoIA.valor || ''}
                        onChange={(e) => setCardConfirmacaoIA({ ...cardConfirmacaoIA, valor: e.target.value })}
                        className="w-full px-2.5 py-1.5 border border-slate-300 rounded-lg font-bold text-slate-900"
                      />
                    </div>
                    <div>
                      <label className="font-semibold text-slate-600 block mb-1">Data</label>
                      <input
                        type="date"
                        value={cardConfirmacaoIA.data || ''}
                        onChange={(e) => setCardConfirmacaoIA({ ...cardConfirmacaoIA, data: e.target.value })}
                        className="w-full px-2.5 py-1.5 border border-slate-300 rounded-lg"
                      />
                    </div>
                    <div>
                      <label className="font-semibold text-slate-600 block mb-1">Conta</label>
                      <select
                        value={cardConfirmacaoIA.contaId || ''}
                        onChange={(e) => {
                          const c = contas.find((x) => String(x.id) === e.target.value);
                          setCardConfirmacaoIA({
                            ...cardConfirmacaoIA,
                            contaId: Number(e.target.value),
                            conta: c ? c.nome : cardConfirmacaoIA.conta
                          });
                        }}
                        className="w-full px-2.5 py-1.5 border border-slate-300 rounded-lg"
                      >
                        {contas.map((c) => (
                          <option key={c.id} value={c.id}>
                            {c.nome}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="font-semibold text-slate-600 block mb-1">Categoria</label>
                      <select
                        value={cardConfirmacaoIA.categoriaId || ''}
                        onChange={(e) => {
                          const cat = categoriasPlanas.find((x) => String(x.id) === e.target.value);
                          setCardConfirmacaoIA({
                            ...cardConfirmacaoIA,
                            categoriaId: Number(e.target.value),
                            categoria: cat ? cat.nome : cardConfirmacaoIA.categoria
                          });
                        }}
                        className="w-full px-2.5 py-1.5 border border-slate-300 rounded-lg"
                      >
                        {categoriasPlanas.map((cat) => (
                          <option key={cat.id} value={cat.id}>
                            {cat.caminhoCompleto || cat.nome}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="font-semibold text-slate-600 block mb-1">Parcelas (RN-003)</label>
                      <input
                        type="number"
                        min="1"
                        max="48"
                        value={cardConfirmacaoIA.parcelas || 1}
                        onChange={(e) => setCardConfirmacaoIA({ ...cardConfirmacaoIA, parcelas: Number(e.target.value) })}
                        className="w-full px-2.5 py-1.5 border border-slate-300 rounded-lg"
                      />
                    </div>
                  </div>

                  <div className="mt-3 flex justify-end gap-2">
                    <button
                      type="button"
                      onClick={() => setCardConfirmacaoIA(null)}
                      className="px-3.5 py-1.5 rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-100 text-xs font-semibold"
                    >
                      Cancelar (Nada é gravado - Fluxo 4a)
                    </button>
                    <button
                      type="button"
                      onClick={handleConfirmarCardIA}
                      className="px-4 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold flex items-center gap-1.5 shadow"
                    >
                      <Check className="w-4 h-4" />
                      <span>Confirmar e Gravar Lançamento (RN-006)</span>
                    </button>
                  </div>
                </div>
              )}

              {/* RESULTADO DE CONSULTA DO ASSISTENTE IA (RN-006 / UC12) */}
              {resultadoConsultaIA && (
                <div className="mt-4 bg-white text-slate-900 rounded-xl p-4 shadow-xl border border-blue-300">
                  <div className="flex items-center justify-between mb-2">
                    <span className="px-2.5 py-0.5 rounded-full bg-blue-100 text-blue-800 font-bold text-xs">
                      Resposta da Consulta Calculada no Banco Local (RN-006)
                    </span>
                    <SeloModeloIA comando={resultadoConsultaIA.comando} />
                    <button
                      onClick={() => setResultadoConsultaIA(null)}
                      className="text-xs text-slate-500 hover:text-slate-800"
                    >
                      Fechar
                    </button>
                  </div>
                  <p className="text-sm font-bold text-slate-900 mb-2">
                    {resultadoConsultaIA.resposta.mensagem}
                  </p>
                  {resultadoConsultaIA.resposta.transacoesEncontradas?.length > 0 && (
                    <div className="max-h-40 overflow-y-auto border border-slate-200 rounded-lg">
                      <table className="w-full text-xs">
                        <thead className="bg-slate-100 text-slate-600">
                          <tr>
                            <th className="p-2 text-left">Data</th>
                            <th className="p-2 text-left">Descrição</th>
                            <th className="p-2 text-left">Conta</th>
                            <th className="p-2 text-right">Valor</th>
                          </tr>
                        </thead>
                        <tbody>
                          {resultadoConsultaIA.resposta.transacoesEncontradas.map((t) => (
                            <tr key={t.id} className="border-t border-slate-100">
                              <td className="p-2">{t.data}</td>
                              <td className="p-2 font-medium">{t.descricao}</td>
                              <td className="p-2">{t.conta?.nome}</td>
                              <td className="p-2 text-right font-bold">{fmtBRL(t.valor)}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              )}

              {/* PAINEL DE RESULTADO DOS 50 COMANDOS DE TESTE (META OE5) */}
              {benchmark50 && (
                <div className="mt-4 bg-white text-slate-900 rounded-xl p-4 shadow-xl border-2 border-emerald-400">
                  <div className="flex flex-wrap items-center justify-between gap-2 mb-3">
                    <div>
                      <h3 className="text-sm font-extrabold text-slate-900">
                        Relatório Oficial de Qualidade da IA — 50 Comandos de Teste (Seção 11.4 & Meta OE5)
                      </h3>
                      <p className="text-xs text-slate-600">
                        Taxa de Acerto: <strong className="text-emerald-700">{benchmark50.taxaAcertoPercentual}% ({benchmark50.comandosCorretos}/{benchmark50.totalComandosTestados})</strong> — Meta exigida: ≥ 80% | Tempo Médio: <strong>{benchmark50.tempoMedioMs}ms</strong> (Meta: &lt; 3000ms)
                      </p>
                      {benchmark50.respondidosPorProvedor && (
                        <p className="text-xs text-slate-600">
                          Respondido por:{' '}
                          {Object.entries(benchmark50.respondidosPorProvedor)
                            .map(([prov, qtd]) => `${prov} (${qtd})`)
                            .join(' • ')}
                        </p>
                      )}
                    </div>
                    <button
                      onClick={() => setBenchmark50(null)}
                      className="px-2.5 py-1 rounded bg-slate-200 hover:bg-slate-300 text-xs font-semibold"
                    >
                      Ocultar Tabela
                    </button>
                  </div>
                  <div className="max-h-56 overflow-y-auto border border-slate-200 rounded-lg">
                    <table className="w-full text-xs">
                      <thead className="bg-slate-100 text-slate-700 sticky top-0">
                        <tr>
                          <th className="p-2 text-left">#</th>
                          <th className="p-2 text-left">Comando em Linguagem Natural</th>
                          <th className="p-2 text-left">Intenção</th>
                          <th className="p-2 text-left">Descrição / Filtro</th>
                          <th className="p-2 text-right">Valor</th>
                          <th className="p-2 text-right">Tempo</th>
                          <th className="p-2 text-left">Provedor</th>
                          <th className="p-2 text-center">Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {benchmark50.resultados.map((r) => (
                          <tr key={r.id} className="border-t border-slate-100 hover:bg-slate-50">
                            <td className="p-2 font-mono">{r.id}</td>
                            <td className="p-2">{r.comando}</td>
                            <td className="p-2 uppercase font-semibold text-[11px]">{r.intencaoObtida}</td>
                            <td className="p-2">{r.descricaoOuFiltro}</td>
                            <td className="p-2 text-right">{r.valorObtido ? fmtBRL(r.valorObtido) : '-'}</td>
                            <td className="p-2 text-right font-mono">{r.tempoRespostaMs}ms</td>
                            <td className="p-2 text-[11px] text-slate-600">{r.provedorIA}</td>
                            <td className="p-2 text-center">
                              <span
                                className={`px-2 py-0.5 rounded-full font-bold text-[10px] ${
                                  r.passou ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'
                                }`}
                              >
                                {r.passou ? 'OK' : 'ERRO'}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </section>

            {/* Cards Executivos de Patrimonio Liquido e Fluxo de Caixa (RF-009, RN-001) */}
            <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    Patrimônio Líquido Familiar
                  </span>
                  <button
                    onClick={() => setMostrarPrivadasNoCard(!mostrarPrivadasNoCard)}
                    className="text-[11px] text-blue-600 hover:underline flex items-center gap-1"
                    title="Alternar visualização do efeito da RN-001 (Contas Privadas)"
                  >
                    {mostrarPrivadasNoCard ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                    <span>RN-001</span>
                  </button>
                </div>
                <div className="text-2xl font-extrabold text-slate-900">
                  {fmtBRL(
                    mostrarPrivadasNoCard
                      ? dashboard?.patrimonioTotalComContasPrivadas
                      : dashboard?.patrimonioLiquidoFamiliar
                  )}
                </div>
                <p className="text-[11px] text-slate-500 mt-1">
                  {mostrarPrivadasNoCard
                    ? `Inclui contas privadas (${fmtBRL(dashboard?.saldoContasPrivadasOcultas)})`
                    : `Visão Global RN-001 (oculta contas privadas: ${fmtBRL(dashboard?.saldoContasPrivadasOcultas)})`}
                </p>
              </div>

              <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    Carteira de Investimentos
                  </span>
                  <TrendingUp className="w-4 h-4 text-emerald-600" />
                </div>
                <div className="text-2xl font-extrabold text-emerald-700">
                  {fmtBRL(dashboard?.valorCarteiraInvestimentos)}
                </div>
                <p className="text-[11px] text-slate-500 mt-1">
                  Proventos recebidos (RN-005): <strong>{fmtBRL(dashboard?.totalProventosRecebidos)}</strong>
                </p>
              </div>

              <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    Receitas no Mês ({dashboard?.mesReferencia})
                  </span>
                  <ArrowUpRight className="w-4 h-4 text-emerald-600" />
                </div>
                <div className="text-2xl font-extrabold text-slate-900">
                  {fmtBRL(dashboard?.receitasMes)}
                </div>
                <p className="text-[11px] text-emerald-600 font-medium mt-1">
                  Salários + Proventos automáticos
                </p>
              </div>

              <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    Despesas no Mês ({dashboard?.mesReferencia})
                  </span>
                  <ArrowDownRight className="w-4 h-4 text-rose-600" />
                </div>
                <div className="text-2xl font-extrabold text-rose-600">
                  {fmtBRL(dashboard?.despesasMes)}
                </div>
                <p className="text-[11px] text-slate-500 mt-1">
                  Saldo de Caixa Mensal: <strong className="text-slate-800">{fmtBRL(dashboard?.fluxoCaixaMes)}</strong>
                </p>
              </div>
            </section>

            {/* Graficos Interativos: Evolucao Patrimonial e Fluxo de Caixa (RF-009) */}
            <section className="grid grid-cols-1 lg:grid-cols-2 gap-5">
              {/* Grafico 1: Evolucao Patrimonial e Fluxo de Caixa Mensal */}
              <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h3 className="text-sm font-bold text-slate-900">
                      Evolução Patrimonial e Fluxo de Caixa (Últimos 6 Meses — RF-009)
                    </h3>
                    <p className="text-xs text-slate-500">
                      Comparativo mensal de Receitas, Despesas e Patrimônio Líquido Consolidado
                    </p>
                  </div>
                </div>

                <div className="space-y-3">
                  {(dashboard?.serieHistoricaMensal || []).map((item) => {
                    const maxVal = 20000;
                    const wRec = Math.min(100, Math.round((Number(item.receitas) / maxVal) * 100));
                    const wDesp = Math.min(100, Math.round((Number(item.despesas) / maxVal) * 100));
                    return (
                      <div key={item.mes} className="p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                        <div className="flex items-center justify-between text-xs mb-1.5">
                          <span className="font-bold text-slate-800">{item.mes}</span>
                          <div className="flex items-center gap-3">
                            <span className="text-emerald-700 font-semibold">+{fmtBRL(item.receitas)}</span>
                            <span className="text-rose-600 font-semibold">-{fmtBRL(item.despesas)}</span>
                            <span className="text-blue-700 font-bold">
                              Patrimônio: {fmtBRL(item.patrimonioLiquido)}
                            </span>
                          </div>
                        </div>
                        <div className="space-y-1">
                          <div className="w-full bg-slate-200 h-2 rounded-full overflow-hidden">
                            <div className="bg-emerald-500 h-full rounded-full" style={{ width: `${wRec}%` }} />
                          </div>
                          <div className="w-full bg-slate-200 h-2 rounded-full overflow-hidden">
                            <div className="bg-rose-500 h-full rounded-full" style={{ width: `${wDesp}%` }} />
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Grafico 2: Acompanhamento de Orcamentos e Gatilhos 80% / 100% (RF-006, RN-004) */}
              <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h3 className="text-sm font-bold text-slate-900">
                      Orçado vs. Realizado por Categoria (RF-006 & RN-004)
                    </h3>
                    <p className="text-xs text-slate-500">
                      Alertas automáticos disparados em 80% (Atenção) e 100% (Limite Estourado)
                    </p>
                  </div>
                  <button
                    onClick={() => setAbaAtiva('orcamentos')}
                    className="text-xs text-blue-600 hover:underline font-semibold"
                  >
                    Gerenciar Metas →
                  </button>
                </div>

                <div className="space-y-3.5">
                  {orcamentos.map((orc) => {
                    const perc =
                      orc.valorPlanejado > 0
                        ? Math.round((Number(orc.valorGasto) / Number(orc.valorPlanejado)) * 1000) / 10
                        : 0;
                    const corBarra =
                      perc >= 100
                        ? 'bg-rose-600'
                        : perc >= 80
                        ? 'bg-amber-500'
                        : 'bg-emerald-500';
                    return (
                      <div key={orc.id} className="space-y-1">
                        <div className="flex items-center justify-between text-xs">
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-slate-800">{orc.categoria?.nome}</span>
                            {perc >= 100 && (
                              <span className="px-2 py-0.5 rounded-full bg-rose-100 text-rose-800 font-bold text-[10px]">
                                100% ESTOURADO (RN-004)
                              </span>
                            )}
                            {perc >= 80 && perc < 100 && (
                              <span className="px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 font-bold text-[10px]">
                                ALERTA ≥ 80% (RN-004)
                              </span>
                            )}
                          </div>
                          <span className="text-slate-600 font-medium">
                            {fmtBRL(orc.valorGasto)} / <strong>{fmtBRL(orc.valorPlanejado)}</strong> ({perc}%)
                          </span>
                        </div>
                        <div className="w-full h-2.5 bg-slate-100 rounded-full overflow-hidden border border-slate-200">
                          <div
                            className={`h-full ${corBarra} transition-all duration-300`}
                            style={{ width: `${Math.min(100, perc)}%` }}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </section>
          </>
        )}

        {/* ===================================================================
            ABA 2: GESTAO DE CONTAS E CARTOES (RF-002, OE1, RN-001, RN-002)
           =================================================================== */}
        {abaAtiva === 'contas' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
            <div className="lg:col-span-2 space-y-4">
              <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h2 className="text-base font-bold text-slate-900">
                      Contas Bancárias, Poupanças, Cartões e Corretoras (RF-002 & Meta OE1)
                    </h2>
                    <p className="text-xs text-slate-500">
                      RN-001: Contas privadas não entram no Patrimônio Global | RN-002: Exclusão bloqueada se saldo ≠ R$ 0,00
                    </p>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
                  {contas.map((c) => (
                    <div
                      key={c.id}
                      className={`p-4 rounded-xl border transition ${
                        c.privada
                          ? 'bg-amber-50/40 border-amber-300'
                          : 'bg-slate-50 border-slate-200 hover:border-blue-300'
                      }`}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <div className="flex items-center gap-1.5">
                            <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded bg-blue-100 text-blue-800">
                              {c.tipo}
                            </span>
                            {c.privada && (
                              <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded bg-amber-200 text-amber-900 flex items-center gap-1">
                                <Lock className="w-3 h-3" /> Privada (RN-001)
                              </span>
                            )}
                          </div>
                          <h3 className="text-sm font-bold text-slate-900 mt-1.5">{c.nome}</h3>
                          <p className="text-xs text-slate-500">
                            {c.instituicao} • Titular: {c.proprietario?.nome || 'Família'}
                          </p>
                        </div>
                        <div className="text-right">
                          <div
                            className={`text-base font-extrabold ${
                              Number(c.saldo) < 0 ? 'text-rose-600' : 'text-slate-900'
                            }`}
                          >
                            {fmtBRL(c.saldo)}
                          </div>
                          {c.diaVencimento && (
                            <span className="text-[10px] text-slate-500">Vence dia {c.diaVencimento}</span>
                          )}
                        </div>
                      </div>

                      <div className="mt-3 pt-2.5 border-t border-slate-200/80 flex items-center justify-between text-xs">
                        <button
                          onClick={() => handleAlternarPrivacidadeConta(c)}
                          className="text-blue-600 hover:underline font-medium flex items-center gap-1"
                        >
                          {c.privada ? 'Tornar Compartilhada' : 'Marcar como Privada (RN-001)'}
                        </button>

                        <button
                          onClick={() => handleExcluirConta(c)}
                          className={`px-2.5 py-1 rounded-lg font-semibold flex items-center gap-1 ${
                            Number(c.saldo) === 0
                              ? 'bg-emerald-100 text-emerald-800 hover:bg-emerald-200'
                              : 'bg-rose-100 text-rose-700 hover:bg-rose-200'
                          }`}
                          title="RN-002: Só permite excluir se saldo == 0"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                          <span>{Number(c.saldo) === 0 ? 'Excluir (Saldo 0)' : 'Testar Bloqueio RN-002'}</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Formulario de Nova Conta */}
            <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 h-fit">
              <h3 className="text-sm font-bold text-slate-900 mb-3">Cadastrar Nova Conta / Cartão (RF-002)</h3>
              <form onSubmit={handleCriarConta} className="space-y-3 text-xs">
                <div>
                  <label className="font-semibold text-slate-700 block mb-1">Nome da Conta ou Cartão</label>
                  <input
                    type="text"
                    required
                    value={novaConta.nome}
                    onChange={(e) => setNovaConta({ ...novaConta, nome: e.target.value })}
                    placeholder="Ex: Conta Corrente Bradesco"
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="font-semibold text-slate-700 block mb-1">Instituição Financeira</label>
                  <input
                    type="text"
                    value={novaConta.instituicao}
                    onChange={(e) => setNovaConta({ ...novaConta, instituicao: e.target.value })}
                    placeholder="Ex: Bradesco / Nubank / XP"
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Tipo de Conta (OE1)</label>
                    <select
                      value={novaConta.tipo}
                      onChange={(e) => setNovaConta({ ...novaConta, tipo: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                    >
                      <option value="CORRENTE">Conta Corrente</option>
                      <option value="POUPANCA">Conta Poupança</option>
                      <option value="CARTAO_CREDITO">Cartão de Crédito</option>
                      <option value="CORRETORA">Conta Corretora</option>
                    </select>
                  </div>
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Saldo Inicial (R$)</label>
                    <input
                      type="number"
                      step="0.01"
                      value={novaConta.saldo}
                      onChange={(e) => setNovaConta({ ...novaConta, saldo: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                    />
                  </div>
                </div>

                {novaConta.tipo === 'CARTAO_CREDITO' && (
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="font-semibold text-slate-700 block mb-1">Dia de Vencimento</label>
                      <input
                        type="number"
                        min="1"
                        max="31"
                        value={novaConta.diaVencimento}
                        onChange={(e) => setNovaConta({ ...novaConta, diaVencimento: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                      />
                    </div>
                    <div>
                      <label className="font-semibold text-slate-700 block mb-1">Limite (R$)</label>
                      <input
                        type="number"
                        step="0.01"
                        value={novaConta.limiteCredito}
                        onChange={(e) => setNovaConta({ ...novaConta, limiteCredito: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                      />
                    </div>
                  </div>
                )}

                <label className="flex items-center gap-2 p-2.5 rounded-lg bg-amber-50 border border-amber-200 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={novaConta.privada}
                    onChange={(e) => setNovaConta({ ...novaConta, privada: e.target.checked })}
                  />
                  <span className="text-amber-900 font-medium">
                    Marcar como Conta Privada (RN-001: não soma no Patrimônio Global)
                  </span>
                </label>

                <button
                  type="submit"
                  className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-lg shadow transition"
                >
                  Salvar Conta
                </button>
              </form>
            </div>
          </div>
        )}

        {/* ===================================================================
            ABA 3: LANCAMENTO DE TRANSACOES, PARCELAMENTO E RECORRENCIA (RF-003, RN-003)
           =================================================================== */}
        {abaAtiva === 'transacoes' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
            <div className="lg:col-span-2 bg-white rounded-2xl p-5 shadow-sm border border-slate-200">
              <div className="flex flex-wrap items-center justify-between gap-2 mb-4">
                <div>
                  <h2 className="text-base font-bold text-slate-900">
                    Extrato Consolidado de Lançamentos Financeiros (RF-003 & RN-003)
                  </h2>
                  <p className="text-xs text-slate-500">
                    Suporte a receitas, despesas, transferências, parcelamento automático e recorrência
                  </p>
                </div>
              </div>

              {/* Barra de Filtros */}
              <div className="grid grid-cols-1 sm:grid-cols-4 gap-2 mb-4 text-xs">
                <input
                  type="text"
                  placeholder="Buscar por descrição (ex: Uber, Padaria)..."
                  value={filtroTransacao.busca}
                  onChange={(e) => handleFiltrarTransacoes({ busca: e.target.value })}
                  className="px-3 py-2 border border-slate-300 rounded-lg"
                />
                <select
                  value={filtroTransacao.contaId}
                  onChange={(e) => handleFiltrarTransacoes({ contaId: e.target.value })}
                  className="px-3 py-2 border border-slate-300 rounded-lg"
                >
                  <option value="">Todas as Contas</option>
                  {contas.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.nome}
                    </option>
                  ))}
                </select>
                <select
                  value={filtroTransacao.tipo}
                  onChange={(e) => handleFiltrarTransacoes({ tipo: e.target.value })}
                  className="px-3 py-2 border border-slate-300 rounded-lg"
                >
                  <option value="">Todos os Tipos</option>
                  <option value="DESPESA">Despesas</option>
                  <option value="RECEITA">Receitas</option>
                </select>
                <select
                  value={filtroTransacao.periodo}
                  onChange={(e) => handleFiltrarTransacoes({ periodo: e.target.value })}
                  className="px-3 py-2 border border-slate-300 rounded-lg"
                >
                  <option value="mes_atual">Mês Atual</option>
                  <option value="hoje">Hoje</option>
                  <option value="semana_atual">Últimos 7 dias</option>
                  <option value="todos">Todo o Histórico (inclui parcelas futuras)</option>
                </select>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-xs">
                  <thead className="bg-slate-100 text-slate-600">
                    <tr>
                      <th className="p-2.5 text-left">Data</th>
                      <th className="p-2.5 text-left">Descrição</th>
                      <th className="p-2.5 text-left">Categoria (RF-004)</th>
                      <th className="p-2.5 text-left">Conta</th>
                      <th className="p-2.5 text-center">Origem / Parcela</th>
                      <th className="p-2.5 text-right">Valor</th>
                      <th className="p-2.5 text-center">Ação</th>
                    </tr>
                  </thead>
                  <tbody>
                    {transacoes.map((t) => (
                      <tr key={t.id} className="border-t border-slate-100 hover:bg-slate-50">
                        <td className="p-2.5 whitespace-nowrap font-mono">{t.data}</td>
                        <td className="p-2.5 font-semibold text-slate-900">
                          {t.descricao}
                          {t.recorrente && (
                            <span className="ml-1.5 px-1.5 py-0.5 rounded bg-purple-100 text-purple-800 text-[10px]">
                              Recorrente
                            </span>
                          )}
                        </td>
                        <td className="p-2.5 text-slate-600">{t.categoria?.nome || 'Geral'}</td>
                        <td className="p-2.5 text-slate-600">{t.conta?.nome}</td>
                        <td className="p-2.5 text-center">
                          <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-700 font-semibold text-[10px]">
                            {t.origemLancamento} • {t.parcela || 1}/{t.totalParcelas || 1}
                          </span>
                        </td>
                        <td
                          className={`p-2.5 text-right font-bold whitespace-nowrap ${
                            t.tipo === 'RECEITA' ? 'text-emerald-600' : 'text-rose-600'
                          }`}
                        >
                          {t.tipo === 'RECEITA' ? '+' : '-'}
                          {fmtBRL(t.valor)}
                        </td>
                        <td className="p-2.5 text-center">
                          <button
                            onClick={async () => {
                              await api.deleteTransacao(t.id);
                              carregarTudo();
                            }}
                            className="text-slate-400 hover:text-rose-600"
                            title="Excluir transação"
                          >
                            <Trash2 className="w-3.5 h-3.5 inline" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Formulario de Novo Lancamento com Parcelamento Automatico (RN-003) */}
            <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 h-fit">
              <h3 className="text-sm font-bold text-slate-900 mb-3">
                Novo Lançamento Manual / Parcelado (RF-003 & RN-003)
              </h3>
              <form onSubmit={handleCriarTransacao} className="space-y-3 text-xs">
                <div>
                  <label className="font-semibold text-slate-700 block mb-1">Descrição</label>
                  <input
                    type="text"
                    required
                    value={novaTransacao.descricao}
                    onChange={(e) => setNovaTransacao({ ...novaTransacao, descricao: e.target.value })}
                    placeholder="Ex: Geladeira Inox / Supermercado"
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Valor Total (R$)</label>
                    <input
                      type="number"
                      step="0.01"
                      required
                      value={novaTransacao.valor}
                      onChange={(e) => setNovaTransacao({ ...novaTransacao, valor: e.target.value })}
                      placeholder="Ex: 1200.00"
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                    />
                  </div>
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Tipo</label>
                    <select
                      value={novaTransacao.tipo}
                      onChange={(e) => setNovaTransacao({ ...novaTransacao, tipo: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                    >
                      <option value="DESPESA">Despesa</option>
                      <option value="RECEITA">Receita</option>
                      <option value="TRANSFERENCIA">Transferência</option>
                    </select>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Data</label>
                    <input
                      type="date"
                      value={novaTransacao.data}
                      onChange={(e) => setNovaTransacao({ ...novaTransacao, data: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                    />
                  </div>
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Nº de Parcelas (RN-003)</label>
                    <input
                      type="number"
                      min="1"
                      max="48"
                      value={novaTransacao.totalParcelas}
                      onChange={(e) => setNovaTransacao({ ...novaTransacao, totalParcelas: Number(e.target.value) })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                    />
                  </div>
                </div>
                <div>
                  <label className="font-semibold text-slate-700 block mb-1">Conta / Cartão</label>
                  <select
                    value={novaTransacao.contaId}
                    onChange={(e) => setNovaTransacao({ ...novaTransacao, contaId: Number(e.target.value) })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  >
                    {contas.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.nome} ({c.tipo})
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="font-semibold text-slate-700 block mb-1">Categoria</label>
                  <select
                    value={novaTransacao.categoriaId}
                    onChange={(e) => setNovaTransacao({ ...novaTransacao, categoriaId: Number(e.target.value) })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  >
                    {categoriasPlanas.map((cat) => (
                      <option key={cat.id} value={cat.id}>
                        {cat.caminhoCompleto || cat.nome}
                      </option>
                    ))}
                  </select>
                </div>

                <label className="flex items-center gap-2 p-2.5 rounded-lg bg-slate-50 border border-slate-200 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={novaTransacao.recorrente}
                    onChange={(e) => setNovaTransacao({ ...novaTransacao, recorrente: e.target.checked })}
                  />
                  <span className="text-slate-700 font-medium">
                    Lançamento Recorrente Mensal (ex: Aluguel, Assinatura)
                  </span>
                </label>

                <button
                  type="submit"
                  className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-lg shadow transition"
                >
                  Registrar Lançamento
                </button>
              </form>
            </div>
          </div>
        )}

        {/* ===================================================================
            ABA 4: IMPORTACAO DE EXTRATOS CSV (RF-005, OE2, BPMN FIGURA 1, UC05)
           =================================================================== */}
        {abaAtiva === 'importacao' && (
          <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-4">
            <div>
              <h2 className="text-base font-bold text-slate-900">
                Importação de Extratos Bancários CSV com Motor de Conciliação (RF-005, Meta OE2 & BPMN Figura 1)
              </h2>
              <p className="text-xs text-slate-500">
                Fluxo UC05: 1. Escolher conta e enviar CSV → 2. Validar colunas → 3. Descartar duplicadas (OE2: 0 duplicidades) → 4. Revisar categorias → 5. Confirmar importação.
              </p>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 text-xs">
              <div className="space-y-3">
                <div>
                  <label className="font-semibold text-slate-700 block mb-1">1. Conta de Destino do Extrato</label>
                  <select
                    value={csvContaId}
                    onChange={(e) => setCsvContaId(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  >
                    {contas.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.nome} ({c.instituicao})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="font-semibold text-slate-700">
                      2. Conteúdo do Arquivo CSV (data;descricao;valor;tipo)
                    </label>
                    <input
                      type="file"
                      accept=".csv,.txt"
                      onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (file) {
                          const reader = new FileReader();
                          reader.onload = (ev) => setCsvConteudo(String(ev.target?.result || ''));
                          reader.readAsText(file);
                        }
                      }}
                      className="text-[11px]"
                    />
                  </div>
                  <textarea
                    rows={7}
                    value={csvConteudo}
                    onChange={(e) => setCsvConteudo(e.target.value)}
                    className="w-full p-3 border border-slate-300 rounded-lg font-mono text-xs"
                  />
                </div>

                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={handleAnalisarCsv}
                    className="px-4 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-lg shadow"
                  >
                    Validar CSV e Descartar Duplicadas (Etapa 1 BPMN)
                  </button>
                  <button
                    type="button"
                    onClick={() => setCsvConteudo(EXTRATO_EXEMPLO_CSV)}
                    className="px-3 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold rounded-lg"
                  >
                    Restaurar Exemplo
                  </button>
                </div>
              </div>

              {/* Painel de Revisao de Categorias antes da Confirmacao (BPMN Figura 1) */}
              <div className="bg-slate-50 rounded-xl p-4 border border-slate-200">
                <h3 className="text-sm font-bold text-slate-900 mb-2">
                  3. Revisão de Categorias e Conciliação de Duplicidades (UC05)
                </h3>

                {csvAnalise ? (
                  <div className="space-y-3">
                    <div className="grid grid-cols-3 gap-2 text-center">
                      <div className="p-2 rounded-lg bg-white border border-slate-200">
                        <div className="text-lg font-extrabold text-slate-900">{csvAnalise.totalLinhasLidas}</div>
                        <div className="text-[10px] text-slate-500">Linhas Lidas</div>
                      </div>
                      <div className="p-2 rounded-lg bg-emerald-50 border border-emerald-200">
                        <div className="text-lg font-extrabold text-emerald-700">{csvAnalise.quantidadeNovas}</div>
                        <div className="text-[10px] text-emerald-700">Inéditas p/ Importar</div>
                      </div>
                      <div className="p-2 rounded-lg bg-amber-50 border border-amber-200">
                        <div className="text-lg font-extrabold text-amber-800">
                          {csvAnalise.quantidadeDuplicadasDescartadas}
                        </div>
                        <div className="text-[10px] text-amber-800">Duplicadas Descartadas (OE2)</div>
                      </div>
                    </div>

                    {csvAnalise.transacoesParaRevisao.length > 0 && (
                      <div className="max-h-48 overflow-y-auto bg-white rounded-lg border border-slate-200">
                        <table className="w-full text-xs">
                          <thead className="bg-slate-100">
                            <tr>
                              <th className="p-2 text-left">Data</th>
                              <th className="p-2 text-left">Descrição</th>
                              <th className="p-2 text-left">Categoria Sugerida</th>
                              <th className="p-2 text-right">Valor</th>
                            </tr>
                          </thead>
                          <tbody>
                            {csvAnalise.transacoesParaRevisao.map((item, idx) => (
                              <tr key={idx} className="border-t border-slate-100">
                                <td className="p-2">{item.data}</td>
                                <td className="p-2 font-medium">{item.descricao}</td>
                                <td className="p-2">
                                  <select
                                    value={item.categoriaId || ''}
                                    onChange={(e) => {
                                      const novas = [...csvAnalise.transacoesParaRevisao];
                                      novas[idx] = { ...novas[idx], categoriaId: Number(e.target.value) };
                                      setCsvAnalise({ ...csvAnalise, transacoesParaRevisao: novas });
                                    }}
                                    className="px-2 py-1 border border-slate-300 rounded text-[11px]"
                                  >
                                    {categoriasPlanas.map((cat) => (
                                      <option key={cat.id} value={cat.id}>
                                        {cat.nome}
                                      </option>
                                    ))}
                                  </select>
                                </td>
                                <td className="p-2 text-right font-bold">{fmtBRL(item.valor)}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}

                    <button
                      type="button"
                      onClick={handleConfirmarCsv}
                      className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg shadow"
                    >
                      Confirmar Importação e Atualizar Orçamentos (Etapa Final BPMN)
                    </button>
                  </div>
                ) : csvUltimoResultado ? (
                  <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-900 space-y-1">
                    <div className="font-bold text-sm">Extrato Importado com Sucesso!</div>
                    <p>Transações Gravadas: <strong>{csvUltimoResultado.transacoesGravadas}</strong></p>
                    <p>Duplicidades Descartadas (OE2): <strong>{csvUltimoResultado.duplicadasDescartadas}</strong></p>
                    <p className="text-[11px] pt-1">
                      Dica para demonstrar a Meta OE2: Clique novamente em "Validar CSV" com o mesmo arquivo para comprovar que 100% das linhas já gravadas são descartadas automaticamente!
                    </p>
                  </div>
                ) : (
                  <p className="text-xs text-slate-500">
                    Clique em "Validar CSV e Descartar Duplicadas" ao lado para visualizar a conciliação SHA-256 antes de gravar.
                  </p>
                )}
              </div>
            </div>
          </div>
        )}

        {/* ===================================================================
            ABA 5: ORCAMENTOS MENSAIS E ALERTAS 80% / 100% (RF-006, RN-004, OE3)
           =================================================================== */}
        {abaAtiva === 'orcamentos' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
            <div className="lg:col-span-2 bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-4">
              <div>
                <h2 className="text-base font-bold text-slate-900">
                  Metas de Orçamento Mensal por Categoria (RF-006, RN-004 & Meta OE3)
                </h2>
                <p className="text-xs text-slate-500">
                  O sistema monitora os gastos realizados em cada categoria (incluindo subcategorias) e dispara notificações em 80% e 100%.
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
                {orcamentos.map((orc) => {
                  const perc =
                    orc.valorPlanejado > 0
                      ? Math.round((Number(orc.valorGasto) / Number(orc.valorPlanejado)) * 1000) / 10
                      : 0;
                  return (
                    <div key={orc.id} className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-sm text-slate-900">{orc.categoria?.nome}</span>
                        <span className="text-xs font-mono text-slate-500">{orc.mes}</span>
                      </div>
                      <div className="flex items-baseline justify-between text-xs">
                        <span>
                          Gasto Realizado: <strong className="text-slate-900">{fmtBRL(orc.valorGasto)}</strong>
                        </span>
                        <span>
                          Planejado: <strong>{fmtBRL(orc.valorPlanejado)}</strong>
                        </span>
                      </div>
                      <div className="w-full h-3 bg-slate-200 rounded-full overflow-hidden">
                        <div
                          className={`h-full ${
                            perc >= 100 ? 'bg-rose-600' : perc >= 80 ? 'bg-amber-500' : 'bg-emerald-500'
                          }`}
                          style={{ width: `${Math.min(100, perc)}%` }}
                        />
                      </div>
                      <div className="flex items-center justify-between text-[11px] pt-1">
                        <span className="font-bold">{perc}% executado</span>
                        <div className="flex gap-1">
                          <span
                            className={`px-2 py-0.5 rounded ${
                              orc.alerta80Disparado
                                ? 'bg-amber-200 text-amber-900 font-bold'
                                : 'bg-slate-200 text-slate-500'
                            }`}
                          >
                            Gatilho 80%: {orc.alerta80Disparado ? 'ATIVO' : 'OK'}
                          </span>
                          <span
                            className={`px-2 py-0.5 rounded ${
                              orc.alerta100Disparado
                                ? 'bg-rose-200 text-rose-900 font-bold'
                                : 'bg-slate-200 text-slate-500'
                            }`}
                          >
                            Gatilho 100%: {orc.alerta100Disparado ? 'ATIVO' : 'OK'}
                          </span>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 h-fit">
              <h3 className="text-sm font-bold text-slate-900 mb-3">Definir / Ajustar Meta Mensal (RF-006)</h3>
              <form onSubmit={handleSalvarOrcamento} className="space-y-3 text-xs">
                <div>
                  <label className="font-semibold text-slate-700 block mb-1">Categoria</label>
                  <select
                    value={novoOrcamento.categoriaId}
                    onChange={(e) => setNovoOrcamento({ ...novoOrcamento, categoriaId: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  >
                    {categoriasPlanas
                      .filter((c) => !c.pai)
                      .map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.nome}
                        </option>
                      ))}
                  </select>
                </div>
                <div>
                  <label className="font-semibold text-slate-700 block mb-1">Mês (YearMonth)</label>
                  <input
                    type="month"
                    value={novoOrcamento.mes}
                    onChange={(e) => setNovoOrcamento({ ...novoOrcamento, mes: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="font-semibold text-slate-700 block mb-1">Limite Planejado (R$)</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={novoOrcamento.valorPlanejado}
                    onChange={(e) => setNovoOrcamento({ ...novoOrcamento, valorPlanejado: e.target.value })}
                    placeholder="Ex: 1800.00"
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
                <button
                  type="submit"
                  className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-lg shadow"
                >
                  Salvar Orçamento Mensal
                </button>
              </form>
            </div>
          </div>
        )}

        {/* ===================================================================
            ABA 6: INVESTIMENTOS E PROVENTOS (RF-008, RN-005, BPMN FIGURA 3)
           =================================================================== */}
        {abaAtiva === 'investimentos' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
            <div className="lg:col-span-2 space-y-4">
              {/* Resumo da Carteira */}
              <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
                <div className="bg-white p-4 rounded-xl border border-slate-200">
                  <div className="text-[11px] text-slate-500">Custo de Aquisição</div>
                  <div className="text-lg font-extrabold text-slate-900">
                    {fmtBRL(carteira?.custoTotalCarteira)}
                  </div>
                </div>
                <div className="bg-white p-4 rounded-xl border border-slate-200">
                  <div className="text-[11px] text-slate-500">Valor Atual de Mercado</div>
                  <div className="text-lg font-extrabold text-blue-700">
                    {fmtBRL(carteira?.valorMercadoCarteira)}
                  </div>
                </div>
                <div className="bg-white p-4 rounded-xl border border-slate-200">
                  <div className="text-[11px] text-slate-500">Resultado / Valorização</div>
                  <div className="text-lg font-extrabold text-emerald-600">
                    {fmtBRL(carteira?.lucroPrejuizoCarteira)} ({carteira?.rentabilidadeCarteiraPercentual}%)
                  </div>
                </div>
                <div className="bg-white p-4 rounded-xl border border-emerald-200 bg-emerald-50/40">
                  <div className="text-[11px] text-emerald-800 font-semibold">Proventos Recebidos (RN-005)</div>
                  <div className="text-lg font-extrabold text-emerald-700">
                    {fmtBRL(carteira?.totalProventosRecebidos)}
                  </div>
                </div>
              </div>

              {/* Tabela de Posicoes por Ativo (Ticker) */}
              <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200">
                <h2 className="text-base font-bold text-slate-900 mb-3">
                  Posição Consolidada da Carteira por Ativo / Ticker (RF-008 & BPMN Figura 3)
                </h2>
                <div className="overflow-x-auto">
                  <table className="w-full text-xs">
                    <thead className="bg-slate-100 text-slate-600">
                      <tr>
                        <th className="p-2.5 text-left">Ticker</th>
                        <th className="p-2.5 text-left">Ativo / Classe</th>
                        <th className="p-2.5 text-right">Qtd.</th>
                        <th className="p-2.5 text-right">Preço Médio</th>
                        <th className="p-2.5 text-right">Preço Atual</th>
                        <th className="p-2.5 text-right">Total Atual</th>
                        <th className="p-2.5 text-right">Proventos (RN-005)</th>
                      </tr>
                    </thead>
                    <tbody>
                      {(carteira?.posicoes || []).map((pos) => (
                        <tr key={pos.ticker} className="border-t border-slate-100">
                          <td className="p-2.5 font-extrabold text-blue-700">{pos.ticker}</td>
                          <td className="p-2.5">
                            <div className="font-semibold text-slate-900">{pos.nome}</div>
                            <div className="text-[10px] text-slate-500">{pos.tipoAtivo} • {pos.setor}</div>
                          </td>
                          <td className="p-2.5 text-right font-mono">{pos.quantidade}</td>
                          <td className="p-2.5 text-right">{fmtBRL(pos.precoMedio)}</td>
                          <td className="p-2.5 text-right font-semibold">{fmtBRL(pos.precoAtual)}</td>
                          <td className="p-2.5 text-right font-bold text-slate-900">{fmtBRL(pos.valorAtual)}</td>
                          <td className="p-2.5 text-right font-bold text-emerald-700">
                            {fmtBRL(pos.proventosAcumulados)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            {/* Formulario de Operacao de Investimento e Provento (RN-005) */}
            <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 h-fit">
              <h3 className="text-sm font-bold text-slate-900 mb-1">
                Registrar Compra, Venda ou Provento (RF-008 & RN-005)
              </h3>
              <p className="text-[11px] text-slate-500 mb-3">
                RN-005: Ao lançar um PROVENTO, o sistema gera automaticamente uma Receita na conta de destino!
              </p>

              <form onSubmit={handleRegistrarInvestimento} className="space-y-3 text-xs">
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Ticker do Ativo</label>
                    <select
                      value={novaOperacaoInv.ticker}
                      onChange={(e) => setNovaOperacaoInv({ ...novaOperacaoInv, ticker: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg font-bold"
                    >
                      {(carteira?.ativosDisponiveis || []).map((a) => (
                        <option key={a.ticker} value={a.ticker}>
                          {a.ticker} — {a.nome}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Tipo de Operação</label>
                    <select
                      value={novaOperacaoInv.tipo}
                      onChange={(e) => setNovaOperacaoInv({ ...novaOperacaoInv, tipo: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg font-semibold"
                    >
                      <option value="COMPRA">COMPRA</option>
                      <option value="VENDA">VENDA</option>
                      <option value="PROVENTO">PROVENTO / DIVIDENDO (RN-005)</option>
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Quantidade / Cotas</label>
                    <input
                      type="number"
                      step="0.01"
                      required
                      value={novaOperacaoInv.quantidade}
                      onChange={(e) => setNovaOperacaoInv({ ...novaOperacaoInv, quantidade: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                    />
                  </div>
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">
                      {novaOperacaoInv.tipo === 'PROVENTO' ? 'Valor por Cota (R$)' : 'Preço Unitário (R$)'}
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      required
                      value={novaOperacaoInv.preco}
                      onChange={(e) => setNovaOperacaoInv({ ...novaOperacaoInv, preco: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                    />
                  </div>
                </div>

                <div>
                  <label className="font-semibold text-slate-700 block mb-1">
                    Conta de Destino do Provento (RN-005)
                  </label>
                  <select
                    value={novaOperacaoInv.contaDestinoProventoId}
                    onChange={(e) => setNovaOperacaoInv({ ...novaOperacaoInv, contaDestinoProventoId: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  >
                    {contas.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.nome} (Saldo: {fmtBRL(c.saldo)})
                      </option>
                    ))}
                  </select>
                </div>

                <button
                  type="submit"
                  className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg shadow transition"
                >
                  Registrar Operação na Carteira
                </button>
              </form>
            </div>
          </div>
        )}

        {/* ===================================================================
            ABA 7: ARVORE DE CATEGORIAS E TAGS (RF-004)
           =================================================================== */}
        {abaAtiva === 'categorias' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
            <div className="lg:col-span-2 bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-4">
              <h2 className="text-base font-bold text-slate-900">
                Árvore Hierárquica de Categorias e Subcategorias (RF-004)
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {categoriasArvore.map((raiz) => (
                  <div key={raiz.id} className="p-4 rounded-xl bg-slate-50 border border-slate-200">
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <span
                          className="w-3 h-3 rounded-full inline-block"
                          style={{ backgroundColor: raiz.cor }}
                        />
                        <span className="font-bold text-sm text-slate-900">{raiz.nome}</span>
                      </div>
                      <span className="text-[10px] px-2 py-0.5 rounded bg-slate-200 text-slate-700 font-semibold">
                        {raiz.tipoPadrao}
                      </span>
                    </div>
                    <div className="pl-4 border-l-2 border-slate-200 space-y-1 text-xs">
                      {(raiz.subcategorias || []).map((sub) => (
                        <div key={sub.id} className="flex items-center justify-between py-1 text-slate-700">
                          <span>{sub.caminhoCompleto}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>

              <div className="pt-3 border-t border-slate-200">
                <h3 className="text-sm font-bold text-slate-900 mb-2">Etiquetas Personalizadas (Tags — RF-004)</h3>
                <div className="flex flex-wrap gap-2">
                  {tags.map((t) => (
                    <span
                      key={t.id}
                      className="px-3 py-1 rounded-full text-xs font-semibold text-white flex items-center gap-1.5"
                      style={{ backgroundColor: t.cor }}
                    >
                      <TagIcon className="w-3 h-3" />
                      <span>{t.nome}</span>
                    </span>
                  ))}
                </div>
              </div>
            </div>

            <div className="space-y-4">
              <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200">
                <h3 className="text-sm font-bold text-slate-900 mb-3">Nova Categoria / Subcategoria</h3>
                <form
                  onSubmit={async (e) => {
                    e.preventDefault();
                    await api.createCategoria({ ...novaCategoria, grupoId: 1 });
                    setNovaCategoria({ nome: '', cor: '#3B82F6', tipoPadrao: 'DESPESA', paiId: '' });
                    exibirMensagem('sucesso', 'Categoria adicionada à árvore hierárquica (RF-004)!');
                    carregarTudo();
                  }}
                  className="space-y-3 text-xs"
                >
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Nome</label>
                    <input
                      type="text"
                      required
                      value={novaCategoria.nome}
                      onChange={(e) => setNovaCategoria({ ...novaCategoria, nome: e.target.value })}
                      placeholder="Ex: Feira Livre"
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                    />
                  </div>
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Categoria Pai (Opcional)</label>
                    <select
                      value={novaCategoria.paiId}
                      onChange={(e) => setNovaCategoria({ ...novaCategoria, paiId: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                    >
                      <option value="">Raiz (Categoria Principal)</option>
                      {categoriasArvore.map((r) => (
                        <option key={r.id} value={r.id}>
                          {r.nome}
                        </option>
                      ))}
                    </select>
                  </div>
                  <button
                    type="submit"
                    className="w-full py-2 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-lg"
                  >
                    Criar Categoria
                  </button>
                </form>
              </div>

              <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200">
                <h3 className="text-sm font-bold text-slate-900 mb-3">Criar Nova Tag (RF-004)</h3>
                <form
                  onSubmit={async (e) => {
                    e.preventDefault();
                    await api.createTag({ ...novaTag, grupoId: 1 });
                    setNovaTag({ nome: '', cor: '#6366F1' });
                    exibirMensagem('sucesso', 'Tag criada com sucesso!');
                    carregarTudo();
                  }}
                  className="space-y-3 text-xs"
                >
                  <input
                    type="text"
                    required
                    value={novaTag.nome}
                    onChange={(e) => setNovaTag({ ...novaTag, nome: e.target.value })}
                    placeholder="Nome da Tag (ex: Viagem Ferias)"
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  />
                  <button
                    type="submit"
                    className="w-full py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-lg"
                  >
                    Salvar Tag
                  </button>
                </form>
              </div>
            </div>
          </div>
        )}

        {/* ===================================================================
            ABA 8: GRUPO FAMILIAR E PERMISSOES (RF-007, OE4, BPMN FIGURA 2)
           =================================================================== */}
        {abaAtiva === 'grupo' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
            <div className="lg:col-span-2 bg-white rounded-2xl p-5 shadow-sm border border-slate-200">
              <h2 className="text-base font-bold text-slate-900 mb-1">
                Gestão do Grupo Familiar e Controle de Membros (RF-007, Meta OE4 & BPMN Figura 2)
              </h2>
              <p className="text-xs text-slate-500 mb-4">
                Fluxo de convite (CONVIDADO → ATIVO ou RECUSADO) e suspensão de membros pelo Titular.
              </p>

              <div className="space-y-3">
                {membros.map((m) => (
                  <div
                    key={m.id}
                    className="p-4 rounded-xl bg-slate-50 border border-slate-200 flex flex-wrap items-center justify-between gap-3"
                  >
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-sm text-slate-900">{m.nomeConvidado}</span>
                        <span className="px-2 py-0.5 rounded bg-blue-100 text-blue-800 font-bold text-[10px]">
                          {m.papel}
                        </span>
                        <span
                          className={`px-2 py-0.5 rounded font-bold text-[10px] ${
                            m.status === 'ATIVO'
                              ? 'bg-emerald-100 text-emerald-800'
                              : m.status === 'CONVIDADO'
                              ? 'bg-amber-100 text-amber-800'
                              : 'bg-rose-100 text-rose-800'
                          }`}
                        >
                          {m.status}
                        </span>
                      </div>
                      <p className="text-xs text-slate-500 mt-0.5">{m.emailConvidado}</p>
                    </div>

                    <div className="flex items-center gap-2 text-xs">
                      {m.status === 'CONVIDADO' && (
                        <>
                          <button
                            onClick={async () => {
                              await api.responderConvite(m.id, true);
                              exibirMensagem('sucesso', `${m.nomeConvidado} aceitou o convite e está ATIVO no grupo!`);
                              carregarTudo();
                            }}
                            className="px-3 py-1.5 rounded-lg bg-emerald-600 text-white font-semibold"
                          >
                            Aceitar Convite (BPMN)
                          </button>
                          <button
                            onClick={async () => {
                              await api.responderConvite(m.id, false);
                              exibirMensagem('aviso', `Convite de ${m.nomeConvidado} recusado.`);
                              carregarTudo();
                            }}
                            className="px-3 py-1.5 rounded-lg bg-rose-100 text-rose-700 font-semibold"
                          >
                            Recusar
                          </button>
                        </>
                      )}

                      {m.papel !== 'TITULAR' && m.status === 'ATIVO' && (
                        <button
                          onClick={async () => {
                            await api.alterarStatusMembro(m.id, { status: 'SUSPENSO' });
                            exibirMensagem('aviso', `Membro ${m.nomeConvidado} suspenso temporariamente.`);
                            carregarTudo();
                          }}
                          className="px-3 py-1.5 rounded-lg bg-amber-100 hover:bg-amber-200 text-amber-900 font-semibold"
                        >
                          Suspender Acesso
                        </button>
                      )}

                      {m.status === 'SUSPENSO' && (
                        <button
                          onClick={async () => {
                            await api.alterarStatusMembro(m.id, { status: 'ATIVO' });
                            exibirMensagem('sucesso', `Acesso de ${m.nomeConvidado} reativado!`);
                            carregarTudo();
                          }}
                          className="px-3 py-1.5 rounded-lg bg-emerald-100 text-emerald-800 font-semibold"
                        >
                          Reativar Membro
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 h-fit">
              <h3 className="text-sm font-bold text-slate-900 mb-3">Convidar Novo Membro (RF-007)</h3>
              <form onSubmit={handleConvidarMembro} className="space-y-3 text-xs">
                <div>
                  <label className="font-semibold text-slate-700 block mb-1">Nome do Convidado</label>
                  <input
                    type="text"
                    required
                    value={novoMembro.nome}
                    onChange={(e) => setNovoMembro({ ...novoMembro, nome: e.target.value })}
                    placeholder="Ex: Maria Oliveira"
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="font-semibold text-slate-700 block mb-1">E-mail</label>
                  <input
                    type="email"
                    required
                    value={novoMembro.email}
                    onChange={(e) => setNovoMembro({ ...novoMembro, email: e.target.value })}
                    placeholder="maria@ucsal.edu.br"
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="font-semibold text-slate-700 block mb-1">Papel / Permissão (OE4)</label>
                  <select
                    value={novoMembro.papel}
                    onChange={(e) => setNovoMembro({ ...novoMembro, papel: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  >
                    <option value="MEMBRO">Membro Familiar (Dependente)</option>
                    <option value="AUDITOR">Auditor / Consultor (Somente Leitura)</option>
                    <option value="TITULAR">Co-Titular (Administrador Familiar)</option>
                  </select>
                </div>
                <button
                  type="submit"
                  className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-lg shadow"
                >
                  Enviar Convite Familiar
                </button>
              </form>
            </div>
          </div>
        )}

        {/* ===================================================================
            ABA 9: RELATORIOS E EXPORTACAO EM PDF, CSV E EXCEL (RF-009, RF-010)
           =================================================================== */}
        {abaAtiva === 'relatorios' && (
          <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 space-y-5">
            <div>
              <h2 className="text-base font-bold text-slate-900">
                Central de Relatórios Gerenciais e Exportação de Dados (RF-009 & RF-010)
              </h2>
              <p className="text-xs text-slate-500">
                Exporte todos os dados consolidados da família para contabilidade, imposto de renda ou backup externo em PDF, CSV e Excel (.xlsx).
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <button
                type="button"
                onClick={() => baixarExportacao('pdf')}
                className="p-5 rounded-2xl bg-rose-50 hover:bg-rose-100/80 border border-rose-200 flex items-center justify-between transition text-left w-full"
              >
                <div>
                  <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded bg-rose-200 text-rose-900">
                    Documento PDF (OpenPDF)
                  </span>
                  <h3 className="text-sm font-bold text-slate-900 mt-2">Exportar Relatório em PDF</h3>
                  <p className="text-xs text-slate-600 mt-1">
                    Inclui Patrimônio Consolidado (RN-001), Contas, Orçamentos e Extrato.
                  </p>
                </div>
                <FileText className="w-8 h-8 text-rose-600 shrink-0" />
              </button>

              <button
                type="button"
                onClick={() => baixarExportacao('excel')}
                className="p-5 rounded-2xl bg-emerald-50 hover:bg-emerald-100/80 border border-emerald-200 flex items-center justify-between transition text-left w-full"
              >
                <div>
                  <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded bg-emerald-200 text-emerald-900">
                    Planilha Excel .XLSX (Apache POI)
                  </span>
                  <h3 className="text-sm font-bold text-slate-900 mt-2">Exportar Planilha Excel</h3>
                  <p className="text-xs text-slate-600 mt-1">
                    4 abas estruturadas: Resumo, Contas, Transações e Orçamentos.
                  </p>
                </div>
                <FileSpreadsheet className="w-8 h-8 text-emerald-600 shrink-0" />
              </button>

              <button
                type="button"
                onClick={() => baixarExportacao('csv')}
                className="p-5 rounded-2xl bg-blue-50 hover:bg-blue-100/80 border border-blue-200 flex items-center justify-between transition text-left w-full"
              >
                <div>
                  <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded bg-blue-200 text-blue-900">
                    Arquivo CSV (UTF-8 BOM)
                  </span>
                  <h3 className="text-sm font-bold text-slate-900 mt-2">Exportar Extrato CSV</h3>
                  <p className="text-xs text-slate-600 mt-1">
                    Formato universal compatível com bancos, contabilidade e reimportação.
                  </p>
                </div>
                <Download className="w-8 h-8 text-blue-600 shrink-0" />
              </button>
            </div>
          </div>
        )}

        {/* ===================================================================
            ABA 10: CENTRAL DE ALERTAS E NOTIFICACOES (RF-011, RN-004)
           =================================================================== */}
        {abaAtiva === 'notificacoes' && (
          <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h2 className="text-base font-bold text-slate-900">
                  Central de Alertas de Orçamento e Vencimentos (RF-011 & RN-004)
                </h2>
                <p className="text-xs text-slate-500">
                  Notificações gravadas no banco sobre orçamentos (80% e 100%) e avisos diários agendados de vencimento de cartões.
                </p>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={async () => {
                    await api.verificarVencimentos();
                    exibirMensagem('sucesso', 'Tarefa agendada de verificação de vencimentos executada!');
                    carregarTudo();
                  }}
                  className="px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-xs font-semibold text-slate-700"
                >
                  Disparar Verificação de Vencimentos (@Scheduled)
                </button>
                <button
                  onClick={async () => {
                    await api.marcarTodasLidas(1);
                    carregarTudo();
                  }}
                  className="px-3 py-1.5 rounded-lg bg-blue-600 text-white text-xs font-semibold"
                >
                  Marcar Todas como Lidas
                </button>
              </div>
            </div>

            <div className="space-y-2.5">
              {notificacoes.map((n) => (
                <div
                  key={n.id}
                  className={`p-4 rounded-xl border flex items-center justify-between gap-3 ${
                    n.lida ? 'bg-slate-50 border-slate-200 opacity-75' : 'bg-amber-50/50 border-amber-300'
                  }`}
                >
                  <div className="space-y-0.5">
                    <div className="flex items-center gap-2">
                      <span className="px-2 py-0.5 rounded bg-slate-800 text-white text-[10px] font-bold">
                        {n.tipo}
                      </span>
                      <span className="font-bold text-sm text-slate-900">{n.titulo}</span>
                    </div>
                    <p className="text-xs text-slate-700">{n.mensagem}</p>
                  </div>
                  {!n.lida && (
                    <button
                      onClick={async () => {
                        await api.marcarNotificacaoLida(n.id);
                        carregarTudo();
                      }}
                      className="px-3 py-1.5 rounded-lg bg-white border border-slate-300 text-xs font-semibold hover:bg-slate-100 shrink-0"
                    >
                      Marcar como Lida
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* ===================================================================
            ABA 11: SEGURANCA AES-256, LGPD E SYSADMIN (RNF-003, RNF-004, RNF-007, UC13)
           =================================================================== */}
        {abaAtiva === 'seguranca' && (
          <div className="space-y-5">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              {/* Bloco LGPD (RNF-004) */}
              <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-3">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5 text-blue-600" />
                  <h2 className="text-sm font-bold text-slate-900">
                    Privacidade e Conformidade com a LGPD (Lei nº 13.709/2018 — RNF-004)
                  </h2>
                </div>
                <p className="text-xs text-slate-600">
                  O usuário pode baixar todos os seus dados pessoais a qualquer momento (Portabilidade) ou solicitar a exclusão definitiva de sua conta (Direito ao Esquecimento).
                </p>

                <div className="flex flex-wrap gap-2 pt-1">
                  <button
                    onClick={async () => {
                      const dados = await api.exportLgpd(usuario.usuarioId);
                      setPacoteLgpd(dados);
                      exibirMensagem('sucesso', 'Pacote de Portabilidade LGPD gerado e descriptografado para o titular!');
                    }}
                    className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold"
                  >
                    Baixar Meus Dados Pessoais (Portabilidade LGPD)
                  </button>
                </div>

                {pacoteLgpd && (
                  <pre className="p-3 rounded-lg bg-slate-900 text-emerald-300 text-[11px] overflow-x-auto max-h-48">
                    {JSON.stringify(pacoteLgpd, null, 2)}
                  </pre>
                )}
              </div>

              {/* Bloco Backup Diario (RNF-007) */}
              <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-3">
                <div className="flex items-center gap-2">
                  <Database className="w-5 h-5 text-emerald-600" />
                  <h2 className="text-sm font-bold text-slate-900">
                    Rotina de Backup Diário e Disaster Recovery (RNF-007)
                  </h2>
                </div>
                <p className="text-xs text-slate-600">
                  Backups automáticos diários com retenção mínima de 30 dias. Dados sensíveis armazenados com criptografia AES-256-GCM (RNF-003).
                </p>

                <button
                  onClick={async () => {
                    const res = await api.executarBackupDiario();
                    setUltimoBackup(res);
                    exibirMensagem('sucesso', `Backup RNF-007 gerado: ${res.arquivoGerado} (Retenção: ${res.politicaRetencaoDias} dias)`);
                    carregarTudo();
                  }}
                  className="px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold"
                >
                  Executar Backup Agora (Retenção 30 Dias)
                </button>

                {ultimoBackup && (
                  <div className="p-3 rounded-lg bg-emerald-50 border border-emerald-200 text-xs text-emerald-900">
                    <div><strong>Arquivo:</strong> {ultimoBackup.arquivoGerado}</div>
                    <div><strong>Retenção:</strong> {ultimoBackup.politicaRetencaoDias} dias</div>
                    <div><strong>Backups Ativos:</strong> {ultimoBackup.backupsAtivosNoServidor?.length || 1}</div>
                  </div>
                )}
              </div>
            </div>

            {/* Bloco Sysadmin Logs (UC13) */}
            <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200">
              <div className="flex items-center justify-between mb-3">
                <div>
                  <h3 className="text-sm font-bold text-slate-900">
                    Trilha de Auditoria e Logs Técnicos de Segurança — Perfil Sysadmin (UC13 & RNF-003)
                  </h3>
                  <p className="text-xs text-slate-500">
                    Exibe eventos técnicos e de segurança sem expor saldos ou transações descriptografadas dos membros.
                  </p>
                </div>
              </div>

              <div className="max-h-64 overflow-y-auto border border-slate-200 rounded-lg">
                <table className="w-full text-xs">
                  <thead className="bg-slate-100 text-slate-700 sticky top-0">
                    <tr>
                      <th className="p-2 text-left">Data/Hora</th>
                      <th className="p-2 text-left">Nível</th>
                      <th className="p-2 text-left">Módulo</th>
                      <th className="p-2 text-left">Ação</th>
                      <th className="p-2 text-left">Usuário Mascarado</th>
                      <th className="p-2 text-left">Detalhes Técnicos</th>
                    </tr>
                  </thead>
                  <tbody>
                    {logsSysadmin.map((log) => (
                      <tr key={log.id} className="border-t border-slate-100 font-mono text-[11px]">
                        <td className="p-2 whitespace-nowrap">{String(log.dataHora).slice(0, 19).replace('T', ' ')}</td>
                        <td className="p-2">
                          <span
                            className={`px-1.5 py-0.5 rounded font-bold text-[10px] ${
                              log.nivel === 'SECURITY'
                                ? 'bg-purple-100 text-purple-800'
                                : log.nivel === 'WARN'
                                ? 'bg-amber-100 text-amber-800'
                                : 'bg-blue-100 text-blue-800'
                            }`}
                          >
                            {log.nivel}
                          </span>
                        </td>
                        <td className="p-2">{log.modulo}</td>
                        <td className="p-2 font-bold">{log.acao}</td>
                        <td className="p-2">{log.usuarioMascarado}</td>
                        <td className="p-2 font-sans text-slate-600">{log.detalhesTecnicos}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Rodape Academico UCSAL */}
      <footer className="bg-white border-t border-slate-200 py-3 px-4 mt-8 text-xs text-slate-500">
        <div className="max-w-[1600px] mx-auto flex flex-wrap items-center justify-between gap-2">
          <span>
            <strong>FinFamily v2.0</strong> — Universidade Católica do Salvador (UCSAL) • Bacharelado em Engenharia de Software
          </span>
          <span>
            Prof. Msc. Elton Figueiredo da Silva • Tópicos Avançados de Programação (II Unidade)
          </span>
        </div>
      </footer>
    </div>
  );
}
