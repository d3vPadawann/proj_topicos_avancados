package br.ucsal.finfamily.domain.model;

import br.ucsal.finfamily.domain.enums.PapelFamiliar;
import br.ucsal.finfamily.domain.enums.StatusMembro;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade GrupoFamiliar conforme Diagrama de Classes (Figura 5), RF-007 e RN-001.
 */
@Entity
@Table(name = "grupos_familiares")
public class GrupoFamiliar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "titular_id")
    private Usuario titular;

    @JsonIgnore
    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Membro> membros = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Conta> contas = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Orcamento> orcamentos = new ArrayList<>();

    public GrupoFamiliar() {
    }

    public GrupoFamiliar(String nome, Usuario titular) {
        this.nome = nome;
        this.titular = titular;
    }

    /**
     * Metodo do Diagrama de Classes (Figura 5): convidar(email, papel): Membro (RF-007).
     */
    public Membro convidar(String email, String nomeConvidado, PapelFamiliar papel, Usuario usuarioAssociado) {
        Membro novoMembro = new Membro();
        novoMembro.setGrupo(this);
        novoMembro.setEmailConvidado(email);
        novoMembro.setNomeConvidado(nomeConvidado != null && !nomeConvidado.isBlank() ? nomeConvidado : email);
        novoMembro.setPapel(papel != null ? papel : PapelFamiliar.MEMBRO);
        novoMembro.setStatus(StatusMembro.CONVIDADO);
        novoMembro.setUsuario(usuarioAssociado);
        this.membros.add(novoMembro);
        return novoMembro;
    }

    /**
     * Metodo do Diagrama de Classes (Figura 5): patrimonioLiquido(): Decimal
     * Implementa RN-001: Contas com a flag "Privada" nao contabilizam para o Patrimonio Liquido global
     * visivel aos demais membros.
     */
    public BigDecimal patrimonioLiquido() {
        return contas.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getPrivada()))
                .map(c -> c.getSaldo() != null ? c.getSaldo() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    public Usuario getTitular() {
        return titular;
    }

    public void setTitular(Usuario titular) {
        this.titular = titular;
    }

    public List<Membro> getMembros() {
        return membros;
    }

    public void setMembros(List<Membro> membros) {
        this.membros = membros;
    }

    public List<Conta> getContas() {
        return contas;
    }

    public void setContas(List<Conta> contas) {
        this.contas = contas;
    }

    public List<Orcamento> getOrcamentos() {
        return orcamentos;
    }

    public void setOrcamentos(List<Orcamento> orcamentos) {
        this.orcamentos = orcamentos;
    }
}
