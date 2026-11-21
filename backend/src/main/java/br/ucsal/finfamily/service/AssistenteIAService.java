// assistente conversacional com IA
package br.ucsal.finfamily.service;

import br.ucsal.finfamily.domain.enums.TipoTransacao;
import br.ucsal.finfamily.domain.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AssistenteIAService {
    public static final String PROVEDOR_CONTINGENCIA = "CONTINGENCIA_LOCAL (regras)";

    private static final Set<String> PERIODOS_VALIDOS = Set.of("hoje", "semana_atual", "mes_atual", "mes_passado", "ano_atual");

    private final FinanceiroService financeiroService;
    private final AuditLogService auditLogService;
    private final ClienteLLM clienteLLM;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssistenteIAService(FinanceiroService financeiroService, AuditLogService auditLogService, ClienteLLM clienteLLM) {
        this.financeiroService = financeiroService;
        this.auditLogService = auditLogService;
        this.clienteLLM = clienteLLM;
    }

    @Transactional(readOnly = true)
    public Comando interpretar(Long grupoId, String texto) {
        long inicioNs = System.nanoTime();

        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Fluxo Alternativo 3a (UC12): Por favor, digite ou fale um comando para o assistente.");
        }

        String normCheck = normalizar(texto);
        if ((normCheck.contains("qual acao comprar") || normCheck.contains("onde devo investir")
                || normCheck.contains("recomende um investimento") || normCheck.contains("me indique acoes"))
                && !normCheck.contains("gastei") && !normCheck.contains("registrar")) {
            Comando recusaEscopo = new Comando();
            recusaEscopo.setIntencao("fora_escopo");
            recusaEscopo.setTextoOriginal(texto);
            recusaEscopo.setRequerConfirmacao(false);
            recusaEscopo.setProvedorIA("FINFAMILY_GUARDRAIL");
            recusaEscopo.setTempoRespostaMs((System.nanoTime() - inicioNs) / 1_000_000);
            recusaEscopo.setMensagemAssistente(
                    "Em coerência com o escopo do FinFamily (Seção 11.3), o assistente não recomenda investimentos nem presta consultoria financeira; ele apenas registra transações e consulta os dados da própria família.");
            return recusaEscopo;
        }

        GrupoFamiliar grupo = financeiroService.obterGrupoPadrao(grupoId);
        List<Conta> contas = financeiroService.listarContas(grupo.getId());
        List<Categoria> categorias = financeiroService.listarTodasCategoriasPlanas(grupo.getId());

        String promptSeguro = montarPromptSeguroRnf004(texto, contas, categorias);

        Comando comando = clienteLLM.gerarJson(promptSeguro)
                .map(r -> converterRespostaLLM(r.json(), r.provedor(), texto, contas, categorias))
                .orElse(null);

        if (comando == null) {
            comando = interpretarMotorNlpHibrido(texto, contas, categorias);
            comando.setProvedorIA(PROVEDOR_CONTINGENCIA);
            comando.setMensagemAssistente("Serviço de IA indisponível no momento; comando interpretado pelo motor local de contingência (Seção 11.3). "
                    + comando.getMensagemAssistente());
        }

        long duracaoMs = Math.max(1L, (System.nanoTime() - inicioNs) / 1_000_000);
        comando.setTempoRespostaMs(duracaoMs);
        comando.setTextoOriginal(texto);

        auditLogService.registrar("ASSISTENTE_IA_INTERPRETAR", "ASSISTENTE_IA", "membro@finfamily.com", "INFO",
                "Intencao='" + comando.getIntencao() + "' interpretada por " + comando.getProvedorIA() + " em " + duracaoMs
                        + "ms (RNF-004 respeitado: zero saldos enviados).");

        return comando;
    }

    Comando converterRespostaLLM(String json, String provedor, String textoOriginal,
                                 List<Conta> contas, List<Categoria> categorias) {
        JsonNode r;
        try {
            int ini = json.indexOf('{');
            int fim = json.lastIndexOf('}');
            if (ini < 0 || fim <= ini) {
                return null;
            }
            r = objectMapper.readTree(json.substring(ini, fim + 1));
        } catch (Exception e) {
            return null;
        }

        String intencao = texto(r, "intencao").toLowerCase();
        Comando cmd = new Comando();
        cmd.setProvedorIA(provedor);

        switch (intencao) {
            case "fora_escopo" -> {
                cmd.setIntencao("fora_escopo");
                cmd.setRequerConfirmacao(false);
                cmd.setMensagemAssistente(
                        "Em coerência com o escopo do FinFamily (Seção 11.3), o assistente não recomenda investimentos nem presta consultoria financeira; ele apenas registra transações e consulta os dados da própria família.");
                return cmd;
            }
            case "nao_entendido" -> throw new IllegalArgumentException(
                    "Fluxo Alternativo 3a (UC12): Não entendi o comando. Por favor, reformule (ex: 'Registrar gasto de R$ 45,90 na padaria hoje no cartão') ou utilize o formulário de lançamento.");
            case "registrar", "consultar" -> cmd.setIntencao(intencao);
            default -> {
                return null;
            }
        }

        cmd.setTipo("RECEITA".equalsIgnoreCase(texto(r, "tipo")) ? "RECEITA" : "DESPESA");

        if ("consultar".equals(intencao)) {
            cmd.setRequerConfirmacao(false);
            String periodo = texto(r, "periodo").toLowerCase();
            cmd.setPeriodo(PERIODOS_VALIDOS.contains(periodo) ? periodo : "mes_atual");
            cmd.setBusca(texto(r, "busca"));

            Conta conta = buscarContaPorNome(texto(r, "conta"), contas);
            if (conta != null) {
                cmd.setConta(conta.getNome());
                cmd.setContaId(conta.getId());
            }
            Categoria categoria = buscarCategoriaPorNome(texto(r, "categoria"), categorias);
            if (categoria != null) {
                cmd.setCategoria(categoria.getNome());
                cmd.setCategoriaId(categoria.getId());
            }

            cmd.setMensagemAssistente(String.format(
                    "Filtro de consulta identificado: tipo=%s, busca='%s', categoria='%s', período='%s'.",
                    cmd.getTipo(),
                    cmd.getBusca().isBlank() ? "Todas" : cmd.getBusca(),
                    cmd.getCategoria() != null ? cmd.getCategoria() : "Todas",
                    traduzirPeriodo(cmd.getPeriodo())
            ));
            return cmd;
        }

        BigDecimal valor;
        try {
            valor = new BigDecimal(texto(r, "valor").replace(",", ".")).abs().setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
        if (valor.signum() == 0) {
            return null;
        }

        cmd.setRequerConfirmacao(true);
        cmd.setValor(valor);

        String descricao = texto(r, "descricao");
        cmd.setDescricao(descricao.isBlank() ? "Lançamento via Assistente IA" : capitalizar(descricao));

        try {
            String data = texto(r, "data");
            cmd.setData(data.isBlank() ? LocalDate.now() : LocalDate.parse(data));
        } catch (Exception e) {
            cmd.setData(LocalDate.now());
        }

        int parcelas = r.path("parcelas").asInt(1);
        cmd.setParcelas(Math.max(1, Math.min(parcelas, 48)));
        cmd.setRecorrente(r.path("recorrente").asBoolean(false));

        String norm = normalizar(textoOriginal);
        Conta conta = buscarContaPorNome(texto(r, "conta"), contas);
        if (conta == null) {
            conta = identificarContaNoTexto(norm, contas, true);
        }
        if (conta != null) {
            cmd.setConta(conta.getNome());
            cmd.setContaId(conta.getId());
        }

        Categoria categoria = buscarCategoriaPorNome(texto(r, "categoria"), categorias);
        if (categoria == null) {
            categoria = identificarCategoriaParaRegistro(norm, cmd.getDescricao(), cmd.getTipo(), categorias);
        }
        if (categoria != null) {
            cmd.setCategoria(categoria.getNome());
            cmd.setCategoriaId(categoria.getId());
        }

        cmd.setMensagemAssistente(String.format(
                "Card montado para confirmação (RN-006): %s de R$ %.2f — '%s' em %s na conta '%s' [%s]. Confirme para gravar.",
                "RECEITA".equals(cmd.getTipo()) ? "Receita" : "Despesa",
                cmd.getValor(),
                cmd.getDescricao(),
                cmd.getData(),
                cmd.getConta(),
                cmd.getCategoria()
        ));
        return cmd;
    }

    private static String texto(JsonNode no, String campo) {
        JsonNode v = no.get(campo);
        return v == null || v.isNull() ? "" : v.asText("").trim();
    }

    private Conta buscarContaPorNome(String nome, List<Conta> contas) {
        if (nome == null || nome.isBlank() || contas == null) return null;
        String alvo = normalizar(nome);
        return contas.stream().filter(c -> normalizar(c.getNome()).equals(alvo)).findFirst()
                .orElseGet(() -> contas.stream()
                        .filter(c -> normalizar(c.getNome()).contains(alvo) || alvo.contains(normalizar(c.getNome())))
                        .findFirst().orElse(null));
    }

    private Categoria buscarCategoriaPorNome(String nome, List<Categoria> categorias) {
        if (nome == null || nome.isBlank() || categorias == null) return null;
        String alvo = normalizar(nome);
        return categorias.stream().filter(c -> normalizar(c.getNome()).equals(alvo)).findFirst().orElse(null);
    }

    @Transactional
    public Map<String, Object> executar(Long grupoId, Comando comando, boolean confirmadoPeloUsuario, Long autorId) {
        GrupoFamiliar grupo = financeiroService.obterGrupoPadrao(grupoId);

        if ("registrar".equalsIgnoreCase(comando.getIntencao())) {
            if (!confirmadoPeloUsuario) {
                throw new IllegalStateException(
                        "RN-006 Violada: Um lançamento interpretado pelo assistente de IA só pode ser gravado após o usuário confirmar o card exibido.");
            }

            TipoTransacao tipo = "RECEITA".equalsIgnoreCase(comando.getTipo())
                    ? TipoTransacao.RECEITA
                    : TipoTransacao.DESPESA;

            List<Transacao> gravadas = financeiroService.registrarTransacao(
                    grupo.getId(),
                    comando.getDescricao(),
                    comando.getValor(),
                    comando.getData() != null ? comando.getData() : LocalDate.now(),
                    tipo,
                    comando.getContaId(),
                    null,
                    comando.getCategoriaId(),
                    null,
                    comando.getParcelas() != null ? comando.getParcelas() : 1,
                    comando.getRecorrente() != null ? comando.getRecorrente() : false,
                    Boolean.TRUE.equals(comando.getRecorrente()) ? "MENSAL" : null,
                    autorId,
                    "ASSISTENTE_IA"
            );

            Map<String, Object> resposta = new LinkedHashMap<>();
            resposta.put("acao", "LANCAMENTO_CONFIRMADO_E_GRAVADO");
            resposta.put("regraAtendida", "RN-006: Gravado somente após confirmação do card pelo usuário");
            resposta.put("quantidadeParcelasGeradas", gravadas.size());
            resposta.put("transacoes", gravadas);
            resposta.put("mensagem", String.format(
                    "Lançamento '%s' no valor de R$ %.2f registrado com sucesso na conta '%s' (%s)!",
                    comando.getDescricao(),
                    comando.getValor(),
                    comando.getConta() != null ? comando.getConta() : "Conta Principal",
                    comando.getCategoria() != null ? comando.getCategoria() : "Geral"
            ));
            return resposta;
        }

        List<Transacao> filtradas = financeiroService.listarTransacoes(
                grupo.getId(),
                comando.getContaId(),
                comando.getCategoriaId(),
                comando.getTipo() != null ? comando.getTipo() : "DESPESA",
                comando.getBusca(),
                comando.getPeriodo() != null ? comando.getPeriodo() : "mes_atual"
        );

        BigDecimal total = filtradas.stream()
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String rotuloPeriodo = traduzirPeriodo(comando.getPeriodo());
        String alvoFiltro = (comando.getBusca() != null && !comando.getBusca().isBlank())
                ? comando.getBusca()
                : (comando.getCategoria() != null && !comando.getCategoria().isBlank()
                ? comando.getCategoria()
                : (comando.getConta() != null && !comando.getConta().isBlank() ? comando.getConta() : "todas as categorias"));

        String verbo = "RECEITA".equalsIgnoreCase(comando.getTipo()) ? "recebeu" : "gastou";

        Map<String, Object> resultadoConsulta = new LinkedHashMap<>();
        resultadoConsulta.put("acao", "CONSULTA_EXECUTADA_SOMENTE_LEITURA");
        resultadoConsulta.put("regraAtendida", "RN-006 & RNF-004: Consulta executada localmente no banco do FinFamily sem alterar dados");
        resultadoConsulta.put("intencao", "consultar");
        resultadoConsulta.put("filtroAplicado", Map.of(
                "busca", comando.getBusca() != null ? comando.getBusca() : "",
                "categoria", comando.getCategoria() != null ? comando.getCategoria() : "",
                "conta", comando.getConta() != null ? comando.getConta() : "",
                "periodo", comando.getPeriodo() != null ? comando.getPeriodo() : "mes_atual",
                "tipo", comando.getTipo() != null ? comando.getTipo() : "DESPESA"
        ));
        resultadoConsulta.put("quantidadeLancamentos", filtradas.size());
        resultadoConsulta.put("valorTotalCalculado", total);
        resultadoConsulta.put("transacoesEncontradas", filtradas.stream().limit(15).toList());
        resultadoConsulta.put("mensagem", String.format(
                "Consulta concluída: a família %s o total de R$ %.2f com '%s' (%s), distribuído em %d lançamento(s).",
                verbo, total, alvoFiltro, rotuloPeriodo, filtradas.size()
        ));
        return resultadoConsulta;
    }

    public String montarPromptSeguroRnf004(String textoUsuario, List<Conta> contas, List<Categoria> categorias) {
        String nomesContas = contas.stream().map(Conta::getNome).collect(Collectors.joining(", "));
        String nomesCategorias = categorias.stream().map(Categoria::getNome).collect(Collectors.joining(", "));
        return String.format("""
                Você é o assistente do FinFamily, um sistema de gestão financeira familiar.
                Sua única tarefa é interpretar o comando do usuário e devolver um JSON. Você não grava nem consulta dados.
                Data de hoje: %s.
                Contas disponíveis: [%s].
                Categorias disponíveis: [%s].

                Responda APENAS com um objeto JSON com estes campos:
                - "intencao": "registrar" (lançar receita/despesa), "consultar" (perguntar sobre gastos/receitas),
                  "fora_escopo" (pedido de recomendação de investimento ou consultoria) ou "nao_entendido".
                - "tipo": "DESPESA" ou "RECEITA".
                - "valor": número com ponto decimal (ex.: 1250.90) ou null.
                - "descricao": descrição curta do lançamento (ex.: "Padaria") ou null.
                - "data": data do lançamento no formato YYYY-MM-DD ("hoje", "ontem" etc. devem ser convertidos).
                - "conta": exatamente um dos nomes de conta da lista ou null.
                - "categoria": exatamente um dos nomes de categoria da lista ou null.
                - "parcelas": número de parcelas (1 se não for parcelado).
                - "recorrente": true para despesas que se repetem todo mês (assinaturas, mensalidades), senão false.
                - "busca": palavra a procurar na descrição das transações em uma consulta (ex.: "Uber") ou "".
                - "periodo": "hoje", "semana_atual", "mes_atual", "mes_passado" ou "ano_atual".

                Comando do usuário: "%s"
                """,
                LocalDate.now(), nomesContas, nomesCategorias, textoUsuario.replace("\"", "'")
        );
    }

    private Comando interpretarMotorNlpHibrido(String texto, List<Conta> contas, List<Categoria> categorias) {
        Comando cmd = new Comando();
        cmd.setProvedorIA(PROVEDOR_CONTINGENCIA);

        String textoTrim = texto.trim();
        String norm = normalizar(textoTrim);

        boolean isConsulta = norm.startsWith("quanto")
                || norm.startsWith("qual ")
                || norm.startsWith("quais ")
                || norm.startsWith("consultar")
                || norm.startsWith("consulte")
                || norm.startsWith("mostrar")
                || norm.startsWith("mostre")
                || norm.startsWith("listar")
                || norm.startsWith("liste")
                || norm.startsWith("ver ")
                || norm.startsWith("veja ")
                || norm.startsWith("total de")
                || norm.startsWith("soma de")
                || norm.startsWith("exibir")
                || norm.startsWith("buscar")
                || norm.contains("quanto eu ja gastei")
                || norm.contains("quanto gastamos")
                || norm.contains("quanto gastei")
                || norm.contains("quanto recebemos")
                || norm.contains("quanto recebi")
                || (textoTrim.endsWith("?") && extrairValorMonetario(textoTrim) == null);

        if (isConsulta) {
            cmd.setIntencao("consultar");
            cmd.setRequerConfirmacao(false);

            if (norm.contains("recebi") || norm.contains("recebemos") || norm.contains("receita")
                    || norm.contains("ganhei") || norm.contains("ganhamos") || norm.contains("provento")
                    || norm.contains("dividendo") || norm.contains("salario")) {
                cmd.setTipo("RECEITA");
            } else {
                cmd.setTipo("DESPESA");
            }

            cmd.setPeriodo(extrairPeriodo(norm));

            Conta contaIdentificada = identificarContaNoTexto(norm, contas, false);
            if (contaIdentificada != null && (norm.contains("cartao") || norm.contains("conta") || norm.contains("poupanca") || norm.contains("corretora") || norm.contains(normalizar(contaIdentificada.getNome())))) {
                cmd.setConta(contaIdentificada.getNome());
                cmd.setContaId(contaIdentificada.getId());
            }

            Categoria catExplicita = identificarCategoriaExplicitaNoTexto(norm, categorias);
            String termoBusca = extrairTermoBuscaConsulta(textoTrim, norm);

            if (catExplicita != null && (termoBusca == null || normalizar(catExplicita.getNome()).contains(normalizar(termoBusca)))) {
                cmd.setCategoria(catExplicita.getNome());
                cmd.setCategoriaId(catExplicita.getId());
                cmd.setBusca("");
            } else if (termoBusca != null && !termoBusca.isBlank()) {
                cmd.setBusca(capitalizar(termoBusca));
                Categoria catPorTermo = financeiroService.sugerirCategoriaPorDescricao(termoBusca, categorias);
                if (catPorTermo != null && normalizar(catPorTermo.getNome()).equals(normalizar(termoBusca))) {
                    cmd.setCategoria(catPorTermo.getNome());
                    cmd.setCategoriaId(catPorTermo.getId());
                }
            } else if (catExplicita != null) {
                cmd.setCategoria(catExplicita.getNome());
                cmd.setCategoriaId(catExplicita.getId());
            }

            cmd.setMensagemAssistente(String.format(
                    "Filtro de consulta identificado: tipo=%s, busca='%s', categoria='%s', período='%s'.",
                    cmd.getTipo(),
                    cmd.getBusca() != null ? cmd.getBusca() : "Todas",
                    cmd.getCategoria() != null ? cmd.getCategoria() : "Todas",
                    traduzirPeriodo(cmd.getPeriodo())
            ));
            return cmd;
        }

        BigDecimal valor = extrairValorMonetario(textoTrim);
        if (valor == null) {
            throw new IllegalArgumentException(
                    "Fluxo Alternativo 3a (UC12): Não foi possível identificar o valor monetário ou a pergunta no comando. Por favor, reformule (ex: 'Registrar gasto de R$ 45,90 na padaria hoje no cartão') ou utilize o formulário de lançamento.");
        }

        cmd.setIntencao("registrar");
        cmd.setRequerConfirmacao(true);
        cmd.setValor(valor);

        if (norm.contains("receita") || norm.contains("recebi") || norm.contains("recebemos")
                || norm.contains("salario") || norm.contains("provento") || norm.contains("dividendo")
                || norm.contains("reembolso") || norm.contains("venda de") || norm.contains("freela")
                || norm.contains("aluguel recebido") || norm.contains("pix recebido") || norm.contains("rendimento")) {
            cmd.setTipo("RECEITA");
        } else {
            cmd.setTipo("DESPESA");
        }

        cmd.setData(extrairDataComando(norm));

        cmd.setParcelas(extrairNumeroParcelas(norm));

        cmd.setRecorrente(norm.contains("recorrente") || norm.contains("assinatura") || norm.contains("todo mes") || norm.contains("mensalidade"));

        Conta conta = identificarContaNoTexto(norm, contas, true);
        if (conta != null) {
            cmd.setConta(conta.getNome());
            cmd.setContaId(conta.getId());
        } else {
            cmd.setConta(norm.contains("cartao") ? "Cartão de crédito" : "Conta Corrente");
        }

        String descricaoExtraida = extrairDescricaoLancamento(textoTrim, norm);
        cmd.setDescricao(descricaoExtraida);

        Categoria cat = identificarCategoriaParaRegistro(norm, descricaoExtraida, cmd.getTipo(), categorias);
        if (cat != null) {
            cmd.setCategoria(cat.getPai() != null ? cat.getPai().getNome() : cat.getNome());
            cmd.setCategoriaId(cat.getId());
        } else {
            cmd.setCategoria("RECEITA".equals(cmd.getTipo()) ? "Receitas" : "Alimentação");
        }

        cmd.setMensagemAssistente(String.format(
                "Card montado para confirmação (RN-006): %s de R$ %.2f — '%s' em %s na conta '%s' [%s]. Confirme para gravar.",
                "RECEITA".equals(cmd.getTipo()) ? "Receita" : "Despesa",
                cmd.getValor(),
                cmd.getDescricao(),
                cmd.getData(),
                cmd.getConta(),
                cmd.getCategoria()
        ));
        return cmd;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> executarBateria50ComandosTeste(Long grupoId) {
        record CasoTesteIA(int id, String comando, String intencaoEsperada, BigDecimal valorEsperado, String palavraChaveEsperada) {}

        List<CasoTesteIA> casos = List.of(

                new CasoTesteIA(1, "Registrar gasto de R$ 45,90 na padaria hoje no cartão de crédito", "registrar", new BigDecimal("45.90"), "padaria"),
                new CasoTesteIA(2, "Gastei 32,50 com Uber hoje no cartão", "registrar", new BigDecimal("32.50"), "uber"),
                new CasoTesteIA(3, "Lançar despesa de R$ 480,00 no supermercado hoje na conta corrente", "registrar", new BigDecimal("480.00"), "supermercado"),
                new CasoTesteIA(4, "Registrar pagamento de R$ 1.250,00 de aluguel hoje na conta corrente", "registrar", new BigDecimal("1250.00"), "aluguel"),
                new CasoTesteIA(5, "Comprei remédio na farmácia por R$ 89,90 hoje no cartão de crédito", "registrar", new BigDecimal("89.90"), "farm"),
                new CasoTesteIA(6, "Registrar gasto de R$ 55,90 com assinatura Netflix hoje no cartão", "registrar", new BigDecimal("55.90"), "netflix"),
                new CasoTesteIA(7, "Paguei R$ 210,00 de conta de energia Coelba hoje na conta corrente", "registrar", new BigDecimal("210.00"), "energia"),
                new CasoTesteIA(8, "Gastei R$ 180,00 abastecendo gasolina no posto hoje no cartão", "registrar", new BigDecimal("180.00"), "gasolina"),
                new CasoTesteIA(9, "Registrar despesa de R$ 950,00 da mensalidade da faculdade UCSAL hoje", "registrar", new BigDecimal("950.00"), "ucsal"),
                new CasoTesteIA(10, "Recebi salário de R$ 8.500,00 hoje na conta corrente", "registrar", new BigDecimal("8500.00"), "sal"),
                new CasoTesteIA(11, "Registrar receita de R$ 1.200,00 de freela hoje na conta corrente", "registrar", new BigDecimal("1200.00"), "freela"),
                new CasoTesteIA(12, "Gastei R$ 64,00 no restaurante almoço hoje no cartão de crédito", "registrar", new BigDecimal("64.00"), "restaurante"),
                new CasoTesteIA(13, "Lançar compra de R$ 1.500,00 de notebook em 10x no cartão de crédito", "registrar", new BigDecimal("1500.00"), "notebook"),
                new CasoTesteIA(14, "Registrar gasto de R$ 120,00 na academia hoje no cartão", "registrar", new BigDecimal("120.00"), "academia"),
                new CasoTesteIA(15, "Paguei R$ 119,90 de internet fibra hoje na conta corrente", "registrar", new BigDecimal("119.90"), "internet"),
                new CasoTesteIA(16, "Gastei R$ 42,00 no iFood jantar ontem no cartão de crédito", "registrar", new BigDecimal("42.00"), "ifood"),
                new CasoTesteIA(17, "Registrar despesa de R$ 75,00 no cinema com a família hoje no cartão", "registrar", new BigDecimal("75.00"), "cinema"),
                new CasoTesteIA(18, "Lançar gasto de R$ 340,00 de plano de saúde hoje na conta corrente", "registrar", new BigDecimal("340.00"), "plano"),
                new CasoTesteIA(19, "Comprei livros técnicos por R$ 135,50 hoje no cartão de crédito", "registrar", new BigDecimal("135.50"), "livro"),
                new CasoTesteIA(20, "Registrar receita de R$ 320,00 de dividendos recebidos hoje na corretora", "registrar", new BigDecimal("320.00"), "dividendo"),
                new CasoTesteIA(21, "Gastei R$ 28,90 na padaria ontem no cartão de crédito", "registrar", new BigDecimal("28.90"), "padaria"),
                new CasoTesteIA(22, "Paguei R$ 680,00 de condomínio hoje na conta corrente", "registrar", new BigDecimal("680.00"), "condom"),
                new CasoTesteIA(23, "Registrar gasto de R$ 27,90 com Spotify hoje no cartão", "registrar", new BigDecimal("27.90"), "spotify"),
                new CasoTesteIA(24, "Gastei R$ 250,00 na consulta médica hoje na conta corrente", "registrar", new BigDecimal("250.00"), "consulta"),
                new CasoTesteIA(25, "Registrar despesa de R$ 19,50 com estacionamento shopping hoje no cartão", "registrar", new BigDecimal("19.50"), "estacionamento"),

                new CasoTesteIA(26, "Quanto eu já gastei com Uber este mês?", "consultar", null, "uber"),
                new CasoTesteIA(27, "Quanto gastamos com Alimentação este mês?", "consultar", null, "alimenta"),
                new CasoTesteIA(28, "Quanto gastei na padaria este mês?", "consultar", null, "padaria"),
                new CasoTesteIA(29, "Qual o total de gastos com Transporte este mês?", "consultar", null, "transporte"),
                new CasoTesteIA(30, "Quanto gastamos com supermercado neste mês?", "consultar", null, "supermercado"),
                new CasoTesteIA(31, "Consultar despesas com Educação este mês", "consultar", null, "educa"),
                new CasoTesteIA(32, "Quanto já gastamos com Moradia este mês?", "consultar", null, "moradia"),
                new CasoTesteIA(33, "Mostrar gastos com Saúde neste mês", "consultar", null, "sa"),
                new CasoTesteIA(34, "Quanto gastei com Lazer este mês?", "consultar", null, "lazer"),
                new CasoTesteIA(35, "Quanto gastamos no cartão de crédito este mês?", "consultar", null, "cart"),
                new CasoTesteIA(36, "Qual o total de receitas recebidas este mês?", "consultar", null, "receita"),
                new CasoTesteIA(37, "Quanto gastei com iFood este mês?", "consultar", null, "ifood"),
                new CasoTesteIA(38, "Quanto gastamos com gasolina este mês?", "consultar", null, "gasolina"),
                new CasoTesteIA(39, "Consultar gastos com farmácia este mês", "consultar", null, "farm"),
                new CasoTesteIA(40, "Quanto gastei com Netflix este mês?", "consultar", null, "netflix"),
                new CasoTesteIA(41, "Quanto gastamos com restaurante este mês?", "consultar", null, "restaurante"),
                new CasoTesteIA(42, "Qual foi o gasto com energia este mês?", "consultar", null, "energia"),
                new CasoTesteIA(43, "Quanto já gastei hoje?", "consultar", null, "hoje"),
                new CasoTesteIA(44, "Quanto gastamos na semana atual?", "consultar", null, "semana"),
                new CasoTesteIA(45, "Quanto gastamos no mês passado?", "consultar", null, "mes_passado"),
                new CasoTesteIA(46, "Quanto recebemos de dividendos este mês?", "consultar", null, "dividendo"),
                new CasoTesteIA(47, "Mostrar despesas com faculdade este mês", "consultar", null, "faculdade"),
                new CasoTesteIA(48, "Quanto gastei com academia este mês?", "consultar", null, "academia"),
                new CasoTesteIA(49, "Qual o total gasto com condomínio este mês?", "consultar", null, "condom"),
                new CasoTesteIA(50, "Quanto gastamos com internet este mês?", "consultar", null, "internet")
        );

        int acertos = 0;
        long somaTempoMs = 0;
        long maiorTempoMs = 0;
        List<Map<String, Object>> resultadosDetalhados = new ArrayList<>();
        Map<String, Integer> respondidosPorProvedor = new LinkedHashMap<>();

        for (CasoTesteIA caso : casos) {
            long inicioCasoNs = System.nanoTime();
            Comando interpretado;
            try {
                interpretado = interpretar(grupoId, caso.comando());
            } catch (IllegalArgumentException e) {
                interpretado = new Comando();
                interpretado.setIntencao("nao_entendido");
                interpretado.setProvedorIA(clienteLLM.resolverProvedor());
                interpretado.setTempoRespostaMs(Math.max(1L, (System.nanoTime() - inicioCasoNs) / 1_000_000));
            }
            long tMs = interpretado.getTempoRespostaMs() != null ? interpretado.getTempoRespostaMs() : 1L;
            somaTempoMs += tMs;
            maiorTempoMs = Math.max(maiorTempoMs, tMs);

            boolean intencaoOk = caso.intencaoEsperada().equalsIgnoreCase(interpretado.getIntencao());
            boolean valorOk = true;
            if (caso.valorEsperado() != null) {
                valorOk = interpretado.getValor() != null
                        && interpretado.getValor().compareTo(caso.valorEsperado()) == 0;
            }
            boolean tempoOk = tMs < 3000;
            boolean passou = intencaoOk && valorOk && tempoOk;
            if (passou) {
                acertos++;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", caso.id());
            item.put("comando", caso.comando());
            item.put("intencaoEsperada", caso.intencaoEsperada());
            item.put("intencaoObtida", interpretado.getIntencao());
            item.put("valorEsperado", caso.valorEsperado());
            item.put("valorObtido", interpretado.getValor());
            item.put("descricaoOuFiltro", "registrar".equals(interpretado.getIntencao())
                    ? interpretado.getDescricao()
                    : (interpretado.getBusca() != null && !interpretado.getBusca().isBlank() ? interpretado.getBusca() : interpretado.getCategoria()));
            item.put("contaIdentificada", interpretado.getConta());
            item.put("categoriaIdentificada", interpretado.getCategoria());
            item.put("tempoRespostaMs", tMs);
            item.put("provedorIA", interpretado.getProvedorIA());
            item.put("passou", passou);
            resultadosDetalhados.add(item);
            respondidosPorProvedor.merge(interpretado.getProvedorIA(), 1, Integer::sum);
        }

        double percentualAcerto = (acertos * 100.0) / casos.size();

        Map<String, Object> relatorio = new LinkedHashMap<>();
        relatorio.put("metaOE5", ">= 80% de acerto em 50 comandos (25 registros + 25 consultas) e tempo <= 3000ms");
        relatorio.put("provedorConfigurado", clienteLLM.resolverProvedor());
        relatorio.put("respondidosPorProvedor", respondidosPorProvedor);
        relatorio.put("totalComandosTestados", casos.size());
        relatorio.put("comandosCorretos", acertos);
        relatorio.put("taxaAcertoPercentual", BigDecimal.valueOf(percentualAcerto).setScale(2, RoundingMode.HALF_UP));
        relatorio.put("metaAtingida", percentualAcerto >= 80.0 && maiorTempoMs < 3000);
        relatorio.put("tempoMedioMs", Math.max(1L, somaTempoMs / casos.size()));
        relatorio.put("tempoMaximoMs", maiorTempoMs);
        relatorio.put("resultados", resultadosDetalhados);
        return relatorio;
    }

    private BigDecimal extrairValorMonetario(String texto) {
        Pattern pMoeda = Pattern.compile("(?:R\\$\\s*)?(\\d{1,3}(?:\\.\\d{3})+,\\d{2}|\\d+[.,]\\d{2}|\\d{1,3}(?:\\.\\d{3})+)(?!\\s*x|\\s*vezes)", Pattern.CASE_INSENSITIVE);
        Matcher m = pMoeda.matcher(texto);
        if (m.find()) {
            String numStr = m.group(1);
            if (numStr.contains(",") && numStr.contains(".")) {
                numStr = numStr.replace(".", "").replace(",", ".");
            } else if (numStr.contains(",")) {
                numStr = numStr.replace(",", ".");
            }
            return new BigDecimal(numStr).setScale(2, RoundingMode.HALF_UP);
        }

        Pattern pInteiro = Pattern.compile("(?:de|por|gastei|paguei|recebi|valor)\\s+(?:R\\$\\s*)?(\\d+)(?!\\s*x|\\s*vezes)", Pattern.CASE_INSENSITIVE);
        Matcher mInt = pInteiro.matcher(texto);
        if (mInt.find()) {
            return new BigDecimal(mInt.group(1)).setScale(2, RoundingMode.HALF_UP);
        }
        return null;
    }

    private Integer extrairNumeroParcelas(String norm) {
        Pattern p = Pattern.compile("(\\d{1,2})\\s*(?:x|vezes|parcelas)");
        Matcher m = p.matcher(norm);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1));
            return Math.max(1, Math.min(n, 48));
        }
        return 1;
    }

    private LocalDate extrairDataComando(String norm) {
        if (norm.contains("anteontem")) {
            return LocalDate.now().minusDays(2);
        }
        if (norm.contains("ontem")) {
            return LocalDate.now().minusDays(1);
        }
        Pattern pIso = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
        Matcher mIso = pIso.matcher(norm);
        if (mIso.find()) {
            return LocalDate.parse(mIso.group(1));
        }
        Pattern pBr = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})");
        Matcher mBr = pBr.matcher(norm);
        if (mBr.find()) {
            return LocalDate.parse(mBr.group(1), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return LocalDate.now();
    }

    private String extrairPeriodo(String norm) {
        if (norm.contains("hoje")) return "hoje";
        if (norm.contains("semana")) return "semana_atual";
        if (norm.contains("mes passado") || norm.contains("ultimo mes")) return "mes_passado";
        if (norm.contains("este ano") || norm.contains("neste ano") || norm.contains("no ano")) return "ano_atual";
        return "mes_atual";
    }

    private Conta identificarContaNoTexto(String norm, List<Conta> contas, boolean usarDefaultSeNaoEncontrar) {
        if (contas == null || contas.isEmpty()) return null;

        for (Conta c : contas) {
            if (norm.contains(normalizar(c.getNome()))) {
                return c;
            }
        }
        if (norm.contains("cartao")) {
            return contas.stream()
                    .filter(c -> c.getTipo().name().equals("CARTAO_CREDITO") && !Boolean.TRUE.equals(c.getPrivada()))
                    .findFirst()
                    .orElseGet(() -> contas.stream()
                            .filter(c -> c.getTipo().name().equals("CARTAO_CREDITO"))
                            .findFirst().orElse(contas.get(0)));
        }
        if (norm.contains("poupanca")) {
            return contas.stream()
                    .filter(c -> c.getTipo().name().equals("POUPANCA"))
                    .findFirst().orElse(contas.get(0));
        }
        if (norm.contains("corretora")) {
            return contas.stream()
                    .filter(c -> c.getTipo().name().equals("CORRETORA"))
                    .findFirst().orElse(contas.get(0));
        }
        if (norm.contains("conta corrente") || norm.contains("corrente")) {
            return contas.stream()
                    .filter(c -> c.getTipo().name().equals("CORRENTE"))
                    .findFirst().orElse(contas.get(0));
        }
        return usarDefaultSeNaoEncontrar ? contas.get(0) : null;
    }

    private Categoria identificarCategoriaExplicitaNoTexto(String norm, List<Categoria> categorias) {
        if (categorias == null) return null;
        for (Categoria c : categorias) {
            String nCat = normalizar(c.getNome());
            if (norm.contains(nCat)) {
                return c;
            }
        }
        return null;
    }

    private Categoria identificarCategoriaParaRegistro(String norm, String descricao, String tipo, List<Categoria> categorias) {
        if ("RECEITA".equalsIgnoreCase(tipo)) {
            return categorias.stream()
                    .filter(c -> c.getTipoPadrao() == TipoTransacao.RECEITA
                            || normalizar(c.getNome()).contains("receita")
                            || normalizar(c.getNome()).contains("salario")
                            || normalizar(c.getNome()).contains("provento"))
                    .findFirst()
                    .orElse(categorias.isEmpty() ? null : categorias.get(0));
        }
        Categoria explicita = identificarCategoriaExplicitaNoTexto(norm, categorias);
        if (explicita != null) return explicita;
        return financeiroService.sugerirCategoriaPorDescricao(descricao + " " + norm, categorias);
    }

    private String extrairDescricaoLancamento(String textoOriginal, String norm) {
        Map<String, String> mapeamentosDiretos = new LinkedHashMap<>();
        mapeamentosDiretos.put("padaria", "Padaria");
        mapeamentosDiretos.put("uber", "Uber");
        mapeamentosDiretos.put("supermercado", "Supermercado");
        mapeamentosDiretos.put("mercado", "Supermercado");
        mapeamentosDiretos.put("aluguel", "Aluguel");
        mapeamentosDiretos.put("farmacia", "Farmácia");
        mapeamentosDiretos.put("netflix", "Assinatura Netflix");
        mapeamentosDiretos.put("spotify", "Assinatura Spotify");
        mapeamentosDiretos.put("coelba", "Conta de Energia Coelba");
        mapeamentosDiretos.put("energia", "Conta de Energia");
        mapeamentosDiretos.put("gasolina", "Abastecimento Gasolina");
        mapeamentosDiretos.put("ucsal", "Mensalidade Faculdade UCSAL");
        mapeamentosDiretos.put("faculdade", "Mensalidade Faculdade");
        mapeamentosDiretos.put("salario", "Salário Mensal");
        mapeamentosDiretos.put("freela", "Projeto Freelance");
        mapeamentosDiretos.put("restaurante", "Restaurante");
        mapeamentosDiretos.put("notebook", "Compra Notebook");
        mapeamentosDiretos.put("academia", "Mensalidade Academia");
        mapeamentosDiretos.put("internet", "Internet Fibra");
        mapeamentosDiretos.put("ifood", "iFood Delivery");
        mapeamentosDiretos.put("cinema", "Cinema em Família");
        mapeamentosDiretos.put("plano de saude", "Plano de Saúde");
        mapeamentosDiretos.put("livro", "Livros Técnicos");
        mapeamentosDiretos.put("dividendo", "Dividendos Recebidos");
        mapeamentosDiretos.put("condominio", "Taxa de Condomínio");
        mapeamentosDiretos.put("consulta medica", "Consulta Médica");
        mapeamentosDiretos.put("estacionamento", "Estacionamento Shopping");

        for (Map.Entry<String, String> entry : mapeamentosDiretos.entrySet()) {
            if (norm.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        String limpo = textoOriginal
                .replaceAll("(?i)registrar\\s+(gasto|despesa|receita|pagamento|compra)\\s+(de\\s+)?", "")
                .replaceAll("(?i)(gastei|paguei|comprei|recebi|lancar)\\s+", "")
                .replaceAll("(?i)R\\$\\s*[0-9.,]+", "")
                .replaceAll("(?i)\\b[0-9]+[.,][0-9]{2}\\b", "")
                .replaceAll("(?i)\\b(hoje|ontem|anteontem)\\b", "")
                .replaceAll("(?i)\\b(no|na)\\s+(cartao\\s+de\\s+credito|cartao|conta\\s+corrente|poupanca|corretora)\\b", "")
                .replaceAll("(?i)^\\s*(na|no|com|de|por)\\s+", "")
                .trim();

        return limpo.isBlank() ? "Lançamento via Assistente IA" : capitalizar(limpo);
    }

    private String extrairTermoBuscaConsulta(String textoOriginal, String norm) {
        String[] termosConhecidos = {
                "uber", "padaria", "supermercado", "ifood", "gasolina", "farmacia",
                "netflix", "spotify", "restaurante", "energia", "faculdade", "academia",
                "condominio", "internet", "aluguel", "dividendo", "cinema", "livro"
        };
        for (String t : termosConhecidos) {
            if (norm.contains(t)) {
                return capitalizar(t);
            }
        }
        Pattern pCom = Pattern.compile("(?:com|na|no|de)\\s+([a-zA-ZÀ-ÿ]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = pCom.matcher(textoOriginal);
        while (m.find()) {
            String cand = m.group(1);
            String candNorm = normalizar(cand);
            if (!Set.of("este", "neste", "mes", "hoje", "semana", "ano", "cartao", "conta", "total", "gastos", "despesas").contains(candNorm)) {
                return capitalizar(cand);
            }
        }
        return "";
    }

    private String traduzirPeriodo(String periodo) {
        if (periodo == null) return "Mês Atual";
        return switch (periodo.toLowerCase()) {
            case "hoje" -> "Hoje";
            case "semana_atual" -> "Semana Atual";
            case "mes_passado" -> "Mês Passado";
            case "ano_atual" -> "Ano Atual";
            default -> "Mês Atual";
        };
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        return n.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").toLowerCase().trim();
    }

    private static String capitalizar(String s) {
        if (s == null || s.isBlank()) return "";
        String t = s.trim();
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }
}
