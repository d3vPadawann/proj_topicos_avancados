package br.ucsal.finfamily.domain.model;

import br.ucsal.finfamily.domain.enums.TipoConta;
import br.ucsal.finfamily.security.AesAttributeConverter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/**
 * Entidade Conta conforme Diagrama de Classes (Figura 5), RF-002, RN-001, RN-002 e RF-005.
 * Suporta CORRENTE, POUPANCA, CARTAO_CREDITO e CORRETORA sem hierarquia de heranca.
 */
@Entity
@Table(name = "contas")
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String instituicao;

    /**
     * Numero da conta/cartao criptografado em repouso com AES-256 (RNF-003).
     */
    @Convert(converter = AesAttributeConverter.class)
    private String codigoIdentificadorSensivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoConta tipo = TipoConta.CORRENTE;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal limiteCredito = BigDecimal.ZERO;

    /**
     * RN-001: Privacidade de Contas. Contas com a flag "Privada" nao contabilizam
     * para o Patrimonio Liquido global visivel aos demais membros.
     */
    @Column(nullable = false)
    private Boolean privada = false;

    private Integer diaVencimento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "proprietario_id")
    private Usuario proprietario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grupo_id", nullable = false)
    private GrupoFamiliar grupo;

    public Conta() {
    }

    /**
     * Metodo do Diagrama de Classes (Figura 5): podeSerExcluida(): Boolean
     * Implementa RN-002: Uma conta nao pode ser excluida se possuir saldo diferente de zero.
     */
    public Boolean podeSerExcluida() {
        return this.saldo == null || this.saldo.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Metodo do Diagrama de Classes (Figura 5): importarCSV(arquivo): int
     * Calcula quantas transacoes de uma lista de importacao sao ineditas (sem duplicidade - OE2).
     */
    public int importarCSV(List<Transacao> candidatas, Set<String> hashesExistentes) {
        int importadas = 0;
        for (Transacao t : candidatas) {
            String hash = gerarHashConciliacao(this.id, t.getData(), t.getValor(), t.getDescricao());
            t.setHashConciliacao(hash);
            if (!hashesExistentes.contains(hash)) {
                hashesExistentes.add(hash);
                importadas++;
            }
        }
        return importadas;
    }

    public static String gerarHashConciliacao(Long contaId, LocalDate data, BigDecimal valor, String descricao) {
        try {
            String descNorm = descricao == null ? "" : descricao.trim().toLowerCase().replaceAll("\\s+", " ");
            String valorNorm = valor == null ? "0.00" : valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            String base = (contaId != null ? contaId : 0L) + "|" + data + "|" + valorNorm + "|" + descNorm;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(base.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao calcular hash de conciliacao CSV", e);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getInstituicao() {
        return instituicao;
    }

    public void setInstituicao(String instituicao) {
        this.instituicao = instituicao;
    }

    public String getCodigoIdentificadorSensivel() {
        return codigoIdentificadorSensivel;
    }

    public void setCodigoIdentificadorSensivel(String codigoIdentificadorSensivel) {
        this.codigoIdentificadorSensivel = codigoIdentificadorSensivel;
    }

    public TipoConta getTipo() {
        return tipo;
    }

    public void setTipo(TipoConta tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public BigDecimal getLimiteCredito() {
        return limiteCredito;
    }

    public void setLimiteCredito(BigDecimal limiteCredito) {
        this.limiteCredito = limiteCredito;
    }

    public Boolean getPrivada() {
        return privada;
    }

    public void setPrivada(Boolean privada) {
        this.privada = privada;
    }

    public Integer getDiaVencimento() {
        return diaVencimento;
    }

    public void setDiaVencimento(Integer diaVencimento) {
        this.diaVencimento = diaVencimento;
    }

    public Usuario getProprietario() {
        return proprietario;
    }

    public void setProprietario(Usuario proprietario) {
        this.proprietario = proprietario;
    }

    public GrupoFamiliar getGrupo() {
        return grupo;
    }

    public void setGrupo(GrupoFamiliar grupo) {
        this.grupo = grupo;
    }
}
