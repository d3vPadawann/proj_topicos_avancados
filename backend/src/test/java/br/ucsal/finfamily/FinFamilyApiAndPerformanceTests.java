package br.ucsal.finfamily;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de Integracao de API REST e Desempenho / Latencia (RNF-001: < 500ms no P95).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FinFamilyApiAndPerformanceTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("RF-001 & OE4: Login de usuario via API REST deve retornar JWT e perfil correto")
    void deveAutenticarViaRestApiERetornarJwt() throws Exception {
        String jsonLogin = """
                {
                  "email": "eduardorocha.santos@ucsal.edu.br",
                  "senha": "123456"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.papel").value("TITULAR"));
    }

    @Test
    @DisplayName("RNF-001: Tempo de resposta de leitura deve ser inferior a 500ms no 95o percentil")
    void deveAtenderMetaDeLatenciaP95InferiorA500msRNF001() throws Exception {
        List<Long> latenciasMs = new ArrayList<>();
        String token = LoginDeTeste.bearer(mockMvc, LoginDeTeste.TITULAR);

        for (int i = 0; i < 20; i++) {
            long t0 = System.nanoTime();
            mockMvc.perform(get("/api/dashboard").header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patrimonioLiquidoFamiliar").isNumber());
            long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
            latenciasMs.add(elapsedMs);
        }

        Collections.sort(latenciasMs);
        int idxP95 = (int) Math.ceil(latenciasMs.size() * 0.95) - 1;
        long p95Ms = latenciasMs.get(idxP95);

        assertTrue(p95Ms <= 500L, "RNF-001: P95 de leitura deve ser <= 500ms. Obtido: " + p95Ms + "ms");
    }
}
