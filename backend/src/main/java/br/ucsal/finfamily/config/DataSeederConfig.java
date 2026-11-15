package br.ucsal.finfamily.config;

import br.ucsal.finfamily.domain.enums.*;
import br.ucsal.finfamily.domain.model.*;
import br.ucsal.finfamily.repository.*;
import br.ucsal.finfamily.service.AuditLogService;
import br.ucsal.finfamily.service.FinanceiroService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Seeder automatico que popula o banco de dados com a Jornada Tipica de Uso (Secao 2.4 do Artigo)
 * e os 4 Perfis de Stakeholders (Secao 2.9): Titular, Membro, Auditor/Consultor e Sysadmin.
 */
@Configuration
public class DataSeederConfig {

    @Bean
    public CommandLineRunner inicializarDadosDemonstracao(
            UsuarioRepository usuarioRepository,
            GrupoFamiliarRepository grupoFamiliarRepository,
            MembroRepository membroRepository,
            ContaRepository contaRepository,
            CategoriaRepository categoriaRepository,
            TagRepository tagRepository,
            OrcamentoRepository orcamentoRepository,
            AtivoRepository ativoRepository,
            TransacaoRepository transacaoRepository,
            NotificacaoRepository notificacaoRepository,
            FinanceiroService financeiroService,
            AuditLogService auditLogService,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (usuarioRepository.count() > 0) {
                return;
            }

            String senhaPadraoHash = passwordEncoder.encode("123456");

            // 1. Usuarios representando os Perfis da Secao 2.9 e Equipe UCSAL
            Usuario titular = new Usuario("Eduardo Rocha Santos (Titular)", "eduardorocha.santos@ucsal.edu.br", senhaPadraoHash, PapelFamiliar.TITULAR);
            titular.setDocumentoSensivel("123.456.789-00");
            titular = usuarioRepository.save(titular);

            Usuario conjuge = new Usuario("Matheus Ramos de Oliveira (Cônjuge)", "matheusramos.oliveira@ucsal.edu.br", senhaPadraoHash, PapelFamiliar.MEMBRO);
            conjuge.setDocumentoSensivel("234.567.890-11");
            conjuge = usuarioRepository.save(conjuge);

            Usuario filho = new Usuario("Daniel Teixeira Melo (Filho Universitário)", "danielteixeira.melo@ucsal.edu.br", senhaPadraoHash, PapelFamiliar.MEMBRO);
            filho.setDocumentoSensivel("345.678.901-22");
            filho = usuarioRepository.save(filho);

            Usuario auditor = new Usuario("Prof. Elton Figueiredo (Consultor / Auditor)", "elton.figueiredo@pro.ucsal.edu.br", senhaPadraoHash, PapelFamiliar.AUDITOR);
            auditor.setDocumentoSensivel("456.789.012-33");
            auditor = usuarioRepository.save(auditor);

            Usuario sysadmin = new Usuario("Alexandre Villarino (Sysadmin)", "alexandrevillarino.silva@ucsal.edu.br", senhaPadraoHash, PapelFamiliar.SYSADMIN);
            sysadmin.setDocumentoSensivel("567.890.123-44");
            sysadmin = usuarioRepository.save(sysadmin);

            Usuario convidado = new Usuario("Marcos Vinicius Batista (Convidado)", "marcos.batista@ucsal.edu.br", senhaPadraoHash, PapelFamiliar.MEMBRO);
            convidado.setDocumentoSensivel("678.901.234-55");
            convidado = usuarioRepository.save(convidado);

            // 2. Grupo Familiar (RF-007)
            GrupoFamiliar grupo = new GrupoFamiliar("Família FinFamily UCSAL", titular);
            grupo = grupoFamiliarRepository.save(grupo);

            Membro mTitular = new Membro();
            mTitular.setGrupo(grupo);
            mTitular.setUsuario(titular);
            mTitular.setEmailConvidado(titular.getEmail());
            mTitular.setNomeConvidado(titular.getNome());
            mTitular.setPapel(PapelFamiliar.TITULAR);
            mTitular.setStatus(StatusMembro.ATIVO);
            membroRepository.save(mTitular);

            Membro mConjuge = new Membro();
            mConjuge.setGrupo(grupo);
            mConjuge.setUsuario(conjuge);
            mConjuge.setEmailConvidado(conjuge.getEmail());
            mConjuge.setNomeConvidado(conjuge.getNome());
            mConjuge.setPapel(PapelFamiliar.MEMBRO);
            mConjuge.setStatus(StatusMembro.ATIVO);
            membroRepository.save(mConjuge);

            Membro mFilho = new Membro();
            mFilho.setGrupo(grupo);
            mFilho.setUsuario(filho);
            mFilho.setEmailConvidado(filho.getEmail());
            mFilho.setNomeConvidado(filho.getNome());
            mFilho.setPapel(PapelFamiliar.MEMBRO);
            mFilho.setStatus(StatusMembro.ATIVO);
            membroRepository.save(mFilho);

            Membro mAuditor = new Membro();
            mAuditor.setGrupo(grupo);
            mAuditor.setUsuario(auditor);
            mAuditor.setEmailConvidado(auditor.getEmail());
            mAuditor.setNomeConvidado(auditor.getNome());
            mAuditor.setPapel(PapelFamiliar.AUDITOR);
            mAuditor.setStatus(StatusMembro.ATIVO);
            membroRepository.save(mAuditor);

            Membro mConvidado = new Membro();
            mConvidado.setGrupo(grupo);
            mConvidado.setUsuario(convidado);
            mConvidado.setEmailConvidado(convidado.getEmail());
            mConvidado.setNomeConvidado(convidado.getNome());
            mConvidado.setPapel(PapelFamiliar.MEMBRO);
            mConvidado.setStatus(StatusMembro.CONVIDADO);
            membroRepository.save(mConvidado);

            // 3. Contas e Cartoes nos 4 tipos (OE1: Corrente, Poupanca, Cartao, Corretora) + RN-001 (Privada)
            Conta ccItau = new Conta();
            ccItau.setGrupo(grupo);
            ccItau.setProprietario(titular);
            ccItau.setNome("Conta Corrente Principal");
            ccItau.setInstituicao("Itaú Uniclass");
            ccItau.setCodigoIdentificadorSensivel("AG 4512 / CC 99812-3");
            ccItau.setTipo(TipoConta.CORRENTE);
            ccItau.setSaldo(new BigDecimal("14850.00"));
            ccItau.setPrivada(false);
            ccItau = contaRepository.save(ccItau);

            Conta ccNubank = new Conta();
            ccNubank.setGrupo(grupo);
            ccNubank.setProprietario(conjuge);
            ccNubank.setNome("Conta Digital Conjunta");
            ccNubank.setInstituicao("Nubank");
            ccNubank.setCodigoIdentificadorSensivel("AG 0001 / CC 774512-9");
            ccNubank.setTipo(TipoConta.CORRENTE);
            ccNubank.setSaldo(new BigDecimal("6420.00"));
            ccNubank.setPrivada(false);
            ccNubank = contaRepository.save(ccNubank);

            Conta poupanca = new Conta();
            poupanca.setGrupo(grupo);
            poupanca.setProprietario(titular);
            poupanca.setNome("Reserva Familiar Poupança");
            poupanca.setInstituicao("Caixa Econômica");
            poupanca.setCodigoIdentificadorSensivel("AG 1024 / OP 013 / 44510-2");
            poupanca.setTipo(TipoConta.POUPANCA);
            poupanca.setSaldo(new BigDecimal("18500.00"));
            poupanca.setPrivada(false);
            poupanca = contaRepository.save(poupanca);

            Conta cartaoFamilia = new Conta();
            cartaoFamilia.setGrupo(grupo);
            cartaoFamilia.setProprietario(titular);
            cartaoFamilia.setNome("Cartão de crédito");
            cartaoFamilia.setInstituicao("Visa Infinite Itaú");
            cartaoFamilia.setCodigoIdentificadorSensivel("4532 **** **** 8891");
            cartaoFamilia.setTipo(TipoConta.CARTAO_CREDITO);
            cartaoFamilia.setSaldo(new BigDecimal("-1850.00"));
            cartaoFamilia.setLimiteCredito(new BigDecimal("15000.00"));
            cartaoFamilia.setDiaVencimento(LocalDate.now().plusDays(3).getDayOfMonth());
            cartaoFamilia.setPrivada(false);
            cartaoFamilia = contaRepository.save(cartaoFamilia);

            // Cartao do filho marcado como PRIVADO conforme Secao 2.4 e RN-001!
            Conta cartaoFilhoPrivado = new Conta();
            cartaoFilhoPrivado.setGrupo(grupo);
            cartaoFilhoPrivado.setProprietario(filho);
            cartaoFilhoPrivado.setNome("Cartão Universitário do Filho (Privado)");
            cartaoFilhoPrivado.setInstituicao("Inter Mastercard");
            cartaoFilhoPrivado.setCodigoIdentificadorSensivel("5412 **** **** 3319");
            cartaoFilhoPrivado.setTipo(TipoConta.CARTAO_CREDITO);
            cartaoFilhoPrivado.setSaldo(new BigDecimal("-680.00"));
            cartaoFilhoPrivado.setLimiteCredito(new BigDecimal("2500.00"));
            cartaoFilhoPrivado.setDiaVencimento(20);
            cartaoFilhoPrivado.setPrivada(true); // RN-001: Nao entra no patrimonio global!
            cartaoFilhoPrivado = contaRepository.save(cartaoFilhoPrivado);

            Conta corretora = new Conta();
            corretora.setGrupo(grupo);
            corretora.setProprietario(titular);
            corretora.setNome("Conta Corretora XP");
            corretora.setInstituicao("XP Investimentos");
            corretora.setCodigoIdentificadorSensivel("XP-COD-883920");
            corretora.setTipo(TipoConta.CORRETORA);
            corretora.setSaldo(new BigDecimal("3200.00"));
            corretora.setPrivada(false);
            corretora = contaRepository.save(corretora);

            // Conta com saldo zero para demonstrar RN-002 (exclusao permitida quando saldo == 0)
            Conta contaZerada = new Conta();
            contaZerada.setGrupo(grupo);
            contaZerada.setProprietario(titular);
            contaZerada.setNome("Conta Antiga Inativa (Saldo Zero)");
            contaZerada.setInstituicao("Banco do Brasil");
            contaZerada.setCodigoIdentificadorSensivel("AG 3310 / CC 10029-X");
            contaZerada.setTipo(TipoConta.CORRENTE);
            contaZerada.setSaldo(BigDecimal.ZERO);
            contaZerada.setPrivada(false);
            contaRepository.save(contaZerada);

            // 4. Arvore Hierarquica de Categorias e Subcategorias (RF-004)
            Categoria catAlimentacao = categoriaRepository.save(
                    new Categoria("Alimentação", "#F59E0B", "utensils", TipoTransacao.DESPESA, null, grupo));
            Categoria subSupermercado = categoriaRepository.save(
                    new Categoria("Supermercado", "#F59E0B", "shopping-cart", TipoTransacao.DESPESA, catAlimentacao, grupo));
            Categoria subPadaria = categoriaRepository.save(
                    new Categoria("Padaria e Café", "#F59E0B", "coffee", TipoTransacao.DESPESA, catAlimentacao, grupo));
            Categoria subRestaurantes = categoriaRepository.save(
                    new Categoria("Restaurantes e iFood", "#F59E0B", "utensils", TipoTransacao.DESPESA, catAlimentacao, grupo));

            Categoria catTransporte = categoriaRepository.save(
                    new Categoria("Transporte", "#3B82F6", "car", TipoTransacao.DESPESA, null, grupo));
            Categoria subUber = categoriaRepository.save(
                    new Categoria("Uber e Mobilidade", "#3B82F6", "navigation", TipoTransacao.DESPESA, catTransporte, grupo));
            Categoria subCombustivel = categoriaRepository.save(
                    new Categoria("Combustível e Manutenção", "#3B82F6", "fuel", TipoTransacao.DESPESA, catTransporte, grupo));

            Categoria catMoradia = categoriaRepository.save(
                    new Categoria("Moradia", "#8B5CF6", "home", TipoTransacao.DESPESA, null, grupo));
            Categoria subAluguel = categoriaRepository.save(
                    new Categoria("Aluguel e Condomínio", "#8B5CF6", "building", TipoTransacao.DESPESA, catMoradia, grupo));
            Categoria subContasCasa = categoriaRepository.save(
                    new Categoria("Energia, Água e Internet", "#8B5CF6", "zap", TipoTransacao.DESPESA, catMoradia, grupo));

            Categoria catEducacao = categoriaRepository.save(
                    new Categoria("Educação", "#06B6D4", "graduation-cap", TipoTransacao.DESPESA, null, grupo));
            Categoria subFaculdade = categoriaRepository.save(
                    new Categoria("Mensalidade Faculdade UCSAL", "#06B6D4", "book-open", TipoTransacao.DESPESA, catEducacao, grupo));

            Categoria catSaude = categoriaRepository.save(
                    new Categoria("Saúde", "#EC4899", "heart-pulse", TipoTransacao.DESPESA, null, grupo));
            Categoria subFarmacia = categoriaRepository.save(
                    new Categoria("Farmácia e Consultas", "#EC4899", "pill", TipoTransacao.DESPESA, catSaude, grupo));

            Categoria catLazer = categoriaRepository.save(
                    new Categoria("Lazer e Assinaturas", "#6366F1", "film", TipoTransacao.DESPESA, null, grupo));
            Categoria subStreaming = categoriaRepository.save(
                    new Categoria("Streaming (Netflix/Spotify)", "#6366F1", "tv", TipoTransacao.DESPESA, catLazer, grupo));

            Categoria catReceitas = categoriaRepository.save(
                    new Categoria("Receitas e Salários", "#10B981", "dollar-sign", TipoTransacao.RECEITA, null, grupo));
            Categoria subProventos = categoriaRepository.save(
                    new Categoria("Proventos e Dividendos", "#10B981", "trending-up", TipoTransacao.RECEITA, catReceitas, grupo));

            // 5. Tags (RF-004)
            Tag tagEssencial = tagRepository.save(new Tag("Essencial", "#EF4444", grupo));
            Tag tagFamilia = tagRepository.save(new Tag("Família", "#3B82F6", grupo));
            Tag tagRecorrente = tagRepository.save(new Tag("Fixo Mensal", "#8B5CF6", grupo));
            Tag tagFaculdade = tagRepository.save(new Tag("UCSAL", "#06B6D4", grupo));
            Tag tagInvestimento = tagRepository.save(new Tag("Renda Passiva", "#10B981", grupo));

            // 6. Orcamentos Mensais (RF-006 e Secao 2.4: R$ 1.800 Alimentacao, R$ 600 Transporte)
            String mesAtual = YearMonth.now().toString();

            Orcamento orcAlimentacao = new Orcamento();
            orcAlimentacao.setGrupo(grupo);
            orcAlimentacao.setCategoria(catAlimentacao);
            orcAlimentacao.setMes(mesAtual);
            orcAlimentacao.setValorPlanejado(new BigDecimal("1800.00"));
            orcamentoRepository.save(orcAlimentacao);

            Orcamento orcTransporte = new Orcamento();
            orcTransporte.setGrupo(grupo);
            orcTransporte.setCategoria(catTransporte);
            orcTransporte.setMes(mesAtual);
            orcTransporte.setValorPlanejado(new BigDecimal("600.00"));
            orcamentoRepository.save(orcTransporte);

            Orcamento orcMoradia = new Orcamento();
            orcMoradia.setGrupo(grupo);
            orcMoradia.setCategoria(catMoradia);
            orcMoradia.setMes(mesAtual);
            orcMoradia.setValorPlanejado(new BigDecimal("2500.00"));
            orcamentoRepository.save(orcMoradia);

            Orcamento orcEducacao = new Orcamento();
            orcEducacao.setGrupo(grupo);
            orcEducacao.setCategoria(catEducacao);
            orcEducacao.setMes(mesAtual);
            orcEducacao.setValorPlanejado(new BigDecimal("1200.00"));
            orcamentoRepository.save(orcEducacao);

            Orcamento orcSaude = new Orcamento();
            orcSaude.setGrupo(grupo);
            orcSaude.setCategoria(catSaude);
            orcSaude.setMes(mesAtual);
            orcSaude.setValorPlanejado(new BigDecimal("800.00"));
            orcamentoRepository.save(orcSaude);

            Orcamento orcLazer = new Orcamento();
            orcLazer.setGrupo(grupo);
            orcLazer.setCategoria(catLazer);
            orcLazer.setMes(mesAtual);
            orcLazer.setValorPlanejado(new BigDecimal("500.00"));
            orcamentoRepository.save(orcLazer);

            // 7. Lancamentos Financeiros Realistas do Mes Atual e Meses Anteriores
            LocalDate hoje = LocalDate.now();
            LocalDate inicioMes = YearMonth.now().atDay(1);

            // Receitas do mes
            criarTransacaoSeed(transacaoRepository, ccItau, catReceitas, titular,
                    "Salário Mensal Titular", new BigDecimal("9500.00"), inicioMes.plusDays(4), TipoTransacao.RECEITA, false, "MANUAL", List.of(tagFamilia));
            criarTransacaoSeed(transacaoRepository, ccNubank, catReceitas, conjuge,
                    "Salário Mensal Cônjuge", new BigDecimal("6800.00"), inicioMes.plusDays(4), TipoTransacao.RECEITA, false, "MANUAL", List.of(tagFamilia));

            // Despesas de Alimentacao (Somando R$ 1.525,90 = 84.77% de R$ 1.800,00 -> dispara Alerta 80% RN-004!)
            criarTransacaoSeed(transacaoRepository, ccItau, subSupermercado, titular,
                    "Compras do Mês Hiperideal Supermercado", new BigDecimal("980.00"), inicioMes.plusDays(2), TipoTransacao.DESPESA, false, "MANUAL", List.of(tagEssencial, tagFamilia));
            criarTransacaoSeed(transacaoRepository, cartaoFamilia, subSupermercado, conjuge,
                    "Feira Quinzenal Atacadão", new BigDecimal("360.00"), inicioMes.plusDays(8), TipoTransacao.DESPESA, false, "CSV", List.of(tagEssencial));
            criarTransacaoSeed(transacaoRepository, cartaoFamilia, subRestaurantes, conjuge,
                    "Almoço de Domingo Restaurante", new BigDecimal("140.00"), inicioMes.plusDays(9), TipoTransacao.DESPESA, false, "MANUAL", List.of(tagFamilia));
            criarTransacaoSeed(transacaoRepository, cartaoFamilia, subPadaria, filho,
                    "Padaria Pão Quente Café da Manhã", new BigDecimal("45.90"), hoje, TipoTransacao.DESPESA, false, "ASSISTENTE_IA", List.of(tagFamilia));

            // Despesas de Transporte (Somando R$ 612,50 = 102.08% de R$ 600,00 -> dispara Alerta 100% RN-004!)
            criarTransacaoSeed(transacaoRepository, cartaoFamilia, subCombustivel, titular,
                    "Abastecimento Gasolina Posto Shell", new BigDecimal("280.00"), inicioMes.plusDays(3), TipoTransacao.DESPESA, false, "MANUAL", List.of(tagEssencial));
            criarTransacaoSeed(transacaoRepository, cartaoFamilia, subCombustivel, conjuge,
                    "Abastecimento Gasolina Posto BR", new BigDecimal("210.00"), inicioMes.plusDays(10), TipoTransacao.DESPESA, false, "CSV", List.of(tagEssencial));
            criarTransacaoSeed(transacaoRepository, cartaoFamilia, subUber, filho,
                    "Uber Ida para a UCSAL Pituaçu", new BigDecimal("38.50"), inicioMes.plusDays(5), TipoTransacao.DESPESA, false, "ASSISTENTE_IA", List.of(tagFaculdade));
            criarTransacaoSeed(transacaoRepository, cartaoFamilia, subUber, titular,
                    "Uber Reunião Centro", new BigDecimal("44.00"), inicioMes.plusDays(7), TipoTransacao.DESPESA, false, "ASSISTENTE_IA", List.of(tagEssencial));
            criarTransacaoSeed(transacaoRepository, cartaoFamilia, subUber, conjuge,
                    "Uber Aeroporto Salvador", new BigDecimal("40.00"), hoje.minusDays(1), TipoTransacao.DESPESA, false, "ASSISTENTE_IA", List.of(tagFamilia));

            // Despesas de Moradia (Recorrentes)
            criarTransacaoSeed(transacaoRepository, ccItau, subAluguel, titular,
                    "Aluguel e Condomínio Apartamento Pituaçu", new BigDecimal("1680.00"), inicioMes.plusDays(4), TipoTransacao.DESPESA, true, "MANUAL", List.of(tagEssencial, tagRecorrente));
            criarTransacaoSeed(transacaoRepository, ccItau, subContasCasa, titular,
                    "Conta de Energia Coelba + Internet Fibra", new BigDecimal("369.90"), inicioMes.plusDays(6), TipoTransacao.DESPESA, true, "MANUAL", List.of(tagEssencial, tagRecorrente));

            // Despesa de Educacao
            criarTransacaoSeed(transacaoRepository, ccNubank, subFaculdade, filho,
                    "Mensalidade Bacharelado Engenharia de Software UCSAL", new BigDecimal("950.00"), inicioMes.plusDays(5), TipoTransacao.DESPESA, true, "MANUAL", List.of(tagFaculdade, tagRecorrente));

            // Despesas de Saude e Lazer
            criarTransacaoSeed(transacaoRepository, ccItau, subFarmacia, titular,
                    "Plano de Saúde Familiar + Farmácia", new BigDecimal("460.00"), inicioMes.plusDays(7), TipoTransacao.DESPESA, true, "MANUAL", List.of(tagEssencial));
            criarTransacaoSeed(transacaoRepository, cartaoFamilia, subStreaming, conjuge,
                    "Assinaturas Netflix 4K + Spotify Duo", new BigDecimal("83.80"), inicioMes.plusDays(3), TipoTransacao.DESPESA, true, "CSV", List.of(tagRecorrente));

            // Historico dos 5 meses anteriores para o Grafico de Evolucao Patrimonial e Fluxo de Caixa (RF-009)
            for (int m = 1; m <= 5; m++) {
                LocalDate dataMesAnt = inicioMes.minusMonths(m).plusDays(5);
                criarTransacaoSeed(transacaoRepository, ccItau, catReceitas, titular,
                        "Receitas Familiares Consolidadas (" + YearMonth.from(dataMesAnt) + ")",
                        new BigDecimal(String.valueOf(15200 + (5 - m) * 350)), dataMesAnt, TipoTransacao.RECEITA, false, "MANUAL", List.of(tagFamilia));
                criarTransacaoSeed(transacaoRepository, ccItau, catMoradia, titular,
                        "Despesas Familiares Consolidadas (" + YearMonth.from(dataMesAnt) + ")",
                        new BigDecimal(String.valueOf(9400 + (m % 3) * 420)), dataMesAnt.plusDays(5), TipoTransacao.DESPESA, false, "MANUAL", List.of(tagEssencial));
            }

            // 8. Ativos e Operacoes de Investimentos + Proventos (RF-008 e RN-005)
            Ativo petr4 = ativoRepository.save(new Ativo("PETR4", "Petrobras PN", new BigDecimal("38.45"), "ACAO", "Petróleo e Energia"));
            Ativo vale3 = ativoRepository.save(new Ativo("VALE3", "Vale ON", new BigDecimal("61.80"), "ACAO", "Mineração e Siderurgia"));
            Ativo itub4 = ativoRepository.save(new Ativo("ITUB4", "Itaú Unibanco PN", new BigDecimal("35.90"), "ACAO", "Bancos e Finanças"));
            Ativo wege3 = ativoRepository.save(new Ativo("WEGE3", "WEG S.A. ON", new BigDecimal("54.20"), "ACAO", "Bens Industriais"));
            Ativo hglg11 = ativoRepository.save(new Ativo("HGLG11", "CSHG Logística FII", new BigDecimal("162.50"), "FII", "Fundos Imobiliários Logística"));
            Ativo mxrf11 = ativoRepository.save(new Ativo("MXRF11", "Maxi Renda FII", new BigDecimal("10.15"), "FII", "Fundos Imobiliários Papel"));
            Ativo ivvb11 = ativoRepository.save(new Ativo("IVVB11", "iShares S&P 500 ETF", new BigDecimal("348.00"), "ETF", "Internacional EUA"));
            Ativo tesouro = ativoRepository.save(new Ativo("TESOURO_SELIC", "Tesouro Selic 2029", new BigDecimal("15420.00"), "RENDA_FIXA", "Títulos Públicos Federais"));

            // Compras na carteira
            financeiroService.registrarOperacaoInvestimento(
                    grupo.getId(), "PETR4", TipoOperacao.COMPRA, new BigDecimal("200"), new BigDecimal("34.50"),
                    hoje.minusMonths(3), corretora.getId(), ccItau.getId(), null);
            petr4.setPrecoAtual(new BigDecimal("38.45"));
            ativoRepository.save(petr4);

            financeiroService.registrarOperacaoInvestimento(
                    grupo.getId(), "ITUB4", TipoOperacao.COMPRA, new BigDecimal("300"), new BigDecimal("31.80"),
                    hoje.minusMonths(2), corretora.getId(), ccItau.getId(), null);
            itub4.setPrecoAtual(new BigDecimal("35.90"));
            ativoRepository.save(itub4);

            financeiroService.registrarOperacaoInvestimento(
                    grupo.getId(), "HGLG11", TipoOperacao.COMPRA, new BigDecimal("80"), new BigDecimal("156.00"),
                    hoje.minusMonths(2), corretora.getId(), ccItau.getId(), null);
            hglg11.setPrecoAtual(new BigDecimal("162.50"));
            ativoRepository.save(hglg11);

            financeiroService.registrarOperacaoInvestimento(
                    grupo.getId(), "TESOURO_SELIC", TipoOperacao.COMPRA, new BigDecimal("1"), new BigDecimal("14800.00"),
                    hoje.minusMonths(4), corretora.getId(), ccItau.getId(), null);
            tesouro.setPrecoAtual(new BigDecimal("15420.00"));
            ativoRepository.save(tesouro);

            // Proventos que geram automaticamente Receita na conta de destino (RN-005!)
            financeiroService.registrarOperacaoInvestimento(
                    grupo.getId(), "PETR4", TipoOperacao.PROVENTO, new BigDecimal("200"), new BigDecimal("1.45"),
                    inicioMes.plusDays(6), corretora.getId(), ccItau.getId(), null);

            financeiroService.registrarOperacaoInvestimento(
                    grupo.getId(), "HGLG11", TipoOperacao.PROVENTO, new BigDecimal("80"), new BigDecimal("1.10"),
                    inicioMes.plusDays(10), corretora.getId(), corretora.getId(), null);

            // 9. Recalcula orcamentos para disparar os alertas de 80% (Alimentacao) e 100% (Transporte) e vencimentos!
            financeiroService.recalcularOrcamentosDoMes(grupo.getId(), mesAtual);
            financeiroService.verificarVencimentosCartoesAgendado();

            auditLogService.registrar("SISTEMA_INICIALIZADO", "SEEDER", "sysadmin@finfamily.com", "INFO",
                    "Dados da Jornada Tipica de Uso (Secao 2.4) carregados com criptografia AES-256 e BCrypt.");
        };
    }

    private void criarTransacaoSeed(TransacaoRepository repo,
                                    Conta conta,
                                    Categoria categoria,
                                    Usuario autor,
                                    String descricao,
                                    BigDecimal valor,
                                    LocalDate data,
                                    TipoTransacao tipo,
                                    boolean recorrente,
                                    String origem,
                                    List<Tag> tags) {
        Transacao t = new Transacao();
        t.setConta(conta);
        t.setCategoria(categoria);
        t.setAutor(autor);
        t.setDescricao(descricao);
        t.setValor(valor);
        t.setData(data);
        t.setTipo(tipo);
        t.setParcela(1);
        t.setTotalParcelas(1);
        t.setRecorrente(recorrente);
        t.setFrequenciaRecorrencia(recorrente ? "MENSAL" : null);
        t.setOrigemLancamento(origem);
        t.setHashConciliacao(Conta.gerarHashConciliacao(conta.getId(), data, valor, descricao));
        if (tags != null) {
            t.getTags().addAll(tags);
        }
        repo.save(t);
    }
}
