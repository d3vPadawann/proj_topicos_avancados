// geracao e validacao do token JWT
package br.ucsal.finfamily.security;

import br.ucsal.finfamily.domain.enums.PapelFamiliar;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {
    @Value("${finfamily.security.jwt-secret:FinFamilyUCSAL2026TopicosAvancadosChaveSecretaSuperSegura256Bits!}")
    private String jwtSecret;

    @Value("${finfamily.security.jwt-expiration-ms:86400000}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String gerarToken(Long usuarioId, String email, String nome, PapelFamiliar papel, Long grupoId) {
        return gerarToken(usuarioId, email, nome, papel, grupoId, "ATIVO");
    }

    public String gerarToken(Long usuarioId, String email, String nome, PapelFamiliar papel, Long grupoId, String statusMembro) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("usuarioId", usuarioId)
                .claim("nome", nome)
                .claim("papel", papel != null ? papel.name() : PapelFamiliar.MEMBRO.name())
                .claim("grupoId", grupoId != null ? grupoId : 1L)
                .claim("status", statusMembro != null ? statusMembro : "ATIVO")
                .claim("tokenCriptografado", AesAttributeConverter.encrypt(email + ":" + usuarioId))
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims obterClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validarToken(String token) {
        try {
            obterClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
