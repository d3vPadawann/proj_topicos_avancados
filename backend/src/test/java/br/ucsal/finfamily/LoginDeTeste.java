// login usado nos testes
package br.ucsal.finfamily;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

final class LoginDeTeste {
    static final String TITULAR = "eduardorocha.santos@ucsal.edu.br";
    static final String MEMBRO = "danielteixeira.melo@ucsal.edu.br";
    static final String AUDITOR = "elton.figueiredo@pro.ucsal.edu.br";
    static final String SYSADMIN = "alexandrevillarino.silva@ucsal.edu.br";
    static final String CONVIDADO = "marcos.batista@ucsal.edu.br";
    static final String SENHA_DEMO = "123456";

    private static final ObjectMapper JSON = new ObjectMapper();

    private LoginDeTeste() {
    }

    static String bearer(MockMvc mockMvc, String email) throws Exception {
        String resposta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(java.util.Map.of("email", email, "senha", SENHA_DEMO))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JSON.readTree(resposta).get("token").asText();
    }

    static long usuarioId(MockMvc mockMvc, String email) throws Exception {
        String resposta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(java.util.Map.of("email", email, "senha", SENHA_DEMO))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(resposta).get("usuarioId").asLong();
    }
}
