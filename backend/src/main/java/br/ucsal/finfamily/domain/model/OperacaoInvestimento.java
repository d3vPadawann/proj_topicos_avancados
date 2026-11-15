package br.ucsal.finfamily.domain.model;

import br.ucsal.finfamily.domain.enums.TipoOperacao;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Entidade OperacaoInvestimento conforme Diagrama de Classes (Figura 5), RF-008 e RN-005.
 */
@Entity
@Table(name = "operacoes_investimento")
public class OperacaoInvestimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoOperacao tipo;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantidade = BigDecimal.ONE;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal preco = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate data = LocalDate.now();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ativo_ticker", nullable = false)
    private Ativo ativo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conta_corretora_id", nullable = false)
    private Conta contaCorretora;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conta_destino_provento_id")
    private Conta contaDestinoProvento;

    /**
     * Referencia para a Transacao de Receita gerada automaticamente quando tipo == PROVENTO (RN-005).
     */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transacao_provento_id")
    private Transacao transacaoGerada;

    public OperacaoInvestimento() {
    }

    /**
     * Metodo do Diagrama de Classes (Figura 5): valorTotal(): Decimal
     */
    public BigDecimal valorTotal() {
        if (this.quantidade == null || this.preco == null) {
            return BigDecimal.ZERO;
        }
        return this.quantidade.multiply(this.preco).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoOperacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoOperacao tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public Ativo getAtivo() {
        return ativo;
    }

    public void setAtivo(Ativo ativo) {
        this.ativo = ativo;
    }

    public Conta getContaCorretora() {
        return contaCorretora;
    }

    public void setContaCorretora(Conta contaCorretora) {
        this.contaCorretora = contaCorretora;
    }

    public Conta getContaDestinoProvento() {
        return contaDestinoProvento;
    }

    public void setContaDestinoProvento(Conta contaDestinoProvento) {
        this.contaDestinoProvento = contaDestinoProvento;
    }

    public Transacao getTransacaoGerada() {
        return transacaoGerada;
    }

    public void setTransacaoGerada(Transacao transacaoGerada) {
        this.transacaoGerada = transacaoGerada;
    }
}
