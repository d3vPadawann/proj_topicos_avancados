package br.ucsal.finfamily.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entidade Ativo conforme Diagrama de Classes (Figura 5) e RF-008.
 */
@Entity
@Table(name = "ativos")
public class Ativo {

    @Id
    @Column(length = 20, nullable = false, unique = true)
    private String ticker;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal precoAtual = BigDecimal.ZERO;

    private String tipoAtivo = "ACAO"; // ACAO, FII, ETF, RENDA_FIXA

    private String setor;

    public Ativo() {
    }

    public Ativo(String ticker, String nome, BigDecimal precoAtual, String tipoAtivo, String setor) {
        this.ticker = ticker != null ? ticker.toUpperCase().trim() : null;
        this.nome = nome;
        this.precoAtual = precoAtual;
        this.tipoAtivo = tipoAtivo;
        this.setor = setor;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker != null ? ticker.toUpperCase().trim() : null;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public BigDecimal getPrecoAtual() {
        return precoAtual;
    }

    public void setPrecoAtual(BigDecimal precoAtual) {
        this.precoAtual = precoAtual;
    }

    public String getTipoAtivo() {
        return tipoAtivo;
    }

    public void setTipoAtivo(String tipoAtivo) {
        this.tipoAtivo = tipoAtivo;
    }

    public String getSetor() {
        return setor;
    }

    public void setSetor(String setor) {
        this.setor = setor;
    }
}
