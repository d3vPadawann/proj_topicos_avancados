package br.ucsal.finfamily.domain.model;

import br.ucsal.finfamily.domain.enums.PapelFamiliar;
import br.ucsal.finfamily.security.AesAttributeConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade Usuario conforme Diagrama de Classes (Figura 5) e RF-001.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String senhaHash;

    /**
     * Dado sensivel criptografado com AES-256-GCM em repouso (RNF-003).
     */
    @Convert(converter = AesAttributeConverter.class)
    private String documentoSensivel;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PapelFamiliar papelPrincipal = PapelFamiliar.TITULAR;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public Usuario() {
    }

    public Usuario(String nome, String email, String senhaHash, PapelFamiliar papelPrincipal) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.papelPrincipal = papelPrincipal;
        this.ativo = true;
        this.criadoEm = LocalDateTime.now();
    }

    /**
     * Metodo do Diagrama de Classes (Figura 5): autenticar(email, senha): Boolean
     */
    public Boolean autenticar(String emailInformado, boolean senhaCorreta) {
        return Boolean.TRUE.equals(this.ativo)
                && this.email != null
                && this.email.equalsIgnoreCase(emailInformado)
                && senhaCorreta;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getDocumentoSensivel() {
        return documentoSensivel;
    }

    public void setDocumentoSensivel(String documentoSensivel) {
        this.documentoSensivel = documentoSensivel;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public PapelFamiliar getPapelPrincipal() {
        return papelPrincipal;
    }

    public void setPapelPrincipal(PapelFamiliar papelPrincipal) {
        this.papelPrincipal = papelPrincipal;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
