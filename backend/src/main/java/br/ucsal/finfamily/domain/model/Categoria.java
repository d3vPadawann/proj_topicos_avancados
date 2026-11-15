package br.ucsal.finfamily.domain.model;

import br.ucsal.finfamily.domain.enums.TipoTransacao;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade Categoria conforme Diagrama de Classes (Figura 5) e RF-004.
 * Suporta arvore hierarquica de categorias (ex: Alimentacao > Supermercado).
 */
@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String cor = "#3B82F6";

    private String icone = "tag";

    @Enumerated(EnumType.STRING)
    private TipoTransacao tipoPadrao = TipoTransacao.DESPESA;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_pai_id")
    private Categoria pai;

    @JsonIgnore
    @OneToMany(mappedBy = "pai", cascade = CascadeType.ALL)
    private List<Categoria> filhas = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grupo_id")
    private GrupoFamiliar grupo;

    public Categoria() {
    }

    public Categoria(String nome, String cor, String icone, TipoTransacao tipoPadrao, Categoria pai, GrupoFamiliar grupo) {
        this.nome = nome;
        this.cor = cor;
        this.icone = icone;
        this.tipoPadrao = tipoPadrao;
        this.pai = pai;
        this.grupo = grupo;
    }

    /**
     * Metodo do Diagrama de Classes (Figura 5): subcategorias(): List<Categoria>
     */
    public List<Categoria> subcategorias() {
        return this.filhas;
    }

    public String getCaminhoCompleto() {
        if (pai != null) {
            return pai.getNome() + " > " + this.nome;
        }
        return this.nome;
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

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public String getIcone() {
        return icone;
    }

    public void setIcone(String icone) {
        this.icone = icone;
    }

    public TipoTransacao getTipoPadrao() {
        return tipoPadrao;
    }

    public void setTipoPadrao(TipoTransacao tipoPadrao) {
        this.tipoPadrao = tipoPadrao;
    }

    public Categoria getPai() {
        return pai;
    }

    public void setPai(Categoria pai) {
        this.pai = pai;
    }

    public List<Categoria> getFilhas() {
        return filhas;
    }

    public void setFilhas(List<Categoria> filhas) {
        this.filhas = filhas;
    }

    public GrupoFamiliar getGrupo() {
        return grupo;
    }

    public void setGrupo(GrupoFamiliar grupo) {
        this.grupo = grupo;
    }
}
