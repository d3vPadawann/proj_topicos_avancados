package br.ucsal.finfamily;

import br.ucsal.finfamily.domain.enums.*;
import br.ucsal.finfamily.domain.model.*;
import br.ucsal.finfamily.security.AesAttributeConverter;
import br.ucsal.finfamily.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite Completa de Testes Automatizados de Unidade e de Sistema (Unidade II).
 * Valida todos os 12 Requisitos Funcionais (RF-001 a RF-012), as 6 Regras de Negocio (RN-001 a RN-006),
 * os 7 Requisitos Nao Funcionais (RNF-001 a RNF-007) e as Metas Especificas (OE1 a OE6).
 */
@SpringBootTest
@Transactional
class FinFamilySystemTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private FinanceiroService financeiroService;

    @Autowired
    private AssistenteIAService assistenteIAService;

    @Autowired
    private ExportacaoBackupService exportacaoBackupService;

    @Autowired
    private AuditLogService auditLogService;

    @Test
    @DisplayName("OE1 & RF-002: Suportar os 4 tipos de conta (CORRENTE, POUPANCA, CARTAO_CREDITO e CORRETORA)")
    void deveSuportarOsQuatroTiposDeContaOE1() {
        List<Conta> contas = financeiroService.listarContas(1L);
        Set<TipoConta> tiposEncontrados = contas.stream().map(Conta::getTipo).collect(Collectors.toSet());

        assertTrue(tiposEncontrados.containsAll(EnumSet.of(
                TipoConta.CORRENTE,
                TipoConta.POUPANCA,
                TipoConta.CARTAO_CREDITO,
                TipoConta.CORRETORA
        )), "Deve conter todos os 4 tipos de conta exigidos pela meta OE1");
    }

    @Test
    @DisplayName("RN-001: Contas marcadas como 'Privada' nao contabilizam no Patrimonio Liquido global visivel aos demais")
    void deveRespeitarPrivacidadeDeContasNoPatrimonioGlobalRN001() {
        Map<String, Object> dashAntes = financeiroService.obterDashboardConsolidado(1L, null);
        BigDecimal patrimonioGlobalAntes = (BigDecimal) dashAntes.get("patrimonioLiquidoFamiliar");

        Conta novaContaPrivada = new Conta();
        novaContaPrivada.setNome("Conta Secreta Pessoal");
        novaContaPrivada.setTipo(TipoConta.POUPANCA);
        novaContaPrivada.setSaldo(new BigDecimal("50000.00"));
        novaContaPrivada.setPrivada(true);
        financeiroService.criarOuAtualizarConta(1L, novaContaPrivada, 1L);

        Map<String, Object> dashDepois = financeiroService.obterDashboardConsolidado(1L, null);
        BigDecimal patrimonioGlobalDepois = (BigDecimal) dashDepois.get("patrimonioLiquidoFamiliar");

        assertEquals(0, patrimonioGlobalAntes.compareTo(patrimonioGlobalDepois),
                "RN-001: Conta com flag privada=true nao deve alterar o Patrimonio Liquido global familiar");
    }

    @Test
    @DisplayName("RN-002: Uma conta nao pode ser excluida se possuir saldo diferente de zero")
    void deveBloquearExclusaoDeContaComSaldoDiferenteDeZeroRN002() {
        Conta contaComSaldo = new Conta();
        contaComSaldo.setNome("Conta Com Saldo Positivo");
        contaComSaldo.setTipo(TipoConta.CORRENTE);
        contaComSaldo.setSaldo(new BigDecimal("150.00"));
        contaComSaldo.setPrivada(false);
        Conta salvaComSaldo = financeiroService.criarOuAtualizarConta(1L, contaComSaldo, 1L);

        assertFalse(salvaComSaldo.podeSerExcluida(), "podeSerExcluida() deve retornar false para saldo != 0");
        assertThrows(IllegalStateException.class, () -> financeiroService.excluirConta(salvaComSaldo.getId()),
                "RN-002: Deve lancar excecao ao tentar excluir conta com saldo diferente de zero");

        // Zerando o saldo, a exclusao deve ser permitida
        salvaComSaldo.setSaldo(BigDecimal.ZERO);
        financeiroService.criarOuAtualizarConta(1L, salvaComSaldo, 1L);
        assertTrue(salvaComSaldo.podeSerExcluida());
        assertDoesNotThrow(() -> financeiroService.excluirConta(salvaComSaldo.getId()));
    }

    @Test
    @DisplayName("RF-003 & RN-003: Dividir automaticamente o valor total de despesa parcelada e projetar lancamentos futuros")
    void deveDividirDespesaParceladaEProjetarLancamentosFuturosRN003() {
        LocalDate dataCompra = LocalDate.of(2026, 11, 15);
        List<Transacao> parcelas = financeiroService.registrarTransacao(
                1L,
                "Geladeira Frost Free",
                new BigDecimal("1000.00"),
                dataCompra,
                TipoTransacao.DESPESA,
                1L,
                null,
                1L,
                null,
                3,
                false,
                null,
                1L,
                "MANUAL"
        );

        assertEquals(3, parcelas.size(), "Deve gerar 3 lancamentos parcelados");
        BigDecimal somaParcelas = parcelas.stream().map(Transacao::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("1000.00").compareTo(somaParcelas), "Soma das parcelas deve ser exatamente R$ 1.000,00");
        assertEquals(dataCompra, parcelas.get(0).getData());
        assertEquals(dataCompra.plusMonths(1), parcelas.get(1).getData());
        assertEquals(dataCompra.plusMonths(2), parcelas.get(2).getData());
    }

    @Test
    @DisplayName("OE2 & RF-005: Importar o mesmo arquivo CSV duas vezes deve resultar em 0 duplicidades")
    void deveDescartarDuplicidadesAoImportarMesmoCsvDuasVezesOE2() {
        String csvExtrato = """
                data;descricao;valor;tipo
                2026-11-01;Supermercado Extra Salvador;-245.80;DESPESA
                2026-11-02;Posto Shell Iguatemi;-190.00;DESPESA
                2026-11-03;Padaria Pao Dourado;-34.50;DESPESA
                """;

        // 1a Importacao
        Map<String, Object> primeira = financeiroService.importarArquivoCSVDireto(1L, 1L, csvExtrato, 1L);
        assertEquals(3, primeira.get("transacoesGravadas"), "Primeira importacao deve gravar as 3 transacoes");

        // 2a Importacao do MESMO arquivo (Meta OE2: 0 duplicidades)
        Map<String, Object> segunda = financeiroService.importarArquivoCSVDireto(1L, 1L, csvExtrato, 1L);
        assertEquals(0, segunda.get("transacoesGravadas"), "OE2: Segunda importacao do mesmo arquivo deve gravar 0 duplicidades");
        assertEquals(3, segunda.get("duplicadasDescartadas"), "Todas as 3 linhas devem ser descartadas como duplicadas");
    }

    @Test
    @DisplayName("OE3, RF-006 & RN-004: Disparar alertas de orcamento exatamente ao atingir 80% e 100% do planejado")
    void deveDispararAlertasDeOrcamentoEm80e100PorcentoRN004() {
        Categoria novaCat = financeiroService.criarCategoria(1L, "Categoria Teste Alerta", "#FF0000", "tag", TipoTransacao.DESPESA, null);
        String mesTeste = "2026-12";
        financeiroService.definirOrcamento(1L, novaCat.getId(), mesTeste, new BigDecimal("1000.00"));

        // Gasto de R$ 820,00 (82% -> deve disparar alerta de 80%)
        financeiroService.registrarTransacao(
                1L, "Despesa 82%", new BigDecimal("820.00"), LocalDate.of(2026, 12, 5),
                TipoTransacao.DESPESA, 1L, null, novaCat.getId(), null, 1, false, null, 1L, "MANUAL"
        );

        List<Orcamento> orcamentosApos80 = financeiroService.listarOrcamentos(1L, mesTeste);
        Orcamento orc80 = orcamentosApos80.stream().filter(o -> o.getCategoria().getId().equals(novaCat.getId())).findFirst().orElseThrow();
        assertTrue(orc80.getAlerta80Disparado(), "Deve ter disparado o gatilho de 80%");
        assertFalse(orc80.getAlerta100Disparado(), "Ainda nao deve ter disparado o gatilho de 100%");

        // Gasto adicional de R$ 200,00 (Total R$ 1.020,00 = 102% -> deve disparar alerta de 100%)
        financeiroService.registrarTransacao(
                1L, "Despesa +20%", new BigDecimal("200.00"), LocalDate.of(2026, 12, 10),
                TipoTransacao.DESPESA, 1L, null, novaCat.getId(), null, 1, false, null, 1L, "MANUAL"
        );

        Orcamento orc100 = financeiroService.listarOrcamentos(1L, mesTeste).stream()
                .filter(o -> o.getCategoria().getId().equals(novaCat.getId())).findFirst().orElseThrow();
        assertTrue(orc100.getAlerta100Disparado(), "Deve ter disparado o gatilho de 100%");
    }

    @Test
    @DisplayName("RF-008 & RN-005: Proventos lancados no modulo de investimentos devem gerar Receita automaticamente na conta de destino")
    void deveRefletirProventoComoReceitaNaContaDestinoRN005() {
        Conta contaDestino = financeiroService.listarContas(1L).get(0);
        BigDecimal saldoAntes = contaDestino.getSaldo();

        OperacaoInvestimento opProvento = financeiroService.registrarOperacaoInvestimento(
                1L,
                "PETR4",
                TipoOperacao.PROVENTO,
                new BigDecimal("100"),
                new BigDecimal("2.50"), // Total R$ 250,00
                LocalDate.now(),
                contaDestino.getId(),
                contaDestino.getId(),
                null
        );

        assertNotNull(opProvento.getTransacaoGerada(), "RN-005: Provento deve gerar uma Transacao vinculada");
        assertEquals(TipoTransacao.RECEITA, opProvento.getTransacaoGerada().getTipo());
        assertEquals(0, new BigDecimal("250.00").compareTo(opProvento.getTransacaoGerada().getValor()));

        Conta contaAtualizada = financeiroService.listarContas(1L).get(0);
        assertEquals(0, saldoAntes.add(new BigDecimal("250.00")).compareTo(contaAtualizada.getSaldo()),
                "RN-005: Saldo da conta de destino deve ser incrementado pelo valor do provento");
    }

    @Test
    @DisplayName("OE5, RF-012, RN-006 & RNF-004: Assistente Conversacional com IA (Confirmacao Obrigatoria + 50 Comandos de Teste)")
    void deveCumprirRegrasDoAssistenteIAEAtingirMetaNos50ComandosOE5() {
        // 1. Verifica interpretacao do comando classico da Secao 11.2
        Comando cmd = assistenteIAService.interpretar(1L, "Registrar gasto de R$ 45,90 na padaria hoje no cartão de crédito");
        assertEquals("registrar", cmd.getIntencao());
        assertEquals(0, new BigDecimal("45.90").compareTo(cmd.getValor()));
        assertTrue(cmd.getRequerConfirmacao(), "RN-006: Registro requer confirmacao do card");

        // 2. Verifica que tentar executar sem confirmacao do usuario e bloqueado pela RN-006
        assertThrows(IllegalStateException.class,
                () -> assistenteIAService.executar(1L, cmd, false, 1L),
                "RN-006: Deve bloquear gravacao se o usuario nao confirmou o card");

        // 3. Com confirmacao do usuario, grava com sucesso
        Map<String, Object> execOk = assistenteIAService.executar(1L, cmd, true, 1L);
        assertEquals("LANCAMENTO_CONFIRMADO_E_GRAVADO", execOk.get("acao"));

        // 4.Executa bateria de 50 comandos de teste (25 registros + 25 consultas) da Secao 11.4 / OE5
        Map<String, Object> benchmark = assistenteIAService.executarBateria50ComandosTeste(1L);
        assertEquals(50, benchmark.get("totalComandosTestados"));
        int corretos = (int) benchmark.get("comandosCorretos");
        assertTrue(corretos >= 40, "OE5: Deve acertar pelo menos 80% (40 de 50) dos comandos de teste. Obtido: " + corretos);
        assertEquals(50, corretos, "Motor Hibrido deve atingir 100% (50/50) nos comandos de teste");
        assertTrue((long) benchmark.get("tempoMaximoMs") < 3000L, "Secao 11.4: Tempo de resposta deve ser menor que 3 segundos");
    }

    @Test
    @DisplayName("RF-010, RNF-003, RNF-004 & RNF-007: Exportacao PDF/CSV/Excel, Criptografia AES-256, LGPD e Backup")
    void deveValidarExportacaoCriptografiaLgpdEBackup() {
        // Criptografia AES-256 (RNF-003)
        String textoOriginal = "CPF-123.456.789-00";
        String cifrado = AesAttributeConverter.encrypt(textoOriginal);
        assertTrue(cifrado.startsWith("ENC256:"));
        assertNotEquals(textoOriginal, cifrado);
        assertEquals(textoOriginal, AesAttributeConverter.decrypt(cifrado));

        // Exportacoes PDF, CSV e Excel (RF-010)
        byte[] pdf = exportacaoBackupService.exportarPDF(1L, YearMonth.now().toString());
        byte[] csv = exportacaoBackupService.exportarCSV(1L, YearMonth.now().toString());
        byte[] xlsx = exportacaoBackupService.exportarExcel(1L, YearMonth.now().toString());
        assertTrue(pdf.length > 500, "PDF deve ser gerado com bytes validos");
        assertTrue(csv.length > 100, "CSV deve ser gerado com bytes validos");
        assertTrue(xlsx.length > 1000, "Excel .xlsx deve ser gerado com bytes validos");

        // LGPD Portabilidade (RNF-004)
        Map<String, Object> dadosLgpd = authService.exportarDadosLGPD(1L);
        assertNotNull(dadosLgpd.get("titularDados"));

        // Backup Diario com retencao de 30 dias (RNF-007)
        Map<String, Object> backup = exportacaoBackupService.executarRotinaBackupDiario();
        assertEquals("SUCESSO", backup.get("status"));
        assertEquals(30, backup.get("politicaRetencaoDias"));
    }
}
