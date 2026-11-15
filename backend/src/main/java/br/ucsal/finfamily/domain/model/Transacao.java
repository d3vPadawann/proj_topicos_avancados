package br.ucsal.finfamily.domain.model;

import br.ucsal.finfamily.domain.enums.TipoTransacao;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Entidade Transacao conforme Diagrama de Classes (Figura 5), RF-003 e RN-003.
 */
@Entity
@Table(name = "transacoes", indexes = {
        @Index(name = "idx_transacao_hash", columnList = "hashConciliacao"),
        @Index(name = "idx_transacao_data", columnList = "data")
})
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTransacao tipo = TipoTransacao.DESPESA;

    private Integer parcela = 1;

    private Integer totalParcelas = 1;

    private String grupoParcelamentoId;

    @Column(nullable = false)
    private Boolean recorrente = false;

    private String frequenciaRecorrencia; // MENSAL, ANUAL, SEMANAL

    @Column(length = 128)
    private String hashConciliacao;

    private String origemLancamento = "MANUAL"; // MANUAL, CSV, ASSISTENTE_IA, PROVENTO

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conta_destino_id")
    private Conta contaDestino;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "transacao_tags",
            joinColumns = @JoinColumn(name = "transacao_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    public Transacao() {
    }

    /**
     * Metodo do Diagrama de Classes (Figura 5): gerarParcelas(): List<Transacao>
     * Implementa RN-003: O sistema deve dividir automaticamente o valor total de uma
     * despesa parcelada, projetando os lancamentos futuros.
     */
    public List<Transacao> gerarParcelas() {
        List<Transacao> parcelasGeradas = new ArrayList<>();
        int numParcelas = (this.totalParcelas == null || this.totalParcelas < 1) ? 1 : this.totalParcelas;

        if (numParcelas == 1) {
            this.parcela = 1;
            this.totalParcelas = 1;
            parcelasGeradas.add(this);
            return parcelasGeradas;
        }

        String batchId = UUID.randomUUID().toString().substring(0, 8);
        BigDecimal valorTotal = this.valor;
        BigDecimal valorBase = valorTotal.divide(BigDecimal.valueOf(numParcelas), 2, RoundingMode.DOWN);
        BigDecimal somaBase = valorBase.multiply(BigDecimal.valueOf(numParcelas));
        BigDecimal diferencaCentavos = valorTotal.subtract(somaBase);

        for (int i = 1; i <= numParcelas; i++) {
            Transacao p = new Transacao();
            BigDecimal valorParcela = (i == 1) ? valorBase.add(diferencaCentavos) : valorBase;
            p.setDescricao(this.descricao + " (" + i + "/" + numParcelas + ")");
            p.setValor(valorParcela);
            p.setData(this.data.plusMonths(i - 1));
            p.setTipo(this.tipo);
            p.setParcela(i);
            p.setTotalParcelas(numParcelas);
            p.setGrupoParcelamentoId(batchId);
            p.setRecorrente(false);
            p.setOrigemLancamento(this.origemLancamento);
            p.setConta(this.conta);
            p.setContaDestino(this.contaDestino);
            p.setCategoria(this.categoria);
            p.setTags(new HashSet<>(this.tags));
            p.setAutor(this.autor);
            parcelasGeradas.add(p);
        }
        return parcelasGeradas;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoTransacao tipo) {
        this.tipo = tipo;
    }

    public Integer getParcela() {
        return parcela;
    }

    public void setParcela(Integer parcela) {
        this.parcela = parcela;
    }

    public Integer getTotalParcelas() {
        return totalParcelas;
    }

    public void setTotalParcelas(Integer totalParcelas) {
        this.totalParcelas = totalParcelas;
    }

    public String getGrupoParcelamentoId() {
        return grupoParcelamentoId;
    }

    public void setGrupoParcelamentoId(String grupoParcelamentoId) {
        this.grupoParcelamentoId = grupoParcelamentoId;
    }

    public Boolean getRecorrente() {
        return recorrente;
    }

    public void setRecorrente(Boolean recorrente) {
        this.recorrente = recorrente;
    }

    public String getFrequenciaRecorrencia() {
        return frequenciaRecorrencia;
    }

    public void setFrequenciaRecorrencia(String frequenciaRecorrencia) {
        this.frequenciaRecorrencia = frequenciaRecorrencia;
    }

    public String getHashConciliacao() {
        return hashConciliacao;
    }

    public void setHashConciliacao(String hashConciliacao) {
        this.hashConciliacao = hashConciliacao;
    }

    public String getOrigemLancamento() {
        return origemLancamento;
    }

    public void setOrigemLancamento(String origemLancamento) {
        this.origemLancamento = origemLancamento;
    }

    public Conta getConta() {
        return conta;
    }

    public void setConta(Conta conta) {
        this.conta = conta;
    }

    public Conta getContaDestino() {
        return contaDestino;
    }

    public void setContaDestino(Conta contaDestino) {
        this.contaDestino = contaDestino;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public Usuario getAutor() {
        return autor;
    }

    public void setAutor(Usuario autor) {
        this.autor = autor;
    }
}
