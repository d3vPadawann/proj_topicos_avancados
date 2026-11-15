package br.ucsal.finfamily.controller;

import br.ucsal.finfamily.domain.enums.*;
import br.ucsal.finfamily.domain.model.*;
import br.ucsal.finfamily.service.AssistenteIAService;
import br.ucsal.finfamily.service.AuditLogService;
import br.ucsal.finfamily.service.ExportacaoBackupService;
import br.ucsal.finfamily.service.FinanceiroService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller REST Principal do Sistema FinFamily.
 * Expoe todas as funcionalidades de RF-002 a RF-012, Regras de Negocio RN-001 a RN-006
 * e Requisitos Nao Funcionais RNF-001 a RNF-007.
 */
@RestController
@RequestMapping("/api")
public class FinFamilyController {

    private final FinanceiroService financeiroService;
    private final AssistenteIAService assistenteIAService;
    private final ExportacaoBackupService exportacaoBackupService;
    private final AuditLogService auditLogService;

    public FinFamilyController(FinanceiroService financeiroService,
                               AssistenteIAService assistenteIAService,
                               ExportacaoBackupService exportacaoBackupService,
                               AuditLogService auditLogService) {
        this.financeiroService = financeiroService;
        this.assistenteIAService = assistenteIAService;
        this.exportacaoBackupService = exportacaoBackupService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "sistema", "FinFamily - Sistema de Gestão Financeira e Controle de Investimentos Familiar",
                "versao", "2.0.0-UNIDADE-II"
        ));
    }

    // =========================================================================
    // RF-009: DASHBOARD E RELATORIOS CONSOLIDADOS
    // =========================================================================

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> obterDashboard(
            @RequestParam(required = false, defaultValue = "1") Long grupoId,
            @RequestParam(required = false) String mes) {
        return ResponseEntity.ok(financeiroService.obterDashboardConsolidado(grupoId, mes));
    }

    // =========================================================================
    // RF-002, RN-001, RN-002: CONTAS E CARTOES
    // =========================================================================

    @GetMapping("/contas")
    public ResponseEntity<List<Conta>> listarContas(@RequestParam(required = false, defaultValue = "1") Long grupoId) {
        return ResponseEntity.ok(financeiroService.listarContas(grupoId));
    }

    @PostMapping("/contas")
    public ResponseEntity<Conta> criarConta(@RequestBody Map<String, Object> body) {
        Long grupoId = body.get("grupoId") != null ? Long.valueOf(String.valueOf(body.get("grupoId"))) : 1L;
        Long proprietarioId = body.get("proprietarioId") != null ? Long.valueOf(String.valueOf(body.get("proprietarioId"))) : null;

        Conta c = new Conta();
        c.setNome(String.valueOf(body.get("nome")));
        c.setInstituicao(body.get("instituicao") != null ? String.valueOf(body.get("instituicao")) : "Banco");
        c.setTipo(body.get("tipo") != null ? TipoConta.valueOf(String.valueOf(body.get("tipo"))) : TipoConta.CORRENTE);
        c.setSaldo(body.get("saldo") != null ? new BigDecimal(String.valueOf(body.get("saldo"))) : BigDecimal.ZERO);
        c.setLimiteCredito(body.get("limiteCredito") != null ? new BigDecimal(String.valueOf(body.get("limiteCredito"))) : BigDecimal.ZERO);
        c.setPrivada(body.get("privada") != null && Boolean.parseBoolean(String.valueOf(body.get("privada"))));
        if (body.get("diaVencimento") != null && !String.valueOf(body.get("diaVencimento")).isBlank()) {
            c.setDiaVencimento(Integer.valueOf(String.valueOf(body.get("diaVencimento"))));
        }
        if (body.get("codigoIdentificadorSensivel") != null) {
            c.setCodigoIdentificadorSensivel(String.valueOf(body.get("codigoIdentificadorSensivel")));
        }

        return ResponseEntity.ok(financeiroService.criarOuAtualizarConta(grupoId, c, proprietarioId));
    }

    @PutMapping("/contas/{id}")
    public ResponseEntity<Conta> atualizarConta(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long grupoId = body.get("grupoId") != null ? Long.valueOf(String.valueOf(body.get("grupoId"))) : 1L;
        Long proprietarioId = body.get("proprietarioId") != null ? Long.valueOf(String.valueOf(body.get("proprietarioId"))) : null;

        Conta c = new Conta();
        c.setId(id);
        c.setNome(String.valueOf(body.get("nome")));
        c.setInstituicao(body.get("instituicao") != null ? String.valueOf(body.get("instituicao")) : "Banco");
        c.setTipo(body.get("tipo") != null ? TipoConta.valueOf(String.valueOf(body.get("tipo"))) : TipoConta.CORRENTE);
        c.setSaldo(body.get("saldo") != null ? new BigDecimal(String.valueOf(body.get("saldo"))) : BigDecimal.ZERO);
        c.setLimiteCredito(body.get("limiteCredito") != null ? new BigDecimal(String.valueOf(body.get("limiteCredito"))) : BigDecimal.ZERO);
        c.setPrivada(body.get("privada") != null && Boolean.parseBoolean(String.valueOf(body.get("privada"))));
        if (body.get("diaVencimento") != null && !String.valueOf(body.get("diaVencimento")).isBlank()) {
            c.setDiaVencimento(Integer.valueOf(String.valueOf(body.get("diaVencimento"))));
        }
        return ResponseEntity.ok(financeiroService.criarOuAtualizarConta(grupoId, c, proprietarioId));
    }

    @DeleteMapping("/contas/{id}")
    public ResponseEntity<Map<String, String>> excluirConta(@PathVariable Long id) {
        financeiroService.excluirConta(id);
        return ResponseEntity.ok(Map.of("mensagem", "Conta excluída com sucesso (RN-002 verificada: saldo zero)."));
    }

    // =========================================================================
    // RF-004: CATEGORIAS E TAGS
    // =========================================================================

    @GetMapping("/categorias")
    public ResponseEntity<List<Map<String, Object>>> listarCategoriasArvore(@RequestParam(required = false, defaultValue = "1") Long grupoId) {
        return ResponseEntity.ok(financeiroService.listarCategoriasEmArvore(grupoId));
    }

    @GetMapping("/categorias/planas")
    public ResponseEntity<List<Categoria>> listarCategoriasPlanas(@RequestParam(required = false, defaultValue = "1") Long grupoId) {
        return ResponseEntity.ok(financeiroService.listarTodasCategoriasPlanas(grupoId));
    }

    @PostMapping("/categorias")
    public ResponseEntity<Categoria> criarCategoria(@RequestBody Map<String, Object> body) {
        Long grupoId = body.get("grupoId") != null ? Long.valueOf(String.valueOf(body.get("grupoId"))) : 1L;
        String nome = String.valueOf(body.get("nome"));
        String cor = body.get("cor") != null ? String.valueOf(body.get("cor")) : "#3B82F6";
        String icone = body.get("icone") != null ? String.valueOf(body.get("icone")) : "tag";
        TipoTransacao tipoPadrao = body.get("tipoPadrao") != null
                ? TipoTransacao.valueOf(String.valueOf(body.get("tipoPadrao")))
                : TipoTransacao.DESPESA;
        Long paiId = (body.get("paiId") != null && !String.valueOf(body.get("paiId")).isBlank())
                ? Long.valueOf(String.valueOf(body.get("paiId")))
                : null;
        return ResponseEntity.ok(financeiroService.criarCategoria(grupoId, nome, cor, icone, tipoPadrao, paiId));
    }

    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<Map<String, String>> excluirCategoria(@PathVariable Long id) {
        financeiroService.excluirCategoria(id);
        return ResponseEntity.ok(Map.of("mensagem", "Categoria excluída com sucesso."));
    }

    @GetMapping("/tags")
    public ResponseEntity<List<Tag>> listarTags(@RequestParam(required = false, defaultValue = "1") Long grupoId) {
        return ResponseEntity.ok(financeiroService.listarTags(grupoId));
    }

    @PostMapping("/tags")
    public ResponseEntity<Tag> criarTag(@RequestBody Map<String, Object> body) {
        Long grupoId = body.get("grupoId") != null ? Long.valueOf(String.valueOf(body.get("grupoId"))) : 1L;
        String nome = String.valueOf(body.get("nome"));
        String cor = body.get("cor") != null ? String.valueOf(body.get("cor")) : "#6366F1";
        return ResponseEntity.ok(financeiroService.criarTag(grupoId, nome, cor));
    }

    @DeleteMapping("/tags/{id}")
    public ResponseEntity<Map<String, String>> excluirTag(@PathVariable Long id) {
        financeiroService.excluirTag(id);
        return ResponseEntity.ok(Map.of("mensagem", "Tag excluída com sucesso."));
    }

    // =========================================================================
    // RF-003 e RN-003: TRANSACOES (COM PARCELAMENTO E RECORRENCIA)
    // =========================================================================

    @GetMapping("/transacoes")
    public ResponseEntity<List<Transacao>> listarTransacoes(
            @RequestParam(required = false, defaultValue = "1") Long grupoId,
            @RequestParam(required = false) Long contaId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String periodo) {
        return ResponseEntity.ok(financeiroService.listarTransacoes(grupoId, contaId, categoriaId, tipo, busca, periodo));
    }

    @PostMapping("/transacoes")
    public ResponseEntity<List<Transacao>> registrarTransacao(@RequestBody Map<String, Object> body) {
        Long grupoId = body.get("grupoId") != null ? Long.valueOf(String.valueOf(body.get("grupoId"))) : 1L;
        String descricao = String.valueOf(body.get("descricao"));
        BigDecimal valor = new BigDecimal(String.valueOf(body.get("valor")));
        LocalDate data = (body.get("data") != null && !String.valueOf(body.get("data")).isBlank())
                ? LocalDate.parse(String.valueOf(body.get("data")))
                : LocalDate.now();
        TipoTransacao tipo = body.get("tipo") != null
                ? TipoTransacao.valueOf(String.valueOf(body.get("tipo")).toUpperCase())
                : TipoTransacao.DESPESA;
        Long contaId = (body.get("contaId") != null && !String.valueOf(body.get("contaId")).isBlank())
                ? Long.valueOf(String.valueOf(body.get("contaId")))
                : null;
        Long contaDestinoId = (body.get("contaDestinoId") != null && !String.valueOf(body.get("contaDestinoId")).isBlank())
                ? Long.valueOf(String.valueOf(body.get("contaDestinoId")))
                : null;
        Long categoriaId = (body.get("categoriaId") != null && !String.valueOf(body.get("categoriaId")).isBlank())
                ? Long.valueOf(String.valueOf(body.get("categoriaId")))
                : null;

        List<Long> tagIds = null;
        if (body.get("tagIds") instanceof List<?> rawTags) {
            tagIds = rawTags.stream().map(o -> Long.valueOf(String.valueOf(o))).toList();
        }

        Integer totalParcelas = (body.get("totalParcelas") != null && !String.valueOf(body.get("totalParcelas")).isBlank())
                ? Integer.valueOf(String.valueOf(body.get("totalParcelas")))
                : 1;
        Boolean recorrente = body.get("recorrente") != null && Boolean.parseBoolean(String.valueOf(body.get("recorrente")));
        String frequencia = body.get("frequenciaRecorrencia") != null ? String.valueOf(body.get("frequenciaRecorrencia")) : "MENSAL";
        Long autorId = body.get("autorId") != null ? Long.valueOf(String.valueOf(body.get("autorId"))) : null;

        return ResponseEntity.ok(financeiroService.registrarTransacao(
                grupoId, descricao, valor, data, tipo, contaId, contaDestinoId, categoriaId,
                tagIds, totalParcelas, recorrente, frequencia, autorId, "MANUAL"
        ));
    }

    @DeleteMapping("/transacoes/{id}")
    public ResponseEntity<Map<String, String>> excluirTransacao(@PathVariable Long id) {
        financeiroService.excluirTransacao(id);
        return ResponseEntity.ok(Map.of("mensagem", "Transação excluída com sucesso."));
    }

    // =========================================================================
    // RF-005 e OE2: IMPORTACAO DE EXTRATOS CSV (BPMN FIGURA 1 E UC05)
    // =========================================================================

    @PostMapping("/importacao/analisar")
    public ResponseEntity<Map<String, Object>> analisarCsvTexto(@RequestBody Map<String, Object> body) {
        Long grupoId = body.get("grupoId") != null ? Long.valueOf(String.valueOf(body.get("grupoId"))) : 1L;
        Long contaId = Long.valueOf(String.valueOf(body.get("contaId")));
        String conteudoCsv = String.valueOf(body.get("conteudoCsv"));
        return ResponseEntity.ok(financeiroService.analisarArquivoCSV(grupoId, contaId, conteudoCsv));
    }

    @PostMapping("/importacao/upload-analisar")
    public ResponseEntity<Map<String, Object>> uploadAnalisarCsv(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("contaId") Long contaId,
            @RequestParam(value = "grupoId", defaultValue = "1") Long grupoId) throws Exception {
        String conteudo = new String(arquivo.getBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(financeiroService.analisarArquivoCSV(grupoId, contaId, conteudo));
    }

    @PostMapping("/importacao/confirmar")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> confirmarImportacaoCsv(@RequestBody Map<String, Object> body) {
        Long grupoId = body.get("grupoId") != null ? Long.valueOf(String.valueOf(body.get("grupoId"))) : 1L;
        Long contaId = Long.valueOf(String.valueOf(body.get("contaId")));
        Long autorId = body.get("autorId") != null ? Long.valueOf(String.valueOf(body.get("autorId"))) : null;
        List<Map<String, Object>> transacoesConfirmadas = (List<Map<String, Object>>) body.get("transacoes");
        return ResponseEntity.ok(financeiroService.confirmarImportacaoCSV(grupoId, contaId, transacoesConfirmadas, autorId));
    }

    @PostMapping("/importacao/direto")
    public ResponseEntity<Map<String, Object>> importarCsvDireto(@RequestBody Map<String, Object> body) {
        Long grupoId = body.get("grupoId") != null ? Long.valueOf(String.valueOf(body.get("grupoId"))) : 1L;
        Long contaId = Long.valueOf(String.valueOf(body.get("contaId")));
        String conteudoCsv = String.valueOf(body.get("conteudoCsv"));
        Long autorId = body.get("autorId") != null ? Long.valueOf(String.valueOf(body.get("autorId"))) : null;
        return ResponseEntity.ok(financeiroService.importarArquivoCSVDireto(grupoId, contaId, conteudoCsv, autorId));
    }

    // =========================================================================
    // RF-006 e RN-004: ORCAMENTOS MENSAIS (BUDGETING)
    // =========================================================================

    @GetMapping("/orcamentos")
    public ResponseEntity<List<Orcamento>> listarOrcamentos(
            @RequestParam(required = false, defaultValue = "1") Long grupoId,
            @RequestParam(required = false) String mes) {
        return ResponseEntity.ok(financeiroService.listarOrcamentos(grupoId, mes));
    }

    @PostMapping("/orcamentos")
    public ResponseEntity<Orcamento> definirOrcamento(@RequestBody Map<String, Object> body) {
        Long grupoId = body.get("grupoId") != null ? Long.valueOf(String.valueOf(body.get("grupoId"))) : 1L;
        Long categoriaId = Long.valueOf(String.valueOf(body.get("categoriaId")));
        String mes = body.get("mes") != null ? String.valueOf(body.get("mes")) : null;
        BigDecimal valorPlanejado = new BigDecimal(String.valueOf(body.get("valorPlanejado")));
        return ResponseEntity.ok(financeiroService.definirOrcamento(grupoId, categoriaId, mes, valorPlanejado));
    }

    @DeleteMapping("/orcamentos/{id}")
    public ResponseEntity<Map<String, String>> excluirOrcamento(@PathVariable Long id) {
        financeiroService.excluirOrcamento(id);
        return ResponseEntity.ok(Map.of("mensagem", "Orçamento excluído com sucesso."));
    }

    // =========================================================================
    // RF-007 e OE4: GRUPO FAMILIAR (BPMN FIGURA 2)
    // =========================================================================

    @GetMapping("/grupo/membros")
    public ResponseEntity<List<Membro>> listarMembros(@RequestParam(required = false, defaultValue = "1") Long grupoId) {
        return ResponseEntity.ok(financeiroService.listarMembros(grupoId));
    }

    @PostMapping("/grupo/convidar")
    public ResponseEntity<Membro> convidarMembro(@RequestBody Map<String, Object> body) {
        Long grupoId = body.get("grupoId") != null ? Long.valueOf(String.valueOf(body.get("grupoId"))) : 1L;
        String nome = String.valueOf(body.get("nome"));
        String email = String.valueOf(body.get("email"));
        PapelFamiliar papel = body.get("papel") != null
                ? PapelFamiliar.valueOf(String.valueOf(body.get("papel")).toUpperCase())
                : PapelFamiliar.MEMBRO;
        return ResponseEntity.ok(financeiroService.convidarMembro(grupoId, nome, email, papel));
    }

    @PostMapping("/grupo/membros/{id}/resposta")
    public ResponseEntity<Membro> responderConvite(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean aceitar = body.get("aceitar") == null || Boolean.parseBoolean(String.valueOf(body.get("aceitar")));
        return ResponseEntity.ok(financeiroService.responderConvite(id, aceitar));
    }

    @PatchMapping("/grupo/membros/{id}/status")
    public ResponseEntity<Membro> alterarStatusMembro(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        StatusMembro status = body.get("status") != null ? StatusMembro.valueOf(String.valueOf(body.get("status"))) : null;
        PapelFamiliar papel = body.get("papel") != null ? PapelFamiliar.valueOf(String.valueOf(body.get("papel"))) : null;
        return ResponseEntity.ok(financeiroService.alterarStatusMembro(id, status, papel));
    }

    // =========================================================================
    // RF-008 e RN-005: INVESTIMENTOS E PROVENTOS (BPMN FIGURA 3)
    // =========================================================================

    @GetMapping("/investimentos/carteira")
    public ResponseEntity<Map<String, Object>> consultarCarteira(@RequestParam(required = false, defaultValue = "1") Long grupoId) {
        return ResponseEntity.ok(financeiroService.consultarCarteiraInvestimentos(grupoId));
    }

    @GetMapping("/investimentos/ativos")
    public ResponseEntity<List<Ativo>> listarAtivos() {
        return ResponseEntity.ok(financeiroService.listarAtivos());
    }

    @PostMapping("/investimentos/ativos")
    public ResponseEntity<Ativo> cadastrarAtivo(@RequestBody Map<String, Object> body) {
        String ticker = String.valueOf(body.get("ticker"));
        String nome = String.valueOf(body.get("nome"));
        BigDecimal precoAtual = body.get("precoAtual") != null ? new BigDecimal(String.valueOf(body.get("precoAtual"))) : BigDecimal.ZERO;
        String tipoAtivo = body.get("tipoAtivo") != null ? String.valueOf(body.get("tipoAtivo")) : "ACAO";
        String setor = body.get("setor") != null ? String.valueOf(body.get("setor")) : "Renda Variável";
        return ResponseEntity.ok(financeiroService.cadastrarOuAtualizarAtivo(ticker, nome, precoAtual, tipoAtivo, setor));
    }

    @PostMapping("/investimentos/operacoes")
    public ResponseEntity<OperacaoInvestimento> registrarOperacaoInvestimento(@RequestBody Map<String, Object> body) {
        Long grupoId = body.get("grupoId") != null ? Long.valueOf(String.valueOf(body.get("grupoId"))) : 1L;
        String ticker = String.valueOf(body.get("ticker"));
        TipoOperacao tipo = TipoOperacao.valueOf(String.valueOf(body.get("tipo")).toUpperCase());
        BigDecimal quantidade = new BigDecimal(String.valueOf(body.get("quantidade")));
        BigDecimal preco = new BigDecimal(String.valueOf(body.get("preco")));
        LocalDate data = (body.get("data") != null && !String.valueOf(body.get("data")).isBlank())
                ? LocalDate.parse(String.valueOf(body.get("data")))
                : LocalDate.now();
        Long contaCorretoraId = (body.get("contaCorretoraId") != null && !String.valueOf(body.get("contaCorretoraId")).isBlank())
                ? Long.valueOf(String.valueOf(body.get("contaCorretoraId")))
                : null;
        Long contaDestinoId = (body.get("contaDestinoProventoId") != null && !String.valueOf(body.get("contaDestinoProventoId")).isBlank())
                ? Long.valueOf(String.valueOf(body.get("contaDestinoProventoId")))
                : null;
        String nomeAtivo = body.get("nomeAtivo") != null ? String.valueOf(body.get("nomeAtivo")) : null;

        return ResponseEntity.ok(financeiroService.registrarOperacaoInvestimento(
                grupoId, ticker, tipo, quantidade, preco, data, contaCorretoraId, contaDestinoId, nomeAtivo
        ));
    }

    // =========================================================================
    // RF-011: CENTRAL DE ALERTAS E NOTIFICACOES
    // =========================================================================

    @GetMapping("/notificacoes")
    public ResponseEntity<List<Notificacao>> listarNotificacoes(@RequestParam(required = false, defaultValue = "1") Long grupoId) {
        return ResponseEntity.ok(financeiroService.listarNotificacoes(grupoId));
    }

    @PatchMapping("/notificacoes/{id}/lida")
    public ResponseEntity<Notificacao> marcarComoLida(@PathVariable Long id) {
        return ResponseEntity.ok(financeiroService.marcarNotificacaoComoLida(id));
    }

    @PostMapping("/notificacoes/ler-todas")
    public ResponseEntity<Map<String, String>> marcarTodasComoLidas(@RequestParam(required = false, defaultValue = "1") Long grupoId) {
        financeiroService.marcarTodasNotificacoesComoLidas(grupoId);
        return ResponseEntity.ok(Map.of("mensagem", "Todas as notificações foram marcadas como lidas."));
    }

    @PostMapping("/notificacoes/verificar-vencimentos")
    public ResponseEntity<List<Notificacao>> acionarVerificacaoVencimentos() {
        return ResponseEntity.ok(financeiroService.verificarVencimentosCartoesAgendado());
    }

    // =========================================================================
    // RF-012, RN-006 e OE5: ASSISTENTE CONVERSACIONAL COM IA (BPMN FIGURA 4)
    // =========================================================================

    @PostMapping("/assistente/interpretar")
    public ResponseEntity<Comando> interpretarComandoIA(@RequestBody Map<String, Object> body) {
        Long grupoId = body.get("grupoId") != null ? Long.valueOf(String.valueOf(body.get("grupoId"))) : 1L;
        String texto = String.valueOf(body.get("texto"));
        return ResponseEntity.ok(assistenteIAService.interpretar(grupoId, texto));
    }

    @PostMapping("/assistente/executar")
    public ResponseEntity<Map<String, Object>> executarComandoIA(@RequestBody Map<String, Object> body) {
        Long grupoId = body.get("grupoId") != null ? Long.valueOf(String.valueOf(body.get("grupoId"))) : 1L;
        boolean confirmado = body.get("confirmado") != null && Boolean.parseBoolean(String.valueOf(body.get("confirmado")));
        Long autorId = body.get("autorId") != null ? Long.valueOf(String.valueOf(body.get("autorId"))) : null;

        Comando cmd = new Comando();
        cmd.setIntencao(body.get("intencao") != null ? String.valueOf(body.get("intencao")) : "registrar");
        if (body.get("valor") != null && !String.valueOf(body.get("valor")).isBlank()) {
            cmd.setValor(new BigDecimal(String.valueOf(body.get("valor"))));
        }
        cmd.setDescricao(body.get("descricao") != null ? String.valueOf(body.get("descricao")) : "Lançamento Assistente IA");
        if (body.get("data") != null && !String.valueOf(body.get("data")).isBlank()) {
            cmd.setData(LocalDate.parse(String.valueOf(body.get("data"))));
        } else {
            cmd.setData(LocalDate.now());
        }
        cmd.setConta(body.get("conta") != null ? String.valueOf(body.get("conta")) : null);
        if (body.get("contaId") != null && !String.valueOf(body.get("contaId")).isBlank()) {
            cmd.setContaId(Long.valueOf(String.valueOf(body.get("contaId"))));
        }
        cmd.setCategoria(body.get("categoria") != null ? String.valueOf(body.get("categoria")) : null);
        if (body.get("categoriaId") != null && !String.valueOf(body.get("categoriaId")).isBlank()) {
            cmd.setCategoriaId(Long.valueOf(String.valueOf(body.get("categoriaId"))));
        }
        cmd.setPeriodo(body.get("periodo") != null ? String.valueOf(body.get("periodo")) : "mes_atual");
        cmd.setBusca(body.get("busca") != null ? String.valueOf(body.get("busca")) : null);
        cmd.setTipo(body.get("tipo") != null ? String.valueOf(body.get("tipo")) : "DESPESA");
        if (body.get("parcelas") != null && !String.valueOf(body.get("parcelas")).isBlank()) {
            cmd.setParcelas(Integer.valueOf(String.valueOf(body.get("parcelas"))));
        }
        cmd.setRecorrente(body.get("recorrente") != null && Boolean.parseBoolean(String.valueOf(body.get("recorrente"))));

        return ResponseEntity.ok(assistenteIAService.executar(grupoId, cmd, confirmado, autorId));
    }

    @GetMapping("/assistente/benchmark-50")
    public ResponseEntity<Map<String, Object>> executarBenchmark50Comandos(@RequestParam(required = false, defaultValue = "1") Long grupoId) {
        return ResponseEntity.ok(assistenteIAService.executarBateria50ComandosTeste(grupoId));
    }

    // =========================================================================
    // RF-010: EXPORTACAO DE DADOS EM PDF, CSV E EXCEL (.XLSX)
    // =========================================================================

    @GetMapping("/exportacao/csv")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam(required = false, defaultValue = "1") Long grupoId,
            @RequestParam(required = false) String mes) {
        byte[] bytes = exportacaoBackupService.exportarCSV(grupoId, mes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"finfamily_relatorio.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @GetMapping("/exportacao/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false, defaultValue = "1") Long grupoId,
            @RequestParam(required = false) String mes) {
        byte[] bytes = exportacaoBackupService.exportarExcel(grupoId, mes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"finfamily_relatorio.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/exportacao/pdf")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam(required = false, defaultValue = "1") Long grupoId,
            @RequestParam(required = false) String mes) {
        byte[] bytes = exportacaoBackupService.exportarPDF(grupoId, mes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"finfamily_relatorio.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    // =========================================================================
    // UC13 e RNF-007: SYSADMIN LOGS DE SEGURANCA E ROTINA DE BACKUP DIARIO
    // =========================================================================

    @GetMapping("/sysadmin/logs")
    public ResponseEntity<List<AuditLog>> consultarLogsSistema() {
        return ResponseEntity.ok(auditLogService.listarLogsRecentes());
    }

    @PostMapping("/sysadmin/backup")
    public ResponseEntity<Map<String, Object>> executarBackupManual() {
        return ResponseEntity.ok(exportacaoBackupService.executarRotinaBackupDiario());
    }
}
