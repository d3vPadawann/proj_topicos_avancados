const API_BASE = '/api';

function getHeaders() {
  let token = null;
  try {
    token = sessionStorage.getItem('finfamily_token'); // sessao separada por aba
  } catch {
    // navegador sem armazenamento
  }
  const headers = { 'Content-Type': 'application/json' };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

async function request(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      ...getHeaders(),
      ...(options.headers || {}),
    },
  });
  if (res.status === 401 && !path.startsWith('/auth/login')) {
    // Token ausente, invalido ou expirado: volta para a tela de login
    window.dispatchEvent(new Event('finfamily:sessao-expirada'));
  }
  if (!res.ok) {
    let errMsg = `Erro HTTP ${res.status}`;
    try {
      const errJson = await res.json();
      errMsg = errJson.mensagem || errJson.erro || errMsg;
    } catch {
      // ignore
    }
    throw new Error(errMsg);
  }
  return res.json();
}

// Baixa um arquivo da API enviando o token JWT (usado nas exportacoes PDF, Excel e CSV)
async function baixar(path, nomeArquivo) {
  const res = await fetch(`${API_BASE}${path}`, { headers: getHeaders() });
  if (res.status === 401) {
    window.dispatchEvent(new Event('finfamily:sessao-expirada'));
  }
  if (!res.ok) {
    let errMsg = `Erro HTTP ${res.status}`;
    try {
      const errJson = await res.json();
      errMsg = errJson.mensagem || errMsg;
    } catch {
      // ignore
    }
    throw new Error(errMsg);
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = nomeArquivo;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export const api = {
  baixar,
  // Autenticacao e LGPD (RF-001, RNF-004)
  login: (email, senha) =>
    request('/auth/login', { method: 'POST', body: JSON.stringify({ email, senha }) }),
  register: (payload) =>
    request('/auth/register', { method: 'POST', body: JSON.stringify(payload) }),
  getDemoUsers: () => request('/auth/demo-users'),
  exportLgpd: (usuarioId) => request(`/auth/lgpd/export/${usuarioId}`),
  deleteAccountLgpd: (usuarioId) =>
    request(`/auth/lgpd/delete/${usuarioId}`, { method: 'DELETE' }),

  // Dashboard (RF-009, RN-001)
  getDashboard: (grupoId = 1, mes = '') =>
    request(`/dashboard?grupoId=${grupoId}${mes ? `&mes=${mes}` : ''}`),

  // Contas e Cartoes (RF-002, RN-001, RN-002)
  getContas: (grupoId = 1) => request(`/contas?grupoId=${grupoId}`),
  createConta: (payload) =>
    request('/contas', { method: 'POST', body: JSON.stringify(payload) }),
  updateConta: (id, payload) =>
    request(`/contas/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deleteConta: (id) => request(`/contas/${id}`, { method: 'DELETE' }),

  // Categorias e Tags (RF-004)
  getCategoriasArvore: (grupoId = 1) => request(`/categorias?grupoId=${grupoId}`),
  getCategoriasPlanas: (grupoId = 1) => request(`/categorias/planas?grupoId=${grupoId}`),
  createCategoria: (payload) =>
    request('/categorias', { method: 'POST', body: JSON.stringify(payload) }),
  deleteCategoria: (id) => request(`/categorias/${id}`, { method: 'DELETE' }),
  getTags: (grupoId = 1) => request(`/tags?grupoId=${grupoId}`),
  createTag: (payload) =>
    request('/tags', { method: 'POST', body: JSON.stringify(payload) }),
  deleteTag: (id) => request(`/tags/${id}`, { method: 'DELETE' }),

  // Transacoes (RF-003, RN-003)
  getTransacoes: (params = {}) => {
    const q = new URLSearchParams();
    q.set('grupoId', params.grupoId || 1);
    if (params.contaId) q.set('contaId', params.contaId);
    if (params.categoriaId) q.set('categoriaId', params.categoriaId);
    if (params.tipo) q.set('tipo', params.tipo);
    if (params.busca) q.set('busca', params.busca);
    if (params.periodo) q.set('periodo', params.periodo);
    return request(`/transacoes?${q.toString()}`);
  },
  createTransacao: (payload) =>
    request('/transacoes', { method: 'POST', body: JSON.stringify(payload) }),
  deleteTransacao: (id) => request(`/transacoes/${id}`, { method: 'DELETE' }),

  // Importacao CSV (RF-005, OE2, BPMN Figura 1)
  analisarCsv: (payload) =>
    request('/importacao/analisar', { method: 'POST', body: JSON.stringify(payload) }),
  confirmarCsv: (payload) =>
    request('/importacao/confirmar', { method: 'POST', body: JSON.stringify(payload) }),

  // Orcamentos (RF-006, RN-004, OE3)
  getOrcamentos: (grupoId = 1, mes = '') =>
    request(`/orcamentos?grupoId=${grupoId}${mes ? `&mes=${mes}` : ''}`),
  saveOrcamento: (payload) =>
    request('/orcamentos', { method: 'POST', body: JSON.stringify(payload) }),
  deleteOrcamento: (id) => request(`/orcamentos/${id}`, { method: 'DELETE' }),

  // Grupo Familiar (RF-007, OE4, BPMN Figura 2)
  getMembros: (grupoId = 1) => request(`/grupo/membros?grupoId=${grupoId}`),
  convidarMembro: (payload) =>
    request('/grupo/convidar', { method: 'POST', body: JSON.stringify(payload) }),
  responderConvite: (id, aceitar) =>
    request(`/grupo/membros/${id}/resposta`, {
      method: 'POST',
      body: JSON.stringify({ aceitar }),
    }),
  alterarStatusMembro: (id, payload) =>
    request(`/grupo/membros/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }),

  // Investimentos e Proventos (RF-008, RN-005, BPMN Figura 3)
  getCarteira: (grupoId = 1) => request(`/investimentos/carteira?grupoId=${grupoId}`),
  getAtivos: () => request('/investimentos/ativos'),
  saveAtivo: (payload) =>
    request('/investimentos/ativos', { method: 'POST', body: JSON.stringify(payload) }),
  registrarOperacaoInvestimento: (payload) =>
    request('/investimentos/operacoes', { method: 'POST', body: JSON.stringify(payload) }),

  // Notificacoes (RF-011)
  getNotificacoes: (grupoId = 1) => request(`/notificacoes?grupoId=${grupoId}`),
  marcarNotificacaoLida: (id) =>
    request(`/notificacoes/${id}/lida`, { method: 'PATCH' }),
  marcarTodasLidas: (grupoId = 1) =>
    request(`/notificacoes/ler-todas?grupoId=${grupoId}`, { method: 'POST' }),
  verificarVencimentos: () =>
    request('/notificacoes/verificar-vencimentos', { method: 'POST' }),

  // Assistente Conversacional com IA (RF-012, RN-006, OE5, BPMN Figura 4)
  interpretarIA: (texto, grupoId = 1) =>
    request('/assistente/interpretar', {
      method: 'POST',
      body: JSON.stringify({ grupoId, texto }),
    }),
  executarIA: (payload) =>
    request('/assistente/executar', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  executarBenchmark50: (grupoId = 1) =>
    request(`/assistente/benchmark-50?grupoId=${grupoId}`),

  // Sysadmin & Backups (UC13, RNF-003, RNF-007)
  getLogsSysadmin: () => request('/sysadmin/logs'),
  executarBackupDiario: () => request('/sysadmin/backup', { method: 'POST' }),
};
