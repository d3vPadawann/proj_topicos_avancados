// regras de acesso por perfil
package br.ucsal.finfamily.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final String TITULAR = "TITULAR";
    private static final String MEMBRO = "MEMBRO";
    private static final String AUDITOR = "AUDITOR";
    private static final String SYSADMIN = "SYSADMIN";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/health", "/error").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        .requestMatchers("/api/auth/lgpd/**").authenticated()

                        .requestMatchers("/api/sysadmin/**", "/api/auth/demo-users").hasRole(SYSADMIN)

                        .requestMatchers(HttpMethod.POST, "/api/grupo/membros/*/resposta").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/exportacao/**").hasAnyRole(TITULAR, MEMBRO)

                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole(TITULAR, MEMBRO, AUDITOR)

                        .requestMatchers(HttpMethod.POST, "/api/transacoes", "/api/importacao/**", "/api/assistente/**",
                                "/api/notificacoes/ler-todas").hasAnyRole(TITULAR, MEMBRO)
                        .requestMatchers(HttpMethod.DELETE, "/api/transacoes/*").hasAnyRole(TITULAR, MEMBRO)
                        .requestMatchers(HttpMethod.PATCH, "/api/notificacoes/*/lida").hasAnyRole(TITULAR, MEMBRO)

                        .requestMatchers("/api/**").hasRole(TITULAR)

                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                escreverErro(res, HttpServletResponse.SC_UNAUTHORIZED, "Não autenticado",
                                        "Faça login para acessar este recurso (RF-001)."))
                        .accessDeniedHandler((req, res, e) ->
                                escreverErro(res, HttpServletResponse.SC_FORBIDDEN, "Acesso negado",
                                        "O perfil " + papelAtual() + " não tem permissão para esta operação (OE4)."))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static String papelAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "atual";
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst().orElse("atual");
    }

    private void escreverErro(HttpServletResponse res, int status, String erro, String mensagem) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("erro", erro);
        body.put("mensagem", mensagem);
        res.setStatus(status);
        res.setCharacterEncoding("UTF-8");
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(res.getOutputStream(), body);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Content-Disposition", "Authorization"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
