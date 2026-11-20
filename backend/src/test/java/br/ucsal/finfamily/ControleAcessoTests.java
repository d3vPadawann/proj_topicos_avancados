package br.ucsal.finfamily;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controle de acesso no back-end (RF-001, OE4, Secao 2.9, Tabela 7 e RNF-004):
 * cada perfil so faz o que o documento da Unidade I permite, e ninguem acessa outro grupo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ControleAcessoTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    private String token(String email) throws Exception {
        return LoginDeTeste.bearer(mockMvc, email);
    }

    private static final String NOVA_CONTA = """
            {"nome":"Conta Teste","instituicao":"Banco","tipo":"CORRENTE","saldo":0}
            """;

    private static final String NOVA_TRANSACAO = """
            {"descricao":"Teste de acesso","valor":10.00,"tipo":"DESPESA"}
            """;

    // ---------------------------------------------------------------- sem login

    @Test
    @DisplayName("RF-001: sem token a API responde 401")
    void semTokenRecebe401() throws Exception {
        mockMvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/contas").contentType(MediaType.APPLICATION_JSON).content(NOVA_CONTA))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/auth/lgpd/delete/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/demo-users")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Rotas publicas continuam abertas: health e login")
    void rotasPublicasContinuamAbertas() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
        assertTrue(token(LoginDeTeste.TITULAR).startsWith("Bearer "));
    }

    // ---------------------------------------------------------------- auditor

    @Test
    @DisplayName("OE4: Auditor le os paineis, mas nao grava nem exporta")
    void auditorSomenteLeitura() throws Exception {
        String auditor = token(LoginDeTeste.AUDITOR);
        mockMvc.perform(get("/api/dashboard").header("Authorization", auditor)).andExpect(status().isOk());
        mockMvc.perform(get("/api/transacoes").header("Authorization", auditor)).andExpect(status().isOk());

        mockMvc.perform(post("/api/contas").header("Authorization", auditor)
                        .contentType(MediaType.APPLICATION_JSON).content(NOVA_CONTA))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/transacoes").header("Authorization", auditor)
                        .contentType(MediaType.APPLICATION_JSON).content(NOVA_TRANSACAO))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/assistente/interpretar").header("Authorization", auditor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"gastei 10 reais\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/exportacao/csv").header("Authorization", auditor))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- membro

    @Test
    @DisplayName("Secao 2.9: Membro lanca transacoes e exporta, mas nao altera configuracoes do grupo")
    void membroLancaMasNaoConfigura() throws Exception {
        String membro = token(LoginDeTeste.MEMBRO);
        mockMvc.perform(post("/api/transacoes").header("Authorization", membro)
                        .contentType(MediaType.APPLICATION_JSON).content(NOVA_TRANSACAO))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/exportacao/csv").header("Authorization", membro)).andExpect(status().isOk());

        mockMvc.perform(post("/api/contas").header("Authorization", membro)
                        .contentType(MediaType.APPLICATION_JSON).content(NOVA_CONTA))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/orcamentos").header("Authorization", membro)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"categoriaId\":1,\"valorPlanejado\":100}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/grupo/convidar").header("Authorization", membro)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"X\",\"email\":\"x@x.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Membro so exclui as transacoes que ele mesmo lancou")
    void membroNaoExcluiTransacaoDeOutro() throws Exception {
        String titular = token(LoginDeTeste.TITULAR);
        String resposta = mockMvc.perform(post("/api/transacoes").header("Authorization", titular)
                        .contentType(MediaType.APPLICATION_JSON).content(NOVA_TRANSACAO))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long idDoTitular = JSON.readTree(resposta).get(0).get("id").asLong();

        String membro = token(LoginDeTeste.MEMBRO);
        mockMvc.perform(delete("/api/transacoes/" + idDoTitular).header("Authorization", membro))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/transacoes/" + idDoTitular).header("Authorization", titular))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- titular

    @Test
    @DisplayName("Titular gerencia contas, mas nao pode promover alguem a Sysadmin ou Titular")
    void titularGerenciaComLimites() throws Exception {
        String titular = token(LoginDeTeste.TITULAR);
        mockMvc.perform(post("/api/contas").header("Authorization", titular)
                        .contentType(MediaType.APPLICATION_JSON).content(NOVA_CONTA))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/grupo/convidar").header("Authorization", titular)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Hacker\",\"email\":\"h@h.com\",\"papel\":\"SYSADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- sysadmin

    @Test
    @DisplayName("UC13: Sysadmin ve logs e faz backup, mas nao acessa dados financeiros")
    void sysadminSemDadosFinanceiros() throws Exception {
        String sysadmin = token(LoginDeTeste.SYSADMIN);
        mockMvc.perform(get("/api/sysadmin/logs").header("Authorization", sysadmin)).andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard").header("Authorization", sysadmin)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/transacoes").header("Authorization", sysadmin)).andExpect(status().isForbidden());

        String titular = token(LoginDeTeste.TITULAR);
        mockMvc.perform(get("/api/sysadmin/logs").header("Authorization", titular)).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- convidado

    @Test
    @DisplayName("RF-007: convidado que ainda nao aceitou o convite nao ve os dados do grupo")
    void convidadoNaoVeDados() throws Exception {
        String convidado = token(LoginDeTeste.CONVIDADO);
        mockMvc.perform(get("/api/dashboard").header("Authorization", convidado)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF-007 / BPMN Figura 2: so o proprio convidado responde ao convite")
    void soOConvidadoRespondeAoConvite() throws Exception {
        String login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + LoginDeTeste.CONVIDADO + "\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode resp = JSON.readTree(login);
        assertEquals("CONVIDADO", resp.get("statusMembro").asText());
        long membroId = resp.get("membroId").asLong();

        String titular = token(LoginDeTeste.TITULAR);
        mockMvc.perform(post("/api/grupo/membros/" + membroId + "/resposta").header("Authorization", titular)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"aceitar\":true}"))
                .andExpect(status().isForbidden());

        String convidado = "Bearer " + resp.get("token").asText();
        mockMvc.perform(post("/api/grupo/membros/" + membroId + "/resposta").header("Authorization", convidado)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"aceitar\":true}"))
                .andExpect(status().isOk());

        // Depois de aceitar, um novo login ja da acesso aos dados do grupo
        mockMvc.perform(get("/api/dashboard").header("Authorization", token(LoginDeTeste.CONVIDADO)))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- LGPD

    @Test
    @DisplayName("RNF-004: LGPD vale so para os dados do proprio usuario")
    void lgpdSoDoProprioUsuario() throws Exception {
        String membro = token(LoginDeTeste.MEMBRO);
        long idMembro = LoginDeTeste.usuarioId(mockMvc, LoginDeTeste.MEMBRO);
        long idTitular = LoginDeTeste.usuarioId(mockMvc, LoginDeTeste.TITULAR);

        mockMvc.perform(get("/api/auth/lgpd/export/" + idMembro).header("Authorization", membro))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/lgpd/export/" + idTitular).header("Authorization", membro))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/auth/lgpd/delete/" + idTitular).header("Authorization", membro))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RNF-003: o CPF nunca aparece nas respostas da API")
    void cpfNuncaApareceNasRespostas() throws Exception {
        String titular = token(LoginDeTeste.TITULAR);
        mockMvc.perform(get("/api/grupo/membros").header("Authorization", titular))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("documentoSensivel"))));
        mockMvc.perform(get("/api/contas").header("Authorization", titular))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("documentoSensivel"))));
    }

    // ---------------------------------------------------------------- isolamento entre grupos

    @Test
    @DisplayName("Isolamento: cada familia so ve e altera os dados do proprio grupo")
    void gruposFicamIsolados() throws Exception {
        // Nova familia, cadastrada sem nome de grupo: deve ganhar um grupo proprio
        String email = "familia." + UUID.randomUUID() + "@teste.com";
        String cadastro = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outra Familia\",\"email\":\"" + email + "\",\"senha\":\"segredo123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode novo = JSON.readTree(cadastro);
        String outraFamilia = "Bearer " + novo.get("token").asText();

        String titular = token(LoginDeTeste.TITULAR);
        long grupoDemo = JSON.readTree(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + LoginDeTeste.TITULAR + "\",\"senha\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()).get("grupoId").asLong();
        assertNotEquals(grupoDemo, novo.get("grupoId").asLong(), "Cadastro sem convite nao pode entrar no grupo de outra familia");

        // A nova familia tenta forcar o grupoId da familia de demonstracao no corpo: e ignorado
        String contaCriada = mockMvc.perform(post("/api/contas").header("Authorization", outraFamilia)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Conta Isolada\",\"tipo\":\"CORRENTE\",\"saldo\":0,\"grupoId\":" + grupoDemo + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long contaDaOutraFamilia = JSON.readTree(contaCriada).get("id").asLong();

        mockMvc.perform(get("/api/contas").header("Authorization", titular))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Conta Isolada"))));

        // O titular da familia de demonstracao nao altera nem le registros da outra familia
        mockMvc.perform(delete("/api/contas/" + contaDaOutraFamilia).header("Authorization", titular))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/transacoes?contaId=" + contaDaOutraFamilia).header("Authorization", titular))
                .andExpect(status().isForbidden());
    }
}
