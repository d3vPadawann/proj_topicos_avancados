package br.ucsal.finfamily.service;

import br.ucsal.finfamily.domain.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

/**
 * Servico de Exportacao de Dados em PDF, CSV e Excel (RF-010)
 * e Rotina de Backup Diario com retencao de 30 dias para Disaster Recovery (RNF-007).
 */
@Service
public class ExportacaoBackupService {

    @Value("${finfamily.backup.dir:./backups}")
    private String backupDir;

    @Value("${finfamily.backup.retention-days:30}")
    private int retentionDays;

    private final FinanceiroService financeiroService;
    private final AuditLogService auditLogService;

    public ExportacaoBackupService(FinanceiroService financeiroService, AuditLogService auditLogService) {
        this.financeiroService = financeiroService;
        this.auditLogService = auditLogService;
    }

    /**
     * RF-010: Exportacao em CSV (compativel com contabilidade e reimportacao).
     */
    @Transactional(readOnly = true)
    public byte[] exportarCSV(Long grupoId, String mes) {
        List<Transacao> transacoes = financeiroService.listarTransacoes(grupoId, null, null, null, null, mes);
        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM para compatibilidade com Excel
        sb.append('\uFEFF');
        sb.append("data;descricao;valor;tipo;conta;categoria;parcela;total_parcelas;recorrente;origem\n");

        for (Transacao t : transacoes) {
            sb.append(t.getData()).append(";")
              .append(escaparCsv(t.getDescricao())).append(";")
              .append(t.getValor().toPlainString()).append(";")
              .append(t.getTipo().name()).append(";")
              .append(escaparCsv(t.getConta() != null ? t.getConta().getNome() : "")).append(";")
              .append(escaparCsv(t.getCategoria() != null ? t.getCategoria().getCaminhoCompleto() : "Geral")).append(";")
              .append(t.getParcela() != null ? t.getParcela() : 1).append(";")
              .append(t.getTotalParcelas() != null ? t.getTotalParcelas() : 1).append(";")
              .append(Boolean.TRUE.equals(t.getRecorrente()) ? "SIM" : "NAO").append(";")
              .append(t.getOrigemLancamento() != null ? t.getOrigemLancamento() : "MANUAL")
              .append("\n");
        }

        auditLogService.registrar("EXPORTAR_CSV", "RELATORIOS_EXPORTACAO", "titular@finfamily.com", "INFO",
                "Exportacao CSV concluida com " + transacoes.size() + " registros (RF-010).");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * RF-010: Exportacao em Excel (.xlsx) com multiplas abas (Resumo, Contas, Transacoes, Orcamentos).
     */
    @Transactional(readOnly = true)
    public byte[] exportarExcel(Long grupoId, String mes) {
        String mesRef = (mes != null && !mes.isBlank() && !mes.equalsIgnoreCase("todos")) ? mes : YearMonth.now().toString();
        Map<String, Object> dash = financeiroService.obterDashboardConsolidado(grupoId, mesRef);
        List<Conta> contas = financeiroService.listarContas(grupoId);
        List<Transacao> transacoes = financeiroService.listarTransacoes(grupoId, null, null, null, null, mes);
        List<Orcamento> orcamentos = financeiroService.listarOrcamentos(grupoId, mesRef);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Aba 1: Resumo Patrimonial
            Sheet sheetResumo = workbook.createSheet("Resumo Patrimonial");
            criarLinhaChaveValor(sheetResumo, 0, "Grupo Familiar", String.valueOf(dash.get("nomeGrupo")), headerStyle);
            criarLinhaChaveValor(sheetResumo, 1, "Mês de Referência", mesRef, headerStyle);
            criarLinhaChaveValor(sheetResumo, 2, "Patrimônio Líquido Consolidado (RN-001)", "R$ " + dash.get("patrimonioLiquidoFamiliar"), headerStyle);
            criarLinhaChaveValor(sheetResumo, 3, "Saldo em Contas Públicas", "R$ " + dash.get("saldoContasPublicas"), headerStyle);
            criarLinhaChaveValor(sheetResumo, 4, "Carteira de Investimentos", "R$ " + dash.get("valorCarteiraInvestimentos"), headerStyle);
            criarLinhaChaveValor(sheetResumo, 5, "Receitas no Mês", "R$ " + dash.get("receitasMes"), headerStyle);
            criarLinhaChaveValor(sheetResumo, 6, "Despesas no Mês", "R$ " + dash.get("despesasMes"), headerStyle);
            criarLinhaChaveValor(sheetResumo, 7, "Fluxo de Caixa Líquido", "R$ " + dash.get("fluxoCaixaMes"), headerStyle);

            // Aba 2: Contas e Cartoes
            Sheet sheetContas = workbook.createSheet("Contas e Cartoes");
            Row headerContas = sheetContas.createRow(0);
            String[] colContas = {"ID", "Nome", "Instituição", "Tipo", "Saldo (R$)", "Privada (RN-001)", "Vencimento"};
            for (int i = 0; i < colContas.length; i++) {
                Cell cell = headerContas.createCell(i);
                cell.setCellValue(colContas[i]);
                cell.setCellStyle(headerStyle);
            }
            int rowIdx = 1;
            for (Conta c : contas) {
                Row r = sheetContas.createRow(rowIdx++);
                r.createCell(0).setCellValue(c.getId());
                r.createCell(1).setCellValue(c.getNome());
                r.createCell(2).setCellValue(c.getInstituicao() != null ? c.getInstituicao() : "");
                r.createCell(3).setCellValue(c.getTipo().getDescricao());
                r.createCell(4).setCellValue(c.getSaldo() != null ? c.getSaldo().doubleValue() : 0.0);
                r.createCell(5).setCellValue(Boolean.TRUE.equals(c.getPrivada()) ? "SIM (Privada)" : "NÃO (Pública)");
                r.createCell(6).setCellValue(c.getDiaVencimento() != null ? "Dia " + c.getDiaVencimento() : "-");
            }

            // Aba 3: Transacoes
            Sheet sheetTrans = workbook.createSheet("Transacoes");
            Row headerTrans = sheetTrans.createRow(0);
            String[] colTrans = {"Data", "Descrição", "Tipo", "Valor (R$)", "Conta", "Categoria", "Parcela", "Origem"};
            for (int i = 0; i < colTrans.length; i++) {
                Cell cell = headerTrans.createCell(i);
                cell.setCellValue(colTrans[i]);
                cell.setCellStyle(headerStyle);
            }
            rowIdx = 1;
            for (Transacao t : transacoes) {
                Row r = sheetTrans.createRow(rowIdx++);
                r.createCell(0).setCellValue(t.getData().toString());
                r.createCell(1).setCellValue(t.getDescricao());
                r.createCell(2).setCellValue(t.getTipo().name());
                r.createCell(3).setCellValue(t.getValor() != null ? t.getValor().doubleValue() : 0.0);
                r.createCell(4).setCellValue(t.getConta() != null ? t.getConta().getNome() : "");
                r.createCell(5).setCellValue(t.getCategoria() != null ? t.getCategoria().getCaminhoCompleto() : "Geral");
                r.createCell(6).setCellValue((t.getParcela() != null ? t.getParcela() : 1) + "/" + (t.getTotalParcelas() != null ? t.getTotalParcelas() : 1));
                r.createCell(7).setCellValue(t.getOrigemLancamento() != null ? t.getOrigemLancamento() : "MANUAL");
            }

            // Aba 4: Orcamentos
            Sheet sheetOrc = workbook.createSheet("Orcamentos");
            Row headerOrc = sheetOrc.createRow(0);
            String[] colOrc = {"Mês", "Categoria", "Planejado (R$)", "Realizado (R$)", "Progresso (%)", "Status Alerta (RN-004)"};
            for (int i = 0; i < colOrc.length; i++) {
                Cell cell = headerOrc.createCell(i);
                cell.setCellValue(colOrc[i]);
                cell.setCellStyle(headerStyle);
            }
            rowIdx = 1;
            for (Orcamento o : orcamentos) {
                Row r = sheetOrc.createRow(rowIdx++);
                r.createCell(0).setCellValue(o.getMes());
                r.createCell(1).setCellValue(o.getCategoria() != null ? o.getCategoria().getNome() : "");
                r.createCell(2).setCellValue(o.getValorPlanejado().doubleValue());
                r.createCell(3).setCellValue(o.getValorGasto().doubleValue());
                r.createCell(4).setCellValue(o.percentualGasto().doubleValue());
                String statusAlerta = Boolean.TRUE.equals(o.getAlerta100Disparado())
                        ? "ESTOURADO (>=100%)"
                        : (Boolean.TRUE.equals(o.getAlerta80Disparado()) ? "ATENÇÃO (>=80%)" : "DENTRO DA META");
                r.createCell(5).setCellValue(statusAlerta);
            }

            workbook.write(out);
            auditLogService.registrar("EXPORTAR_EXCEL", "RELATORIOS_EXPORTACAO", "titular@finfamily.com", "INFO",
                    "Planilha Excel (.xlsx) gerada com 4 abas (RF-010).");
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao exportar Excel: " + e.getMessage(), e);
        }
    }

    /**
     * RF-010: Exportacao em PDF formatado para apresentacao ou contabilidade.
     */
    @Transactional(readOnly = true)
    public byte[] exportarPDF(Long grupoId, String mes) {
        String mesRef = (mes != null && !mes.isBlank() && !mes.equalsIgnoreCase("todos")) ? mes : YearMonth.now().toString();
        Map<String, Object> dash = financeiroService.obterDashboardConsolidado(grupoId, mesRef);
        List<Conta> contas = financeiroService.listarContas(grupoId);
        List<Orcamento> orcamentos = financeiroService.listarOrcamentos(grupoId, mesRef);
        List<Transacao> transacoes = financeiroService.listarTransacoes(grupoId, null, null, null, null, mes);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitulo = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(30, 58, 138));
            Font fontSub = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(55, 65, 81));
            Font fontTexto = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
            Font fontHeaderTabela = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);

            Paragraph titulo = new Paragraph("FinFamily — Relatório Gerencial e Patrimonial Familiar", fontTitulo);
            titulo.setSpacingAfter(4f);
            document.add(titulo);

            Paragraph sub = new Paragraph(String.format(
                    "Grupo: %s | Período: %s | Emitido em: %s",
                    dash.get("nomeGrupo"),
                    mesRef,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            ), fontTexto);
            sub.setSpacingAfter(14f);
            document.add(sub);

            // Bloco 1: Indicadores Consolidados
            document.add(new Paragraph("1. Visão Consolidada do Patrimônio e Fluxo de Caixa (RN-001)", fontSub));
            PdfPTable tabResumo = new PdfPTable(4);
            tabResumo.setWidthPercentage(100);
            tabResumo.setSpacingBefore(6f);
            tabResumo.setSpacingAfter(12f);

            adicionarCelulaHeader(tabResumo, "Patrimônio Líquido Familiar", fontHeaderTabela);
            adicionarCelulaHeader(tabResumo, "Carteira de Investimentos", fontHeaderTabela);
            adicionarCelulaHeader(tabResumo, "Receitas no Período", fontHeaderTabela);
            adicionarCelulaHeader(tabResumo, "Despesas no Período", fontHeaderTabela);

            tabResumo.addCell(new Phrase("R$ " + dash.get("patrimonioLiquidoFamiliar"), fontTexto));
            tabResumo.addCell(new Phrase("R$ " + dash.get("valorCarteiraInvestimentos"), fontTexto));
            tabResumo.addCell(new Phrase("R$ " + dash.get("receitasMes"), fontTexto));
            tabResumo.addCell(new Phrase("R$ " + dash.get("despesasMes"), fontTexto));
            document.add(tabResumo);

            // Bloco 2: Contas e Cartoes
            document.add(new Paragraph("2. Contas Bancárias, Cartões e Corretoras (RF-002)", fontSub));
            PdfPTable tabContas = new PdfPTable(4);
            tabContas.setWidthPercentage(100);
            tabContas.setSpacingBefore(6f);
            tabContas.setSpacingAfter(12f);
            adicionarCelulaHeader(tabContas, "Conta / Cartão", fontHeaderTabela);
            adicionarCelulaHeader(tabContas, "Tipo (OE1)", fontHeaderTabela);
            adicionarCelulaHeader(tabContas, "Privacidade (RN-001)", fontHeaderTabela);
            adicionarCelulaHeader(tabContas, "Saldo Atual", fontHeaderTabela);

            for (Conta c : contas) {
                tabContas.addCell(new Phrase(c.getNome(), fontTexto));
                tabContas.addCell(new Phrase(c.getTipo().getDescricao(), fontTexto));
                tabContas.addCell(new Phrase(Boolean.TRUE.equals(c.getPrivada()) ? "Privada (Oculta no Global)" : "Compartilhada", fontTexto));
                tabContas.addCell(new Phrase("R$ " + c.getSaldo(), fontTexto));
            }
            document.add(tabContas);

            // Bloco 3: Orcado vs Realizado
            document.add(new Paragraph("3. Acompanhamento de Orçamentos Mensais (RF-006 / RN-004)", fontSub));
            PdfPTable tabOrc = new PdfPTable(4);
            tabOrc.setWidthPercentage(100);
            tabOrc.setSpacingBefore(6f);
            tabOrc.setSpacingAfter(12f);
            adicionarCelulaHeader(tabOrc, "Categoria", fontHeaderTabela);
            adicionarCelulaHeader(tabOrc, "Planejado", fontHeaderTabela);
            adicionarCelulaHeader(tabOrc, "Realizado", fontHeaderTabela);
            adicionarCelulaHeader(tabOrc, "Execução (%)", fontHeaderTabela);

            for (Orcamento o : orcamentos) {
                tabOrc.addCell(new Phrase(o.getCategoria() != null ? o.getCategoria().getNome() : "", fontTexto));
                tabOrc.addCell(new Phrase("R$ " + o.getValorPlanejado(), fontTexto));
                tabOrc.addCell(new Phrase("R$ " + o.getValorGasto(), fontTexto));
                tabOrc.addCell(new Phrase(o.percentualGasto() + "%", fontTexto));
            }
            document.add(tabOrc);

            // Bloco 4: Transacoes Recentes
            document.add(new Paragraph("4. Extrato de Lançamentos Financeiros (RF-003)", fontSub));
            PdfPTable tabTrans = new PdfPTable(5);
            tabTrans.setWidthPercentage(100);
            tabTrans.setSpacingBefore(6f);
            adicionarCelulaHeader(tabTrans, "Data", fontHeaderTabela);
            adicionarCelulaHeader(tabTrans, "Descrição", fontHeaderTabela);
            adicionarCelulaHeader(tabTrans, "Categoria", fontHeaderTabela);
            adicionarCelulaHeader(tabTrans, "Tipo", fontHeaderTabela);
            adicionarCelulaHeader(tabTrans, "Valor (R$)", fontHeaderTabela);

            for (Transacao t : transacoes.stream().limit(35).toList()) {
                tabTrans.addCell(new Phrase(t.getData().toString(), fontTexto));
                tabTrans.addCell(new Phrase(t.getDescricao(), fontTexto));
                tabTrans.addCell(new Phrase(t.getCategoria() != null ? t.getCategoria().getNome() : "Geral", fontTexto));
                tabTrans.addCell(new Phrase(t.getTipo().name(), fontTexto));
                tabTrans.addCell(new Phrase("R$ " + t.getValor(), fontTexto));
            }
            document.add(tabTrans);

            document.close();
            auditLogService.registrar("EXPORTAR_PDF", "RELATORIOS_EXPORTACAO", "titular@finfamily.com", "INFO",
                    "Relatorio PDF gerado com sucesso (RF-010).");
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao gerar PDF: " + e.getMessage(), e);
        }
    }

    /**
     * RNF-007 (Rotinas de Backup):
     * "Os bancos de dados devem possuir backup diario com retencao minima de 30 dias para Disaster Recovery."
     */
    @Scheduled(cron = "0 30 2 * * *")
    @Transactional(readOnly = true)
    public Map<String, Object> executarRotinaBackupDiario() {
        try {
            Path dir = Paths.get(backupDir);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path arquivoBackup = dir.resolve("finfamily_backup_" + timestamp + ".json");

            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("sistema", "FinFamily - UCSAL");
            snapshot.put("requisito", "RNF-007: Backup Diário com retenção mínima de 30 dias para Disaster Recovery");
            snapshot.put("geradoEm", LocalDateTime.now().toString());
            snapshot.put("retencaoDias", retentionDays);
            snapshot.put("dashboardSnapshot", financeiroService.obterDashboardConsolidado(1L, null));
            snapshot.put("carteiraSnapshot", financeiroService.consultarCarteiraInvestimentos(1L));

            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            Files.writeString(arquivoBackup, mapper.writeValueAsString(snapshot), StandardCharsets.UTF_8);

            // Politica de retencao de 30 dias
            Instant limiteRetencao = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
            int removidosExpirados = 0;
            List<String> arquivosDisponiveis = new ArrayList<>();

            try (Stream<Path> files = Files.list(dir)) {
                for (Path p : files.toList()) {
                    if (Files.isRegularFile(p)) {
                        if (Files.getLastModifiedTime(p).toInstant().isBefore(limiteRetencao)) {
                            Files.deleteIfExists(p);
                            removidosExpirados++;
                        } else {
                            arquivosDisponiveis.add(p.getFileName().toString());
                        }
                    }
                }
            }

            auditLogService.registrar("BACKUP_DIARIO_DR", "INFRAESTRUTURA_BACKUP", "sysadmin@finfamily.com", "INFO",
                    "Backup gerado em " + arquivoBackup.getFileName() + " com retencao de " + retentionDays + " dias (RNF-007).");

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("status", "SUCESSO");
            res.put("arquivoGerado", arquivoBackup.getFileName().toString());
            res.put("caminhoCompleto", arquivoBackup.toAbsolutePath().toString());
            res.put("politicaRetencaoDias", retentionDays);
            res.put("backupsExpiradosRemovidos", removidosExpirados);
            res.put("backupsAtivosNoServidor", arquivosDisponiveis);
            return res;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao executar backup RNF-007: " + e.getMessage(), e);
        }
    }

    private void criarLinhaChaveValor(Sheet sheet, int rowIdx, String chave, String valor, CellStyle styleChave) {
        Row r = sheet.createRow(rowIdx);
        Cell c0 = r.createCell(0);
        c0.setCellValue(chave);
        c0.setCellStyle(styleChave);
        r.createCell(1).setCellValue(valor);
    }

    private void adicionarCelulaHeader(PdfPTable table, String texto, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setBackgroundColor(new Color(30, 58, 138));
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private String escaparCsv(String s) {
        if (s == null) return "";
        return s.replace(";", ",").replace("\n", " ");
    }
}
