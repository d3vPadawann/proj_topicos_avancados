package br.ucsal.finfamily.domain.model;

import br.ucsal.finfamily.domain.enums.TipoNotificacao;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade Orcamento conforme Diagrama de Classes (Figura 5), RF-006 e RN-004.
 */
@Entity
@Table(name = "orcamentos")
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Representa YearMonth no formato YYYY-MM (ex: "2026-11").
     */
    @Column(nullable = false, length = 7)
    private String mes;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorPlanejado = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorGasto = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean alerta80Disparado = false;

    @Column(nullable = false)
    private Boolean alerta100Disparado = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grupo_id", nullable = false)
    private GrupoFamiliar grupo;

    public Orcamento() {
    }

    /**
     * Metodo do Diagrama de Classes (Figura 5): percentualGasto(): Decimal
     */
    public BigDecimal percentualGasto() {
        if (this.valorPlanejado == null || this.valorPlanejado.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal gasto = this.valorGasto != null ? this.valorGasto : BigDecimal.ZERO;
        return gasto.multiply(BigDecimal.valueOf(100))
                .divide(this.valorPlanejado, 2, RoundingMode.HALF_UP);
    }

    /**
     * Metodo do Diagrama de Classes (Figura 5): verificarAlerta(): List<Notificacao>
     * Implementa RN-004: O sistema deve disparar notificacoes quando uma categoria
     * atingir 80% e 100% do valor planejado.
     */
    public List<Notificacao> verificarAlerta() {
        List<Notificacao> notificacoes = new ArrayList<>();
        BigDecimal percentual = percentualGasto();
        String nomeCat = categoria != null ? categoria.getNome() : "Categoria";

        if (percentual.compareTo(BigDecimal.valueOf(80)) >= 0 && !Boolean.TRUE.equals(this.alerta80Disparado)) {
            this.alerta80Disparado = true;
            Notificacao n80 = new Notificacao();
            n80.setTipo(TipoNotificacao.ORCAMENTO_80);
            n80.setTitulo("Alerta de Orçamento (80%): " + nomeCat);
            n80.setMensagem(String.format(
                    "Atenção! Os gastos com '%s' no mês %s atingiram %.1f%% do orçamento planejado (R$ %.2f de R$ %.2f).",
                    nomeCat, this.mes, percentual, this.valorGasto, this.valorPlanejado));
            n80.setGrupo(this.grupo);
            notificacoes.add(n80);
        }

        if (percentual.compareTo(BigDecimal.valueOf(100)) >= 0 && !Boolean.TRUE.equals(this.alerta100Disparado)) {
            this.alerta100Disparado = true;
            Notificacao n100 = new Notificacao();
            n100.setTipo(TipoNotificacao.ORCAMENTO_100);
            n100.setTitulo("Orçamento Estourado (100%): " + nomeCat);
            n100.setMensagem(String.format(
                    "Limite atingido! Os gastos com '%s' no mês %s chegaram a %.1f%% do orçamento planejado (R$ %.2f de R$ %.2f).",
                    nomeCat, this.mes, percentual, this.valorGasto, this.valorPlanejado));
            n100.setGrupo(this.grupo);
            notificacoes.add(n100);
        }

        return notificacoes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMes() {
        return mes;
    }

    public void setMes(String mes) {
        this.mes = mes;
    }

    public BigDecimal getValorPlanejado() {
        return valorPlanejado;
    }

    public void setValorPlanejado(BigDecimal valorPlanejado) {
        this.valorPlanejado = valorPlanejado;
    }

    public BigDecimal getValorGasto() {
        return valorGasto;
    }

    public void setValorGasto(BigDecimal valorGasto) {
        this.valorGasto = valorGasto;
    }

    public Boolean getAlerta80Disparado() {
        return alerta80Disparado;
    }

    public void setAlerta80Disparado(Boolean alerta80Disparado) {
        this.alerta80Disparado = alerta80Disparado;
    }

    public Boolean getAlerta100Disparado() {
        return alerta100Disparado;
    }

    public void setAlerta100Disparado(Boolean alerta100Disparado) {
        this.alerta100Disparado = alerta100Disparado;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public GrupoFamiliar getGrupo() {
        return grupo;
    }

    public void setGrupo(GrupoFamiliar grupo) {
        this.grupo = grupo;
    }
}
