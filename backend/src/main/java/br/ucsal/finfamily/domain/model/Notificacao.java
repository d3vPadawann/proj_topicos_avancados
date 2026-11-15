package br.ucsal.finfamily.domain.model;

import br.ucsal.finfamily.domain.enums.TipoNotificacao;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade Notificacao conforme Diagrama de Classes (Figura 5), RF-011 e RN-004.
 */
@Entity
@Table(name = "notificacoes")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    @Column(nullable = false, length = 600)
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacao tipo = TipoNotificacao.SISTEMA;

    @Column(nullable = false)
    private Boolean lida = false;

    @Column(nullable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grupo_id")
    private GrupoFamiliar grupo;

    public Notificacao() {
    }

    /**
     * Metodo do Diagrama de Classes (Figura 5): marcarComoLida(): void
     */
    public void marcarComoLida() {
        this.lida = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public TipoNotificacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoNotificacao tipo) {
        this.tipo = tipo;
    }

    public Boolean getLida() {
        return lida;
    }

    public void setLida(Boolean lida) {
        this.lida = lida;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(LocalDateTime criadaEm) {
        this.criadaEm = criadaEm;
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
}
