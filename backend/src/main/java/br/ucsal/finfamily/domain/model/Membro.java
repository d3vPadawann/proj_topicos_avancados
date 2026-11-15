package br.ucsal.finfamily.domain.model;

import br.ucsal.finfamily.domain.enums.PapelFamiliar;
import br.ucsal.finfamily.domain.enums.StatusMembro;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade Membro conforme Diagrama de Classes (Figura 5), RF-007 e BPMN Figura 2.
 * Guarda o papel (TITULAR, MEMBRO, AUDITOR) e o status (CONVIDADO, ATIVO, SUSPENSO, RECUSADO).
 */
@Entity
@Table(name = "membros_familiares")
public class Membro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PapelFamiliar papel = PapelFamiliar.MEMBRO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusMembro status = StatusMembro.CONVIDADO;

    @Column(nullable = false)
    private String emailConvidado;

    @Column(nullable = false)
    private String nomeConvidado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grupo_id", nullable = false)
    private GrupoFamiliar grupo;

    @Column(nullable = false)
    private LocalDateTime dataConvite = LocalDateTime.now();

    public Membro() {
    }

    /**
     * Metodo do Diagrama de Classes (Figura 5): aceitarConvite(): void
     */
    public void aceitarConvite() {
        this.status = StatusMembro.ATIVO;
    }

    /**
     * Fluxo alternativo do BPMN Figura 2: recusar convite.
     */
    public void recusarConvite() {
        this.status = StatusMembro.RECUSADO;
    }

    /**
     * Metodo do Diagrama de Classes (Figura 5): suspender(): void
     */
    public void suspender() {
        this.status = StatusMembro.SUSPENSO;
    }

    public void reativar() {
        this.status = StatusMembro.ATIVO;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PapelFamiliar getPapel() {
        return papel;
    }

    public void setPapel(PapelFamiliar papel) {
        this.papel = papel;
    }

    public StatusMembro getStatus() {
        return status;
    }

    public void setStatus(StatusMembro status) {
        this.status = status;
    }

    public String getEmailConvidado() {
        return emailConvidado;
    }

    public void setEmailConvidado(String emailConvidado) {
        this.emailConvidado = emailConvidado;
    }

    public String getNomeConvidado() {
        return nomeConvidado;
    }

    public void setNomeConvidado(String nomeConvidado) {
        this.nomeConvidado = nomeConvidado;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public GrupoFamiliar getGrupo() {
        return grupo;
    }

    public void setGrupo(GrupoFamiliar grupo) {
        this.grupo = grupo;
    }

    public LocalDateTime getDataConvite() {
        return dataConvite;
    }

    public void setDataConvite(LocalDateTime dataConvite) {
        this.dataConvite = dataConvite;
    }
}
