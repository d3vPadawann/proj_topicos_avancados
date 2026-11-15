package br.ucsal.finfamily.service;

import br.ucsal.finfamily.domain.enums.*;
import br.ucsal.finfamily.domain.model.*;
import br.ucsal.finfamily.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servico Central de Regras de Negocio Financeiras do FinFamily.
 * Implementa RF-002 a RF-009, RF-011 e as Regras de Negocio RN-001 a RN-005.
 */
@Service
public class FinanceiroService {

    private final GrupoFamiliarRepository grupoFamiliarRepository;
    private final MembroRepository membroRepository;
    private final UsuarioRepository usuarioRepository;
    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;
    private final TagRepository tagRepository;
    private final TransacaoRepository transacaoRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final AtivoRepository ativoRepository;
    private final OperacaoInvestimentoRepository operacaoInvestimentoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final AuditLogService auditLogService;

    public FinanceiroService(GrupoFamiliarRepository grupoFamiliarRepository,
                             MembroRepository membroRepository,
                             UsuarioRepository usuarioRepository,
                             ContaRepository contaRepository,
                             CategoriaRepository categoriaRepository,
                             TagRepository tagRepository,
                             TransacaoRepository transacaoRepository,
                             OrcamentoRepository orcamentoRepository,
                             AtivoRepository ativoRepository,
                             OperacaoInvestimentoRepository operacaoInvestimentoRepository,
                             NotificacaoRepository notificacaoRepository,
                             AuditLogService auditLogService) {
        this.grupoFamiliarRepository = grupoFamiliarRepository;
        this.membroRepository = membroRepository;
        this.usuarioRepository = usuarioRepository;
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
        this.tagRepository = tagRepository;
        this.transacaoRepository = transacaoRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.ativoRepository = ativoRepository;
        this.operacaoInvestimentoRepository = operacaoInvestimentoRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.auditLogService = auditLogService;
    }

    public GrupoFamiliar obterGrupoPadrao(Long grupoId) {
        if (grupoId != null) {
            Optional<GrupoFamiliar> opt = grupoFamiliarRepository.findById(grupoId);
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        return grupoFamiliarRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhum grupo familiar cadastrado."));
    }

    // =========================================================================
    // RF-002, RN-001, RN-002: GESTAO DE CONTAS E CARTOES
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Conta> listarContas(Long grupoId) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        return contaRepository.findByGrupoIdOrderByIdAsc(grupo.getId());
    }

    @Transactional
    public Conta criarOuAtualizarConta(Long grupoId, Conta dadosConta, Long proprietarioId) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        Conta conta;
        boolean isNova = (dadosConta.getId() == null);
        if (!isNova) {
            conta = contaRepository.findById(dadosConta.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada."));
        } else {
            conta = new Conta();
            conta.setGrupo(grupo);
        }

        conta.setNome(dadosConta.getNome());
        conta.setInstituicao(dadosConta.getInstituicao() != null ? dadosConta.getInstituicao() : "Instituição Financeira");
        conta.setTipo(dadosConta.getTipo() != null ? dadosConta.getTipo() : TipoConta.CORRENTE);
        conta.setSaldo(dadosConta.getSaldo() != null ? dadosConta.getSaldo() : BigDecimal.ZERO);
        conta.setLimiteCredito(dadosConta.getLimiteCredito() != null ? dadosConta.getLimiteCredito() : BigDecimal.ZERO);
        conta.setPrivada(Boolean.TRUE.equals(dadosConta.getPrivada()));
        conta.setDiaVencimento(dadosConta.getDiaVencimento());
        if (dadosConta.getCodigoIdentificadorSensivel() != null) {
            conta.setCodigoIdentificadorSensivel(dadosConta.getCodigoIdentificadorSensivel());
        }

        if (proprietarioId != null) {
            usuarioRepository.findById(proprietarioId).ifPresent(conta::setProprietario);
        } else if (conta.getProprietario() == null) {
            conta.setProprietario(grupo.getTitular());
        }

        Conta salva = contaRepository.save(conta);
        auditLogService.registrar(isNova ? "CRIAR_CONTA" : "ATUALIZAR_CONTA", "CONTAS",
                salva.getProprietario() != null ? salva.getProprietario().getEmail() : "titular@finfamily.com",
                "INFO", "Conta '" + salva.getNome() + "' (" + salva.getTipo() + "), privada=" + salva.getPrivada());
        return salva;
    }

    /**
     * RN-002: Restricao de Exclusao de Contas.
     * Uma conta nao pode ser excluida se possuir saldo diferente de zero.
     */
    @Transactional
    public void excluirConta(Long contaId) {
        Conta conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada."));

        if (!conta.podeSerExcluida()) {
            throw new IllegalStateException(
                    String.format("RN-002 Violada: A conta '%s' não pode ser excluída pois possui saldo diferente de zero (Saldo atual: R$ %.2f).",
                            conta.getNome(), conta.getSaldo()));
        }

        List<Transacao> transacoesConta = transacaoRepository.findByContaIdOrderByDataDescIdDesc(contaId);
        if (!transacoesConta.isEmpty()) {
            transacaoRepository.deleteAll(transacoesConta);
        }

        contaRepository.delete(conta);
        auditLogService.registrar("EXCLUIR_CONTA", "CONTAS", "titular@finfamily.com", "INFO",
                "Conta '" + conta.getNome() + "' excluida com saldo R$ 0,00 (RN-002 atendida).");
    }

    // =========================================================================
    // RF-004: CATEGORIAS (ARVORE HIERARQUICA) E TAGS
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarCategoriasEmArvore(Long grupoId) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        List<Categoria> todas = categoriaRepository.findByGrupoIdOrderByNomeAsc(grupo.getId());
        List<Categoria> raizes = todas.stream().filter(c -> c.getPai() == null).toList();

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Categoria raiz : raizes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", raiz.getId());
            item.put("nome", raiz.getNome());
            item.put("caminhoCompleto", raiz.getCaminhoCompleto());
            item.put("cor", raiz.getCor());
            item.put("icone", raiz.getIcone());
            item.put("tipoPadrao", raiz.getTipoPadrao());
            item.put("paiId", null);

            List<Map<String, Object>> subcats = todas.stream()
                    .filter(sub -> sub.getPai() != null && Objects.equals(sub.getPai().getId(), raiz.getId()))
                    .map(sub -> {
                        Map<String, Object> s = new LinkedHashMap<>();
                        s.put("id", sub.getId());
                        s.put("nome", sub.getNome());
                        s.put("caminhoCompleto", sub.getCaminhoCompleto());
                        s.put("cor", sub.getCor());
                        s.put("icone", sub.getIcone());
                        s.put("tipoPadrao", sub.getTipoPadrao());
                        s.put("paiId", raiz.getId());
                        s.put("paiNome", raiz.getNome());
                        return s;
                    }).toList();

            item.put("subcategorias", subcats);
            resultado.add(item);
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<Categoria> listarTodasCategoriasPlanas(Long grupoId) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        return categoriaRepository.findByGrupoIdOrderByNomeAsc(grupo.getId());
    }

    @Transactional
    public Categoria criarCategoria(Long grupoId, String nome, String cor, String icone, TipoTransacao tipoPadrao, Long paiId) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        Categoria pai = null;
        if (paiId != null) {
            pai = categoriaRepository.findById(paiId).orElse(null);
        }
        Categoria nova = new Categoria(
                nome,
                cor != null ? cor : (pai != null ? pai.getCor() : "#3B82F6"),
                icone != null ? icone : "tag",
                tipoPadrao != null ? tipoPadrao : TipoTransacao.DESPESA,
                pai,
                grupo
        );
        return categoriaRepository.save(nova);
    }

    @Transactional
    public void excluirCategoria(Long categoriaId) {
        List<Categoria> subcategorias = categoriaRepository.findByPaiIdOrderByNomeAsc(categoriaId);
        for (Categoria sub : subcategorias) {
            sub.setPai(null);
            categoriaRepository.save(sub);
        }
        categoriaRepository.deleteById(categoriaId);
    }

    @Transactional(readOnly = true)
    public List<Tag> listarTags(Long grupoId) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        return tagRepository.findByGrupoIdOrderByNomeAsc(grupo.getId());
    }

    @Transactional
    public Tag criarTag(Long grupoId, String nome, String cor) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        Tag tag = new Tag(nome, cor != null ? cor : "#6366F1", grupo);
        return tagRepository.save(tag);
    }

    @Transactional
    public void excluirTag(Long tagId) {
        tagRepository.deleteById(tagId);
    }

    // =========================================================================
    // RF-003 e RN-003: LANCAMENTO DE TRANSACOES (COM PARCELAMENTO E RECORRENCIA)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Transacao> listarTransacoes(Long grupoId, Long contaId, Long categoriaId, String tipoStr, String busca, String periodo) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        List<Transacao> lista = transacaoRepository.findByContaGrupoIdOrderByDataDescIdDesc(grupo.getId());

        return lista.stream().filter(t -> {
            if (contaId != null && !Objects.equals(t.getConta().getId(), contaId)) {
                return false;
            }
            if (categoriaId != null) {
                if (t.getCategoria() == null) return false;
                boolean matchDireto = Objects.equals(t.getCategoria().getId(), categoriaId);
                boolean matchPai = t.getCategoria().getPai() != null && Objects.equals(t.getCategoria().getPai().getId(), categoriaId);
                if (!matchDireto && !matchPai) return false;
            }
            if (tipoStr != null && !tipoStr.isBlank() && !tipoStr.equalsIgnoreCase("TODOS")) {
                if (!t.getTipo().name().equalsIgnoreCase(tipoStr)) return false;
            }
            if (busca != null && !busca.isBlank()) {
                String termo = busca.trim().toLowerCase();
                boolean matchDesc = t.getDescricao() != null && t.getDescricao().toLowerCase().contains(termo);
                boolean matchCat = t.getCategoria() != null && t.getCategoria().getNome().toLowerCase().contains(termo);
                boolean matchConta = t.getConta() != null && t.getConta().getNome().toLowerCase().contains(termo);
                if (!matchDesc && !matchCat && !matchConta) return false;
            }
            if (periodo != null && !periodo.isBlank() && !periodo.equalsIgnoreCase("todos")) {
                LocalDate hoje = LocalDate.now();
                if ("hoje".equalsIgnoreCase(periodo) && !t.getData().isEqual(hoje)) {
                    return false;
                } else if ("semana_atual".equalsIgnoreCase(periodo)) {
                    LocalDate inicioSemana = hoje.minusDays(7);
                    if (t.getData().isBefore(inicioSemana) || t.getData().isAfter(hoje.plusDays(1))) return false;
                } else if ("mes_atual".equalsIgnoreCase(periodo)) {
                    YearMonth ym = YearMonth.from(hoje);
                    if (!YearMonth.from(t.getData()).equals(ym)) return false;
                } else if ("mes_passado".equalsIgnoreCase(periodo)) {
                    YearMonth ymPassado = YearMonth.from(hoje).minusMonths(1);
                    if (!YearMonth.from(t.getData()).equals(ymPassado)) return false;
                } else if ("ano_atual".equalsIgnoreCase(periodo)) {
                    if (t.getData().getYear() != hoje.getYear()) return false;
                } else if (periodo.matches("\\d{4}-\\d{2}")) {
                    YearMonth ymFiltro = YearMonth.parse(periodo);
                    if (!YearMonth.from(t.getData()).equals(ymFiltro)) return false;
                }
            }
            return true;
        }).toList();
    }

    @Transactional
    public List<Transacao> registrarTransacao(Long grupoId,
                                              String descricao,
                                              BigDecimal valor,
                                              LocalDate data,
                                              TipoTransacao tipo,
                                              Long contaId,
                                              Long contaDestinoId,
                                              Long categoriaId,
                                              List<Long> tagIds,
                                              Integer totalParcelas,
                                              Boolean recorrente,
                                              String frequenciaRecorrencia,
                                              Long autorId,
                                              String origemLancamento) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);

        Conta conta;
        if (contaId != null) {
            conta = contaRepository.findById(contaId)
                    .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada."));
        } else {
            conta = contaRepository.findByGrupoIdOrderByIdAsc(grupo.getId()).stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Grupo não possui contas cadastradas."));
        }

        Conta contaDestino = null;
        if (contaDestinoId != null) {
            contaDestino = contaRepository.findById(contaDestinoId).orElse(null);
        }

        Categoria categoria = null;
        if (categoriaId != null) {
            categoria = categoriaRepository.findById(categoriaId).orElse(null);
        }
        if (categoria == null) {
            categoria = categoriaRepository.findByGrupoIdOrderByNomeAsc(grupo.getId()).stream()
                    .findFirst().orElse(null);
        }

        Set<Tag> tags = new HashSet<>();
        if (tagIds != null && !tagIds.isEmpty()) {
            tags.addAll(tagRepository.findAllById(tagIds));
        }

        Usuario autor = null;
        if (autorId != null) {
            autor = usuarioRepository.findById(autorId).orElse(grupo.getTitular());
        } else {
            autor = grupo.getTitular();
        }

        Transacao base = new Transacao();
        base.setDescricao(descricao != null && !descricao.isBlank() ? descricao : "Lançamento Financeiro");
        base.setValor(valor != null ? valor.abs() : BigDecimal.ZERO);
        base.setData(data != null ? data : LocalDate.now());
        base.setTipo(tipo != null ? tipo : TipoTransacao.DESPESA);
        base.setTotalParcelas(totalParcelas != null && totalParcelas > 1 ? totalParcelas : 1);
        base.setRecorrente(Boolean.TRUE.equals(recorrente));
        base.setFrequenciaRecorrencia(Boolean.TRUE.equals(recorrente) ? (frequenciaRecorrencia != null ? frequenciaRecorrencia : "MENSAL") : null);
        base.setOrigemLancamento(origemLancamento != null ? origemLancamento : "MANUAL");
        base.setConta(conta);
        base.setContaDestino(contaDestino);
        base.setCategoria(categoria);
        base.setTags(tags);
        base.setAutor(autor);

        // RN-003: Divide automaticamente o valor total se totalParcelas > 1 e projeta lancamentos futuros
        List<Transacao> lancamentosParaSalvar = base.gerarParcelas();
        List<Transacao> salvas = new ArrayList<>();
        Set<String> mesesAfetados = new HashSet<>();

        for (Transacao t : lancamentosParaSalvar) {
            if (t.getHashConciliacao() == null) {
                t.setHashConciliacao(Conta.gerarHashConciliacao(
                        conta.getId(), t.getData(), t.getValor(), t.getDescricao() + "|" + UUID.randomUUID()));
            }
            Transacao salva = transacaoRepository.save(t);
            salvas.add(salva);
            mesesAfetados.add(YearMonth.from(t.getData()).toString());

            // Atualiza saldo se for na data atual/passada ou primeira parcela
            if (t.getParcela() == null || t.getParcela() == 1) {
                aplicarImpactoSaldo(conta, contaDestino, t.getTipo(), t.getValor());
            }
        }

        // Verifica e atualiza orcamentos dos meses afetados (RN-004)
        for (String mes : mesesAfetados) {
            recalcularOrcamentosDoMes(grupo.getId(), mes);
        }

        return salvas;
    }

    private void aplicarImpactoSaldo(Conta conta, Conta contaDestino, TipoTransacao tipo, BigDecimal valor) {
        if (conta == null || valor == null) return;
        BigDecimal saldoAtual = conta.getSaldo() != null ? conta.getSaldo() : BigDecimal.ZERO;

        if (tipo == TipoTransacao.RECEITA) {
            conta.setSaldo(saldoAtual.add(valor));
            contaRepository.save(conta);
        } else if (tipo == TipoTransacao.DESPESA) {
            conta.setSaldo(saldoAtual.subtract(valor));
            contaRepository.save(conta);
        } else if (tipo == TipoTransacao.TRANSFERENCIA) {
            conta.setSaldo(saldoAtual.subtract(valor));
            contaRepository.save(conta);
            if (contaDestino != null) {
                BigDecimal saldoDest = contaDestino.getSaldo() != null ? contaDestino.getSaldo() : BigDecimal.ZERO;
                contaDestino.setSaldo(saldoDest.add(valor));
                contaRepository.save(contaDestino);
            }
        }
    }

    @Transactional
    public void excluirTransacao(Long transacaoId) {
        Transacao t = transacaoRepository.findById(transacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada."));
        Conta conta = t.getConta();
        Long grupoId = conta.getGrupo().getId();
        String mes = YearMonth.from(t.getData()).toString();

        // Reverte impacto no saldo
        if (t.getParcela() == null || t.getParcela() == 1) {
            if (t.getTipo() == TipoTransacao.RECEITA) {
                conta.setSaldo(conta.getSaldo().subtract(t.getValor()));
            } else if (t.getTipo() == TipoTransacao.DESPESA) {
                conta.setSaldo(conta.getSaldo().add(t.getValor()));
            }
            contaRepository.save(conta);
        }

        transacaoRepository.delete(t);
        recalcularOrcamentosDoMes(grupoId, mes);
    }

    // =========================================================================
    // RF-005 e OE2: IMPORTACAO DE EXTRATOS CSV COM DEDUPLICACAO (BPMN FIGURA 1)
    // =========================================================================

    /**
     * Etapa 1 do BPMN Figura 1 e UC05:
     * Valida o arquivo CSV, descarta linhas ja importadas anteriormente (OE2) e devolve
     * as transacoes para revisao de categoria pelo membro antes da confirmacao.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> analisarArquivoCSV(Long grupoId, Long contaId, String conteudoCsv) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        Conta conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta de destino não encontrada."));

        if (conteudoCsv == null || conteudoCsv.isBlank()) {
            throw new IllegalArgumentException("Arquivo CSV vazio ou inválido.");
        }

        List<Categoria> categorias = categoriaRepository.findByGrupoIdOrderByNomeAsc(grupo.getId());
        List<Map<String, Object>> linhasValidasParaRevisao = new ArrayList<>();
        List<Map<String, Object>> linhasDuplicadasDescartadas = new ArrayList<>();
        Set<String> hashesVistosNoArquivo = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(conteudoCsv.trim()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("Arquivo CSV inválido: cabeçalho ausente.");
            }

            String separador = headerLine.contains(";") ? ";" : ",";
            String headerLower = headerLine.toLowerCase();
            if (!headerLower.contains("data") || (!headerLower.contains("descri") && !headerLower.contains("historico")) || !headerLower.contains("valor")) {
                throw new IllegalArgumentException(
                        "Fluxo Alternativo 2a (UC05): Arquivo CSV inválido. As colunas obrigatórias (data, descricao, valor) não foram encontradas.");
            }

            String linha;
            int numLinha = 1;
            while ((linha = reader.readLine()) != null) {
                numLinha++;
                if (linha.isBlank()) continue;

                String[] partes = linha.split(separador);
                if (partes.length < 3) {
                    throw new IllegalArgumentException("Linha " + numLinha + " inválida no CSV: esperado data, descrição e valor.");
                }

                LocalDate data = parseDataFlexivel(partes[0].trim());
                String descricao = partes[1].trim().replace("\"", "");
                BigDecimal valorBruto = parseValorFlexivel(partes[2].trim());
                TipoTransacao tipo = valorBruto.compareTo(BigDecimal.ZERO) < 0 ? TipoTransacao.DESPESA : TipoTransacao.DESPESA;
                if (partes.length >= 4 && partes[3].trim().equalsIgnoreCase("RECEITA")) {
                    tipo = TipoTransacao.RECEITA;
                } else if (descricao.toLowerCase().contains("salário") || descricao.toLowerCase().contains("salario")
                        || descricao.toLowerCase().contains("pix recebido") || descricao.toLowerCase().contains("provento")
                        || descricao.toLowerCase().contains("dividendo")) {
                    tipo = TipoTransacao.RECEITA;
                }

                BigDecimal valorAbs = valorBruto.abs().setScale(2, RoundingMode.HALF_UP);
                String hash = Conta.gerarHashConciliacao(conta.getId(), data, valorAbs, descricao);

                Categoria catSugerida = sugerirCategoriaPorDescricao(descricao, categorias);

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("linha", numLinha);
                item.put("data", data.toString());
                item.put("descricao", descricao);
                item.put("valor", valorAbs);
                item.put("tipo", tipo.name());
                item.put("categoriaId", catSugerida != null ? catSugerida.getId() : null);
                item.put("categoriaNome", catSugerida != null ? catSugerida.getNome() : "Geral");
                item.put("hashConciliacao", hash);

                boolean jaExisteNoBanco = transacaoRepository.existsByHashConciliacao(hash);
                boolean duplicadaNoProprioArquivo = !hashesVistosNoArquivo.add(hash);

                if (jaExisteNoBanco || duplicadaNoProprioArquivo) {
                    item.put("motivoDescarte", jaExisteNoBanco ? "Já importada anteriormente no banco" : "Linha duplicada no mesmo arquivo");
                    linhasDuplicadasDescartadas.add(item);
                } else {
                    linhasValidasParaRevisao.add(item);
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao validar arquivo CSV: " + e.getMessage(), e);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("contaId", conta.getId());
        resultado.put("contaNome", conta.getNome());
        resultado.put("totalLinhasLidas", linhasValidasParaRevisao.size() + linhasDuplicadasDescartadas.size());
        resultado.put("quantidadeNovas", linhasValidasParaRevisao.size());
        resultado.put("quantidadeDuplicadasDescartadas", linhasDuplicadasDescartadas.size());
        resultado.put("transacoesParaRevisao", linhasValidasParaRevisao);
        resultado.put("duplicadasDescartadas", linhasDuplicadasDescartadas);
        return resultado;
    }

    /**
     * Etapa 2 do BPMN Figura 1 e UC05:
     * Confirma a importacao das transacoes revisadas (ou importa direto do CSV),
     * garantindo 0 duplicidades (OE2) e disparando alertas de orcamento (RN-004).
     */
    @Transactional
    public Map<String, Object> confirmarImportacaoCSV(Long grupoId, Long contaId, List<Map<String, Object>> transacoesConfirmadas, Long autorId) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        Conta conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta de destino não encontrada."));

        int gravadas = 0;
        int descartadasPorDuplicidade = 0;
        Set<String> mesesAfetados = new HashSet<>();
        List<Transacao> salvas = new ArrayList<>();

        Usuario autor = autorId != null ? usuarioRepository.findById(autorId).orElse(grupo.getTitular()) : grupo.getTitular();

        for (Map<String, Object> item : transacoesConfirmadas) {
            LocalDate data = LocalDate.parse(String.valueOf(item.get("data")));
            String descricao = String.valueOf(item.get("descricao"));
            BigDecimal valor = new BigDecimal(String.valueOf(item.get("valor"))).abs().setScale(2, RoundingMode.HALF_UP);
            String tipoStr = item.get("tipo") != null ? String.valueOf(item.get("tipo")) : "DESPESA";
            TipoTransacao tipo = TipoTransacao.valueOf(tipoStr.toUpperCase());

            String hash = Conta.gerarHashConciliacao(conta.getId(), data, valor, descricao);
            if (transacaoRepository.existsByHashConciliacao(hash)) {
                descartadasPorDuplicidade++;
                continue;
            }

            Categoria categoria = null;
            if (item.get("categoriaId") != null) {
                Long catId = Long.valueOf(String.valueOf(item.get("categoriaId")));
                categoria = categoriaRepository.findById(catId).orElse(null);
            }
            if (categoria == null) {
                categoria = sugerirCategoriaPorDescricao(descricao, categoriaRepository.findByGrupoIdOrderByNomeAsc(grupo.getId()));
            }

            Transacao t = new Transacao();
            t.setConta(conta);
            t.setData(data);
            t.setDescricao(descricao);
            t.setValor(valor);
            t.setTipo(tipo);
            t.setParcela(1);
            t.setTotalParcelas(1);
            t.setRecorrente(false);
            t.setHashConciliacao(hash);
            t.setOrigemLancamento("CSV");
            t.setCategoria(categoria);
            t.setAutor(autor);

            salvas.add(transacaoRepository.save(t));
            aplicarImpactoSaldo(conta, null, tipo, valor);
            mesesAfetados.add(YearMonth.from(data).toString());
            gravadas++;
        }

        List<Notificacao> alertasDisparados = new ArrayList<>();
        for (String mes : mesesAfetados) {
            alertasDisparados.addAll(recalcularOrcamentosDoMes(grupo.getId(), mes));
        }

        auditLogService.registrar("IMPORTACAO_CSV", "IMPORTACAO",
                autor != null ? autor.getEmail() : "titular@finfamily.com",
                "INFO", "Importadas " + gravadas + " transacoes na conta '" + conta.getNome() + "', duplicadas descartadas=" + descartadasPorDuplicidade);

        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("contaId", conta.getId());
        resumo.put("transacoesGravadas", gravadas);
        resumo.put("duplicadasDescartadas", descartadasPorDuplicidade);
        resumo.put("alertasOrcamentoGerados", alertasDisparados.size());
        resumo.put("alertas", alertasDisparados);
        resumo.put("transacoes", salvas);
        return resumo;
    }

    @Transactional
    public Map<String, Object> importarArquivoCSVDireto(Long grupoId, Long contaId, String conteudoCsv, Long autorId) {
        Map<String, Object> analise = analisarArquivoCSV(grupoId, contaId, conteudoCsv);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> paraRevisao = (List<Map<String, Object>>) analise.get("transacoesParaRevisao");
        int descartadasNaAnalise = (int) analise.get("quantidadeDuplicadasDescartadas");

        Map<String, Object> confirmacao = confirmarImportacaoCSV(grupoId, contaId, paraRevisao, autorId);
        int descartadasNaConfirmacao = (int) confirmacao.get("duplicadasDescartadas");
        confirmacao.put("duplicadasDescartadas", descartadasNaAnalise + descartadasNaConfirmacao);
        return confirmacao;
    }

    private LocalDate parseDataFlexivel(String str) {
        String s = str.replace("\"", "").trim();
        if (s.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return LocalDate.parse(s);
        }
        if (s.matches("\\d{2}/\\d{2}/\\d{4}")) {
            return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return LocalDate.now();
    }

    private BigDecimal parseValorFlexivel(String str) {
        String s = str.replace("\"", "").replace("R$", "").trim();
        if (s.contains(",") && s.contains(".")) {
            s = s.replace(".", "").replace(",", ".");
        } else if (s.contains(",")) {
            s = s.replace(",", ".");
        }
        return new BigDecimal(s);
    }

    public Categoria sugerirCategoriaPorDescricao(String descricao, List<Categoria> categorias) {
        if (categorias == null || categorias.isEmpty()) return null;
        String d = descricao != null ? descricao.toLowerCase() : "";

        for (Categoria c : categorias) {
            String n = c.getNome().toLowerCase();
            if ((d.contains("padaria") || d.contains("pao") || d.contains("pão") || d.contains("restaurante") || d.contains("ifood"))
                    && (n.contains("padaria") || n.contains("alimenta"))) {
                return c;
            }
            if ((d.contains("mercado") || d.contains("supermercado") || d.contains("atacadao") || d.contains("assai"))
                    && (n.contains("supermercado") || n.contains("alimenta"))) {
                return c;
            }
            if ((d.contains("uber") || d.contains("99") || d.contains("gasolina") || d.contains("posto") || d.contains("combustivel"))
                    && (n.contains("transporte") || n.contains("mobilidade"))) {
                return c;
            }
            if ((d.contains("aluguel") || d.contains("condominio") || d.contains("energia") || d.contains("coelba") || d.contains("internet"))
                    && (n.contains("moradia") || n.contains("casa"))) {
                return c;
            }
            if ((d.contains("faculdade") || d.contains("ucsal") || d.contains("curso") || d.contains("livro"))
                    && (n.contains("educa"))) {
                return c;
            }
            if ((d.contains("farmacia") || d.contains("plano de saude") || d.contains("consulta") || d.contains("academia"))
                    && (n.contains("saúde") || n.contains("saude"))) {
                return c;
            }
            if ((d.contains("netflix") || d.contains("spotify") || d.contains("cinema") || d.contains("viagem") || d.contains("lazer"))
                    && (n.contains("lazer") || n.contains("assinatura"))) {
                return c;
            }
            if ((d.contains("salario") || d.contains("salário") || d.contains("provento") || d.contains("dividendo") || d.contains("freela"))
                    && (n.contains("receita") || n.contains("salário") || n.contains("provento"))) {
                return c;
            }
        }
        return categorias.stream()
                .filter(c -> c.getNome().toLowerCase().contains("alimenta"))
                .findFirst()
                .orElse(categorias.get(0));
    }

    // =========================================================================
    // RF-006, RN-004 e OE3: ORCAMENTOS MENSAIS E GATILHOS DE ALERTA (80% E 100%)
    // =========================================================================

    @Transactional
    public List<Orcamento> listarOrcamentos(Long grupoId, String mes) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        String mesFiltro = (mes != null && !mes.isBlank()) ? mes : YearMonth.now().toString();
        recalcularOrcamentosDoMes(grupo.getId(), mesFiltro);
        return orcamentoRepository.findByGrupoIdAndMesOrderByIdAsc(grupo.getId(), mesFiltro);
    }

    @Transactional
    public Orcamento definirOrcamento(Long grupoId, Long categoriaId, String mes, BigDecimal valorPlanejado) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));
        String mesAlvo = (mes != null && !mes.isBlank()) ? mes : YearMonth.now().toString();

        Orcamento orcamento = orcamentoRepository.findByGrupoIdAndCategoriaIdAndMes(grupo.getId(), categoriaId, mesAlvo)
                .orElseGet(() -> {
                    Orcamento novo = new Orcamento();
                    novo.setGrupo(grupo);
                    novo.setCategoria(categoria);
                    novo.setMes(mesAlvo);
                    return novo;
                });

        orcamento.setValorPlanejado(valorPlanejado);
        // Reavalia flags de alerta caso o limite tenha sido alterado
        orcamento.setAlerta80Disparado(false);
        orcamento.setAlerta100Disparado(false);
        orcamento = orcamentoRepository.save(orcamento);

        recalcularOrcamentosDoMes(grupo.getId(), mesAlvo);
        return orcamentoRepository.findById(orcamento.getId()).orElse(orcamento);
    }

    @Transactional
    public void excluirOrcamento(Long orcamentoId) {
        orcamentoRepository.deleteById(orcamentoId);
    }

    /**
     * Recalcula os gastos de todos os orcamentos do grupo naquele mes e dispara notificacoes
     * quando atingir 80% e 100% do valor planejado (RN-004 / OE3).
     */
    @Transactional
    public List<Notificacao> recalcularOrcamentosDoMes(Long grupoId, String mes) {
        List<Orcamento> orcamentos = orcamentoRepository.findByGrupoIdAndMesOrderByIdAsc(grupoId, mes);
        if (orcamentos.isEmpty()) return Collections.emptyList();

        YearMonth ym = YearMonth.parse(mes);
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        List<Transacao> despesasMes = transacaoRepository.findByContaGrupoIdAndTipoAndDataBetween(
                grupoId, TipoTransacao.DESPESA, inicio, fim);

        List<Notificacao> notificacoesCriadas = new ArrayList<>();

        for (Orcamento orc : orcamentos) {
            Long catId = orc.getCategoria().getId();
            BigDecimal somaCategoria = despesasMes.stream()
                    .filter(t -> {
                        if (t.getCategoria() == null) return false;
                        boolean mesmaCat = Objects.equals(t.getCategoria().getId(), catId);
                        boolean subCatDaCategoria = t.getCategoria().getPai() != null
                                && Objects.equals(t.getCategoria().getPai().getId(), catId);
                        return mesmaCat || subCatDaCategoria;
                    })
                    .map(Transacao::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            orc.setValorGasto(somaCategoria);
            List<Notificacao> alertas = orc.verificarAlerta();
            orcamentoRepository.save(orc);

            for (Notificacao alerta : alertas) {
                notificacoesCriadas.add(notificacaoRepository.save(alerta));
            }
        }
        return notificacoesCriadas;
    }

    // =========================================================================
    // RF-007 e OE4: GESTAO DO GRUPO FAMILIAR (BPMN FIGURA 2)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Membro> listarMembros(Long grupoId) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        return membroRepository.findByGrupoIdOrderByIdAsc(grupo.getId());
    }

    @Transactional
    public Membro convidarMembro(Long grupoId, String nomeConvidado, String emailConvidado, PapelFamiliar papel) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        String emailNorm = emailConvidado.trim().toLowerCase();

        Optional<Membro> existente = membroRepository.findByGrupoIdAndEmailConvidadoIgnoreCase(grupo.getId(), emailNorm);
        if (existente.isPresent()) {
            throw new IllegalArgumentException("Este e-mail já faz parte ou já foi convidado para o grupo familiar.");
        }

        Usuario usuarioAssociado = usuarioRepository.findByEmailIgnoreCase(emailNorm).orElse(null);
        Membro novoMembro = grupo.convidar(emailNorm, nomeConvidado, papel, usuarioAssociado);
        novoMembro = membroRepository.save(novoMembro);

        Notificacao notif = new Notificacao();
        notif.setGrupo(grupo);
        notif.setTipo(TipoNotificacao.CONVITE);
        notif.setTitulo("Convite Familiar Enviado");
        notif.setMensagem("Convite enviado para " + novoMembro.getNomeConvidado() + " (" + emailNorm + ") com o perfil " + novoMembro.getPapel() + ".");
        notificacaoRepository.save(notif);

        auditLogService.registrar("CONVIDAR_MEMBRO", "GRUPO_FAMILIAR", emailNorm, "INFO",
                "Membro convidado com papel " + novoMembro.getPapel() + " e status CONVIDADO.");

        return novoMembro;
    }

    @Transactional
    public Membro responderConvite(Long membroId, boolean aceitar) {
        Membro membro = membroRepository.findById(membroId)
                .orElseThrow(() -> new IllegalArgumentException("Convite de membro não encontrado."));

        if (aceitar) {
            membro.aceitarConvite();
            Notificacao notif = new Notificacao();
            notif.setGrupo(membro.getGrupo());
            notif.setTipo(TipoNotificacao.CONVITE);
            notif.setTitulo("Novo Membro Ativo no Grupo");
            notif.setMensagem(membro.getNomeConvidado() + " aceitou o convite e agora está ATIVO no grupo familiar com perfil " + membro.getPapel() + ".");
            notificacaoRepository.save(notif);
        } else {
            membro.recusarConvite();
        }
        return membroRepository.save(membro);
    }

    @Transactional
    public Membro alterarStatusMembro(Long membroId, StatusMembro novoStatus, PapelFamiliar novoPapel) {
        Membro membro = membroRepository.findById(membroId)
                .orElseThrow(() -> new IllegalArgumentException("Membro não encontrado."));

        if (membro.getPapel() == PapelFamiliar.TITULAR && novoStatus == StatusMembro.SUSPENSO) {
            throw new IllegalStateException("O Administrador Familiar (Titular) não pode ser suspenso.");
        }

        if (novoStatus != null) {
            if (novoStatus == StatusMembro.SUSPENSO) {
                membro.suspender();
            } else if (novoStatus == StatusMembro.ATIVO) {
                membro.reativar();
            } else {
                membro.setStatus(novoStatus);
            }
        }
        if (novoPapel != null) {
            membro.setPapel(novoPapel);
        }

        auditLogService.registrar("ALTERAR_MEMBRO", "GRUPO_FAMILIAR", membro.getEmailConvidado(), "SECURITY",
                "Status atualizado para " + membro.getStatus() + " e papel para " + membro.getPapel());

        return membroRepository.save(membro);
    }

    // =========================================================================
    // RF-008 e RN-005: INVESTIMENTOS E PROVENTOS (BPMN FIGURA 3)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Ativo> listarAtivos() {
        return ativoRepository.findAll();
    }

    @Transactional
    public Ativo cadastrarOuAtualizarAtivo(String ticker, String nome, BigDecimal precoAtual, String tipoAtivo, String setor) {
        String tickerNorm = ticker.trim().toUpperCase();
        Ativo ativo = ativoRepository.findByTickerIgnoreCase(tickerNorm)
                .orElseGet(Ativo::new);
        ativo.setTicker(tickerNorm);
        ativo.setNome(nome != null && !nome.isBlank() ? nome : tickerNorm);
        ativo.setPrecoAtual(precoAtual != null ? precoAtual : BigDecimal.ZERO);
        ativo.setTipoAtivo(tipoAtivo != null && !tipoAtivo.isBlank() ? tipoAtivo : "ACAO");
        ativo.setSetor(setor != null ? setor : "Mercado Financeiro");
        return ativoRepository.save(ativo);
    }

    /**
     * BPMN Figura 3, RF-008 e RN-005:
     * Registra Compra, Venda ou Provento de um ativo identificado por ticker.
     * Quando for PROVENTO, gera automaticamente uma Transacao de RECEITA na conta de destino (RN-005).
     */
    @Transactional
    public OperacaoInvestimento registrarOperacaoInvestimento(Long grupoId,
                                                              String ticker,
                                                              TipoOperacao tipo,
                                                              BigDecimal quantidade,
                                                              BigDecimal precoUnitario,
                                                              LocalDate data,
                                                              Long contaCorretoraId,
                                                              Long contaDestinoProventoId,
                                                              String nomeAtivoOpcional) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        String tickerNorm = ticker.trim().toUpperCase();

        Ativo ativo = ativoRepository.findByTickerIgnoreCase(tickerNorm)
                .orElseGet(() -> {
                    if (nomeAtivoOpcional != null && !nomeAtivoOpcional.isBlank()) {
                        return cadastrarOuAtualizarAtivo(tickerNorm, nomeAtivoOpcional, precoUnitario, "ACAO", "Renda Variável");
                    }
                    throw new IllegalArgumentException("Ticker '" + tickerNorm + "' não encontrado no cadastro de ativos. Cadastre o ativo ou informe seu nome.");
                });

        Conta contaCorretora;
        if (contaCorretoraId != null) {
            contaCorretora = contaRepository.findById(contaCorretoraId)
                    .orElseThrow(() -> new IllegalArgumentException("Conta corretora não encontrada."));
        } else {
            List<Conta> corretoras = contaRepository.findByGrupoIdAndTipo(grupo.getId(), TipoConta.CORRETORA);
            contaCorretora = !corretoras.isEmpty() ? corretoras.get(0)
                    : contaRepository.findByGrupoIdOrderByIdAsc(grupo.getId()).get(0);
        }

        Conta contaDestino = contaCorretora;
        if (contaDestinoProventoId != null) {
            contaDestino = contaRepository.findById(contaDestinoProventoId).orElse(contaCorretora);
        }

        OperacaoInvestimento op = new OperacaoInvestimento();
        op.setAtivo(ativo);
        op.setTipo(tipo);
        op.setQuantidade(quantidade != null ? quantidade : BigDecimal.ONE);
        op.setPreco(precoUnitario != null ? precoUnitario : ativo.getPrecoAtual());
        op.setData(data != null ? data : LocalDate.now());
        op.setContaCorretora(contaCorretora);
        op.setContaDestinoProvento(contaDestino);

        BigDecimal valorOperacao = op.valorTotal();

        if (tipo == TipoOperacao.COMPRA) {
            ativo.setPrecoAtual(op.getPreco());
            ativoRepository.save(ativo);
        } else if (tipo == TipoOperacao.VENDA) {
            ativo.setPrecoAtual(op.getPreco());
            ativoRepository.save(ativo);
        } else if (tipo == TipoOperacao.PROVENTO) {
            // RN-005: Tratamento de Proventos - Dividendos lancados no modulo de investimentos
            // devem ser refletidos automaticamente como "Receitas" na conta de destino.
            Categoria catProventos = categoriaRepository.findFirstByGrupoIdAndNomeIgnoreCase(grupo.getId(), "Proventos e Dividendos")
                    .orElseGet(() -> criarCategoria(grupo.getId(), "Proventos e Dividendos", "#10B981", "trending-up", TipoTransacao.RECEITA, null));

            List<Transacao> geradas = registrarTransacao(
                    grupo.getId(),
                    "Provento / Dividendo " + ativo.getTicker() + " (" + op.getQuantidade().stripTrailingZeros().toPlainString() + " cotas)",
                    valorOperacao,
                    op.getData(),
                    TipoTransacao.RECEITA,
                    contaDestino.getId(),
                    null,
                    catProventos.getId(),
                    null,
                    1,
                    false,
                    null,
                    grupo.getTitular() != null ? grupo.getTitular().getId() : null,
                    "PROVENTO"
            );
            if (!geradas.isEmpty()) {
                op.setTransacaoGerada(geradas.get(0));
            }
        }

        return operacaoInvestimentoRepository.save(op);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> consultarCarteiraInvestimentos(Long grupoId) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        List<OperacaoInvestimento> operacoes = operacaoInvestimentoRepository.findByContaCorretoraGrupoIdOrderByDataDescIdDesc(grupo.getId());

        Map<String, Map<String, Object>> posicoesMap = new LinkedHashMap<>();
        BigDecimal totalInvestidoCusto = BigDecimal.ZERO;
        BigDecimal valorMercadoTotal = BigDecimal.ZERO;
        BigDecimal totalProventosRecebidos = BigDecimal.ZERO;

        // Processa da mais antiga para a mais recente para calcular preco medio
        List<OperacaoInvestimento> cronologica = new ArrayList<>(operacoes);
        Collections.reverse(cronologica);

        Map<String, BigDecimal> qtdPorTicker = new HashMap<>();
        Map<String, BigDecimal> custoTotalPorTicker = new HashMap<>();
        Map<String, BigDecimal> proventosPorTicker = new HashMap<>();
        Map<String, Ativo> ativoPorTicker = new HashMap<>();

        for (OperacaoInvestimento op : cronologica) {
            String ticker = op.getAtivo().getTicker();
            ativoPorTicker.put(ticker, op.getAtivo());
            BigDecimal qtdAtual = qtdPorTicker.getOrDefault(ticker, BigDecimal.ZERO);
            BigDecimal custoAtual = custoTotalPorTicker.getOrDefault(ticker, BigDecimal.ZERO);
            BigDecimal provAtual = proventosPorTicker.getOrDefault(ticker, BigDecimal.ZERO);

            if (op.getTipo() == TipoOperacao.COMPRA) {
                qtdAtual = qtdAtual.add(op.getQuantidade());
                custoAtual = custoAtual.add(op.valorTotal());
            } else if (op.getTipo() == TipoOperacao.VENDA) {
                if (qtdAtual.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal pm = custoAtual.divide(qtdAtual, 4, RoundingMode.HALF_UP);
                    qtdAtual = qtdAtual.subtract(op.getQuantidade()).max(BigDecimal.ZERO);
                    custoAtual = pm.multiply(qtdAtual).setScale(2, RoundingMode.HALF_UP);
                }
            } else if (op.getTipo() == TipoOperacao.PROVENTO) {
                provAtual = provAtual.add(op.valorTotal());
                totalProventosRecebidos = totalProventosRecebidos.add(op.valorTotal());
            }

            qtdPorTicker.put(ticker, qtdAtual);
            custoTotalPorTicker.put(ticker, custoAtual);
            proventosPorTicker.put(ticker, provAtual);
        }

        List<Map<String, Object>> posicoes = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : qtdPorTicker.entrySet()) {
            String ticker = entry.getKey();
            BigDecimal qtd = entry.getValue();
            if (qtd.compareTo(BigDecimal.ZERO) <= 0) continue;

            Ativo ativo = ativoPorTicker.get(ticker);
            BigDecimal custo = custoTotalPorTicker.getOrDefault(ticker, BigDecimal.ZERO);
            BigDecimal precoMedio = custo.divide(qtd, 2, RoundingMode.HALF_UP);
            BigDecimal precoAtual = ativo.getPrecoAtual() != null ? ativo.getPrecoAtual() : precoMedio;
            BigDecimal valorAtual = qtd.multiply(precoAtual).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lucroPrejuizo = valorAtual.subtract(custo);
            BigDecimal rentabilidadePerc = custo.compareTo(BigDecimal.ZERO) > 0
                    ? lucroPrejuizo.multiply(BigDecimal.valueOf(100)).divide(custo, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            totalInvestidoCusto = totalInvestidoCusto.add(custo);
            valorMercadoTotal = valorMercadoTotal.add(valorAtual);

            Map<String, Object> pos = new LinkedHashMap<>();
            pos.put("ticker", ticker);
            pos.put("nome", ativo.getNome());
            pos.put("tipoAtivo", ativo.getTipoAtivo());
            pos.put("setor", ativo.getSetor());
            pos.put("quantidade", qtd);
            pos.put("precoMedio", precoMedio);
            pos.put("precoAtual", precoAtual);
            pos.put("custoTotal", custo);
            pos.put("valorAtual", valorAtual);
            pos.put("lucroPrejuizo", lucroPrejuizo);
            pos.put("rentabilidadePercentual", rentabilidadePerc);
            pos.put("proventosAcumulados", proventosPorTicker.getOrDefault(ticker, BigDecimal.ZERO));
            posicoes.add(pos);
        }

        BigDecimal lucroTotal = valorMercadoTotal.subtract(totalInvestidoCusto);
        BigDecimal rentabilidadeCarteira = totalInvestidoCusto.compareTo(BigDecimal.ZERO) > 0
                ? lucroTotal.multiply(BigDecimal.valueOf(100)).divide(totalInvestidoCusto, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("custoTotalCarteira", totalInvestidoCusto);
        resposta.put("valorMercadoCarteira", valorMercadoTotal);
        resposta.put("lucroPrejuizoCarteira", lucroTotal);
        resposta.put("rentabilidadeCarteiraPercentual", rentabilidadeCarteira);
        resposta.put("totalProventosRecebidos", totalProventosRecebidos);
        resposta.put("posicoes", posicoes);
        resposta.put("operacoes", operacoes);
        resposta.put("ativosDisponiveis", ativoRepository.findAll());
        return resposta;
    }

    // =========================================================================
    // RF-009 e RN-001: PAINEL DE RELATORIOS E DASHBOARDS CONSOLIDADOS
    // =========================================================================

    @Transactional(readOnly = true)
    public Map<String, Object> obterDashboardConsolidado(Long grupoId, String mesReferencia) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        List<Conta> todasContas = contaRepository.findByGrupoIdOrderByIdAsc(grupo.getId());

        // RN-001: Contas com flag "Privada" nao contabilizam para o Patrimonio Liquido global visivel aos demais membros
        BigDecimal saldoContasPublicas = todasContas.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getPrivada()))
                .map(c -> c.getSaldo() != null ? c.getSaldo() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoContasPrivadas = todasContas.stream()
                .filter(c -> Boolean.TRUE.equals(c.getPrivada()))
                .map(c -> c.getSaldo() != null ? c.getSaldo() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> carteira = consultarCarteiraInvestimentos(grupo.getId());
        BigDecimal valorMercadoCarteira = (BigDecimal) carteira.get("valorMercadoCarteira");
        BigDecimal totalProventos = (BigDecimal) carteira.get("totalProventosRecebidos");

        BigDecimal patrimonioLiquidoGlobalFamiliar = saldoContasPublicas.add(valorMercadoCarteira);
        BigDecimal patrimonioBrutoTotalComPrivadas = patrimonioLiquidoGlobalFamiliar.add(saldoContasPrivadas);

        YearMonth ymAtual = (mesReferencia != null && mesReferencia.matches("\\d{4}-\\d{2}"))
                ? YearMonth.parse(mesReferencia)
                : YearMonth.now();
        LocalDate inicioMes = ymAtual.atDay(1);
        LocalDate fimMes = ymAtual.atEndOfMonth();

        List<Transacao> transacoesMes = transacaoRepository.findByContaGrupoIdAndDataBetweenOrderByDataDescIdDesc(
                grupo.getId(), inicioMes, fimMes);

        BigDecimal receitasMes = transacoesMes.stream()
                .filter(t -> t.getTipo() == TipoTransacao.RECEITA)
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal despesasMes = transacoesMes.stream()
                .filter(t -> t.getTipo() == TipoTransacao.DESPESA)
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fluxoCaixaLiquidoMes = receitasMes.subtract(despesasMes);

        // Despesas agrupadas por Categoria (para grafico de pizza/barras)
        Map<String, BigDecimal> gastosPorCategoriaMap = new LinkedHashMap<>();
        for (Transacao t : transacoesMes) {
            if (t.getTipo() == TipoTransacao.DESPESA) {
                String nomeCat = t.getCategoria() != null
                        ? (t.getCategoria().getPai() != null ? t.getCategoria().getPai().getNome() : t.getCategoria().getNome())
                        : "Outros";
                gastosPorCategoriaMap.put(nomeCat, gastosPorCategoriaMap.getOrDefault(nomeCat, BigDecimal.ZERO).add(t.getValor()));
            }
        }
        List<Map<String, Object>> gastosPorCategoria = gastosPorCategoriaMap.entrySet().stream()
                .map(e -> Map.<String, Object>of("categoria", e.getKey(), "valor", e.getValue()))
                .toList();

        // Evolucao do Fluxo de Caixa e Patrimonio nos ultimos 6 meses
        List<Map<String, Object>> serieHistoricaMensal = new ArrayList<>();
        BigDecimal patrimonioBase = patrimonioLiquidoGlobalFamiliar.multiply(new BigDecimal("0.82")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal passo = patrimonioLiquidoGlobalFamiliar.subtract(patrimonioBase)
                .divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP);

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = ymAtual.minusMonths(i);
            LocalDate ini = ym.atDay(1);
            LocalDate fim = ym.atEndOfMonth();
            List<Transacao> transMes = transacaoRepository.findByContaGrupoIdAndDataBetweenOrderByDataDescIdDesc(grupo.getId(), ini, fim);

            BigDecimal rec = transMes.stream()
                    .filter(t -> t.getTipo() == TipoTransacao.RECEITA)
                    .map(Transacao::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal desp = transMes.stream()
                    .filter(t -> t.getTipo() == TipoTransacao.DESPESA)
                    .map(Transacao::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal patMes = (i == 0)
                    ? patrimonioLiquidoGlobalFamiliar
                    : patrimonioBase.add(passo.multiply(BigDecimal.valueOf(5 - i)));

            Map<String, Object> ponto = new LinkedHashMap<>();
            ponto.put("mes", ym.toString());
            ponto.put("receitas", rec);
            ponto.put("despesas", desp);
            ponto.put("saldoMensal", rec.subtract(desp));
            ponto.put("patrimonioLiquido", patMes);
            serieHistoricaMensal.add(ponto);
        }

        List<Orcamento> orcamentosMes = orcamentoRepository.findByGrupoIdAndMesOrderByIdAsc(grupo.getId(), ymAtual.toString());
        List<Notificacao> notificacoesRecentes = notificacaoRepository.findByGrupoIdOrderByCriadaEmDescIdDesc(grupo.getId())
                .stream().limit(10).toList();

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("grupoId", grupo.getId());
        dashboard.put("nomeGrupo", grupo.getNome());
        dashboard.put("mesReferencia", ymAtual.toString());
        dashboard.put("patrimonioLiquidoFamiliar", patrimonioLiquidoGlobalFamiliar);
        dashboard.put("patrimonioTotalComContasPrivadas", patrimonioBrutoTotalComPrivadas);
        dashboard.put("saldoContasPublicas", saldoContasPublicas);
        dashboard.put("saldoContasPrivadasOcultas", saldoContasPrivadas);
        dashboard.put("valorCarteiraInvestimentos", valorMercadoCarteira);
        dashboard.put("totalProventosRecebidos", totalProventos);
        dashboard.put("receitasMes", receitasMes);
        dashboard.put("despesasMes", despesasMes);
        dashboard.put("fluxoCaixaMes", fluxoCaixaLiquidoMes);
        dashboard.put("contas", todasContas);
        dashboard.put("gastosPorCategoria", gastosPorCategoria);
        dashboard.put("serieHistoricaMensal", serieHistoricaMensal);
        dashboard.put("orcamentos", orcamentosMes);
        dashboard.put("ultimasTransacoes", transacoesMes.stream().limit(12).toList());
        dashboard.put("notificacoesRecentes", notificacoesRecentes);
        return dashboard;
    }

    // =========================================================================
    // RF-011: CENTRAL DE ALERTAS E NOTIFICACOES + TAREFA AGENDADA DE VENCIMENTOS
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Notificacao> listarNotificacoes(Long grupoId) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        return notificacaoRepository.findByGrupoIdOrderByCriadaEmDescIdDesc(grupo.getId());
    }

    @Transactional
    public Notificacao marcarNotificacaoComoLida(Long notificacaoId) {
        Notificacao n = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada."));
        n.marcarComoLida();
        return notificacaoRepository.save(n);
    }

    @Transactional
    public void marcarTodasNotificacoesComoLidas(Long grupoId) {
        GrupoFamiliar grupo = obterGrupoPadrao(grupoId);
        List<Notificacao> naoLidas = notificacaoRepository.findByGrupoIdAndLidaFalseOrderByCriadaEmDesc(grupo.getId());
        for (Notificacao n : naoLidas) {
            n.marcarComoLida();
        }
        notificacaoRepository.saveAll(naoLidas);
    }

    /**
     * Secao 2.5 do Artigo e RF-011:
     * "os avisos de vencimento sao gerados por uma tarefa agendada que roda uma vez por dia no servidor."
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public List<Notificacao> verificarVencimentosCartoesAgendado() {
        List<Conta> todasContas = contaRepository.findAll();
        int diaHoje = LocalDate.now().getDayOfMonth();
        List<Notificacao> geradas = new ArrayList<>();

        for (Conta c : todasContas) {
            if (c.getTipo() == TipoConta.CARTAO_CREDITO && c.getDiaVencimento() != null) {
                int diff = c.getDiaVencimento() - diaHoje;
                if (diff >= 0 && diff <= 5) {
                    Notificacao aviso = new Notificacao();
                    aviso.setGrupo(c.getGrupo());
                    aviso.setTipo(TipoNotificacao.VENCIMENTO);
                    aviso.setTitulo("Vencimento Próximo: " + c.getNome());
                    aviso.setMensagem(String.format(
                            "A fatura do cartão '%s' vence no dia %02d deste mês (Fatura atual: R$ %.2f).",
                            c.getNome(), c.getDiaVencimento(), c.getSaldo() != null ? c.getSaldo().abs() : BigDecimal.ZERO));
                    geradas.add(notificacaoRepository.save(aviso));
                }
            }
        }
        return geradas;
    }
}
