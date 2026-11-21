// filtro que valida o token JWT
package br.ucsal.finfamily.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtTokenProvider.validarToken(token)) {
                Claims claims = jwtTokenProvider.obterClaims(token);
                String email = claims.getSubject();
                String papel = claims.get("papel", String.class);
                Number usuarioIdNum = claims.get("usuarioId", Number.class);
                Number grupoIdNum = claims.get("grupoId", Number.class);

                Long usuarioId = usuarioIdNum != null ? usuarioIdNum.longValue() : 1L;
                Long grupoId = grupoIdNum != null ? grupoIdNum.longValue() : 1L;

                String status = claims.get("status", String.class);
                String papelEfetivo = papel != null ? papel : "MEMBRO";

                Map<String, Object> principalDetails = Map.of(
                        "email", email,
                        "usuarioId", usuarioId,
                        "grupoId", grupoId,
                        "papel", papelEfetivo
                );

                String autoridade = "CONVIDADO".equals(status) ? "ROLE_CONVIDADO" : "ROLE_" + papelEfetivo;

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principalDetails,
                        null,
                        List.of(new SimpleGrantedAuthority(autoridade))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
