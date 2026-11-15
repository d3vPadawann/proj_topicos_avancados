package br.ucsal.finfamily.domain.model;

import jakarta.persistence.*;

/**
 * Entidade Tag conforme Diagrama de Classes (Figura 5) e RF-004.
 */
@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String cor = "#6366F1";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grupo_id")
    private GrupoFamiliar grupo;

    public Tag() {
    }

    public Tag(String nome, String cor, GrupoFamiliar grupo) {
        this.nome = nome;
        this.cor = cor;
        this.grupo = grupo;
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

    public GrupoFamiliar getGrupo() {
        return grupo;
    }

    public void setGrupo(GrupoFamiliar grupo) {
        this.grupo = grupo;
    }
}
