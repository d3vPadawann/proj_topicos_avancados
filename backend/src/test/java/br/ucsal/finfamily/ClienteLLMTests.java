// testes do cliente da IA
package br.ucsal.finfamily;

import br.ucsal.finfamily.service.ClienteLLM;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ClienteLLMTests {
    private HttpServer servidor;
    private String baseUrl;
    private final AtomicReference<String> corpoRecebido = new AtomicReference<>();
    private final AtomicReference<String> chaveRecebida = new AtomicReference<>();

    @BeforeEach
    void iniciarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1beta/models/modelo-teste:generateContent", troca -> {
            corpoRecebido.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            chaveRecebida.set(troca.getRequestHeaders().getFirst("x-goog-api-key"));
            responder(troca, """
                    {"candidates":[{"content":{"parts":[{"text":"{\\"intencao\\":\\"registrar\\",\\"valor\\":10.5}"}]}}]}
                    """);
        });
        servidor.createContext("/api/generate", troca -> {
            corpoRecebido.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            responder(troca, "{\"response\":\"{\\\"intencao\\\":\\\"consultar\\\"}\",\"done\":true}");
        });
        servidor.start();
        baseUrl = "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    @AfterEach
    void pararServidor() {
        servidor.stop(0);
    }

    private static void responder(com.sun.net.httpserver.HttpExchange troca, String corpo) throws IOException {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().add("Content-Type", "application/json");
        troca.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = troca.getResponseBody()) {
            os.write(bytes);
        }
    }

    private ClienteLLM novoCliente(String provedor, String chaveGemini) {
        ClienteLLM cliente = new ClienteLLM();
        ReflectionTestUtils.setField(cliente, "provedor", provedor);
        ReflectionTestUtils.setField(cliente, "timeoutMs", 2800);
        ReflectionTestUtils.setField(cliente, "geminiApiUrl", baseUrl + "/v1beta/models");
        ReflectionTestUtils.setField(cliente, "geminiModel", "modelo-teste");
        ReflectionTestUtils.setField(cliente, "geminiApiKey", chaveGemini);
        ReflectionTestUtils.setField(cliente, "ollamaUrl", baseUrl + "/api/generate");
        ReflectionTestUtils.setField(cliente, "ollamaModel", "llama-teste");
        return cliente;
    }

    @Test
    @DisplayName("Gemini: envia o prompt com a chave no cabecalho e le o JSON gerado")
    void deveChamarGemini() {
        Optional<ClienteLLM.RespostaLLM> r = novoCliente("AUTO", "chave-123").gerarJson("meu prompt");

        assertTrue(r.isPresent());
        assertEquals("GEMINI (modelo-teste)", r.get().provedor());
        assertTrue(r.get().json().contains("\"registrar\""));
        assertEquals("chave-123", chaveRecebida.get(), "A chave deve ir no cabecalho, nao na URL");
        assertTrue(corpoRecebido.get().contains("meu prompt"));
        assertTrue(corpoRecebido.get().contains("application/json"), "Deve pedir resposta em JSON");
    }

    @Test
    @DisplayName("AUTO sem chave do Gemini: usa o Ollama local")
    void deveChamarOllamaQuandoNaoHaChaveGemini() {
        Optional<ClienteLLM.RespostaLLM> r = novoCliente("AUTO", "").gerarJson("outro prompt");

        assertTrue(r.isPresent());
        assertEquals("OLLAMA (llama-teste)", r.get().provedor());
        assertTrue(r.get().json().contains("consultar"));
        assertTrue(corpoRecebido.get().contains("\"format\":\"json\""));
    }

    @Test
    @DisplayName("LOCAL: nao chama nenhum servico de IA")
    void naoDeveChamarNadaNoModoLocal() {
        assertTrue(novoCliente("LOCAL", "chave-123").gerarJson("prompt").isEmpty());
        assertNull(corpoRecebido.get());
    }

    @Test
    @DisplayName("Servico fora do ar: devolve vazio para acionar a contingencia")
    void deveDevolverVazioQuandoServicoIndisponivel() {
        ClienteLLM cliente = novoCliente("OLLAMA", "");
        servidor.stop(0);

        assertTrue(cliente.gerarJson("prompt").isEmpty());
    }
}
