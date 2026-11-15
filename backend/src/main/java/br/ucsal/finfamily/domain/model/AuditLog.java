package br.ucsal.finfamily.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade AuditLog para o Caso de Uso UC13 (Consultar logs do sistema pelo Sysadmin) e RNF-003.
 * Nao armazena dados financeiros descriptografados, apenas metadados tecnicos e de seguranca.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    @Column(nullable = false)
    private String acao;

    @Column(nullable = false)
    private String modulo;

    private String usuarioMascarado;

    private String ipOrigem = "127.0.0.1";

    @Column(nullable = false)
    private String nivel = "INFO"; // INFO, WARN, SECURITY

    @Column(length = 500)
    private String detalhesTecnicos;

    public AuditLog() {
    }

    public AuditLog(String acao, String modulo, String emailUsuario, String nivel, String detalhesTecnicos) {
        this.dataHora = LocalDateTime.now();
        this.acao = acao;
        this.modulo = modulo;
        this.usuarioMascarado = mascararEmail(emailUsuario);
        this.nivel = nivel;
        this.detalhesTecnicos = detalhesTecnicos;
    }

    private static String mascararEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "sistema@finfamily.local";
        }
        String[] partes = email.split("@");
        String prefix = partes[0];
        String maskedPrefix = prefix.length() <= 2 ? prefix + "***" : prefix.substring(0, 2) + "***";
        return maskedPrefix + "@" + partes[1];
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getAcao() {
        return acao;
    }

    public void setAcao(String acao) {
        this.acao = acao;
    }

    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

    public String getUsuarioMascarado() {
        return usuarioMascarado;
    }

    public void setUsuarioMascarado(String usuarioMascarado) {
        this.usuarioMascarado = usuarioMascarado;
    }

    public String getIpOrigem() {
        return ipOrigem;
    }

    public void setIpOrigem(String ipOrigem) {
        this.ipOrigem = ipOrigem;
    }

    public String getNivel() {
        return nivel;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public String getDetalhesTecnicos() {
        return detalhesTecnicos;
    }

    public void setDetalhesTecnicos(String detalhesTecnicos) {
        this.detalhesTecnicos = detalhesTecnicos;
    }
}
