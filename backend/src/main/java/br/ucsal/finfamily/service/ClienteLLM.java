package br.ucsal.finfamily.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente HTTP do modelo de linguagem (LLM) usado pelo Assistente Conversacional (RF-012, Secao 11.2).
 *
 * Provedores suportados (propriedade finfamily.ai.provedor):
 * - GEMINI: API do Google Gemini (exige finfamily.ai.gemini-api-key / variavel GEMINI_API_KEY);
 * - OLLAMA: modelo executado localmente pelo Ollama (o texto nao sai da maquina);
 * - AUTO (padrao): usa o Gemini se houver chave configurada; caso contrario, tenta o Ollama local;
 * - LOCAL: nao chama nenhum LLM (apenas o motor de contingencia do AssistenteIAService).
 *
 * O cliente apenas envia o prompt e devolve o JSON gerado pelo modelo. Quem grava e consulta
 * os dados e sempre o proprio FinFamily (Secao 11.2). Em caso de erro ou tempo excedido
 * devolve Optional.empty() para que o assistente acione a contingencia da Secao 11.3.
 */
@Component
public class ClienteLLM {

    private static final Logger log = LoggerFactory.getLogger(ClienteLLM.class);

    /** Resposta bruta do modelo: o provedor que respondeu e o texto JSON devolvido. */
    public record RespostaLLM(String provedor, String json) {}

    @Value("${finfamily.ai.provedor:AUTO}")
    private String provedor;

    @Value("${finfamily.ai.timeout-ms:2800}")
    private int timeoutMs;

    @Value("${finfamily.ai.gemini-api-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiUrl;

    @Value("${finfamily.ai.gemini-model:gemini-2.0-flash}")
    private String geminiModel;

    @Value("${finfamily.ai.gemini-api-key:}")
    private String geminiApiKey;

    @Value("${finfamily.ai.ollama-url:http://localhost:11434/api/generate}")
    private String ollamaUrl;

    @Value("${finfamily.ai.ollama-model:llama3.2}")
    private String ollamaModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Envia o prompt ao provedor configurado e devolve o JSON gerado pelo modelo.
     */
    public Optional<RespostaLLM> gerarJson(String prompt) {
        String escolhido = resolverProvedor();
        try {
            return switch (escolhido) {
                case "GEMINI" -> chamarGemini(prompt);
                case "OLLAMA" -> chamarOllama(prompt);
                default -> Optional.empty();
            };
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Servico de IA ({}) indisponivel: {}", escolhido, e.getMessage());
            return Optional.empty();
        }
    }

    /** Provedor efetivamente usado, considerando a configuracao AUTO. */
    public String resolverProvedor() {
        String p = provedor == null ? "AUTO" : provedor.trim().toUpperCase();
        if ("AUTO".equals(p)) {
            return temChaveGemini() ? "GEMINI" : "OLLAMA";
        }
        if ("GEMINI".equals(p) && !temChaveGemini()) {
            log.warn("finfamily.ai.provedor=GEMINI, mas nenhuma chave foi configurada (GEMINI_API_KEY).");
            return "LOCAL";
        }
        return p;
    }

    private boolean temChaveGemini() {
        return geminiApiKey != null && !geminiApiKey.isBlank();
    }

    private Optional<RespostaLLM> chamarGemini(String prompt) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0
                )
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(geminiApiUrl + "/" + geminiModel + ":generateContent"))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", geminiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = novoHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("Gemini respondeu HTTP {}", response.statusCode());
            return Optional.empty();
        }
        JsonNode root = objectMapper.readTree(response.body());
        String texto = root.at("/candidates/0/content/parts/0/text").asText("");
        return texto.isBlank() ? Optional.empty() : Optional.of(new RespostaLLM("GEMINI (" + geminiModel + ")", texto));
    }

    private Optional<RespostaLLM> chamarOllama(String prompt) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "model", ollamaModel,
                "prompt", prompt,
                "stream", false,
                "format", "json",
                "options", Map.of("temperature", 0)
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaUrl))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = novoHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("Ollama respondeu HTTP {}", response.statusCode());
            return Optional.empty();
        }
        JsonNode root = objectMapper.readTree(response.body());
        String texto = root.path("response").asText("");
        return texto.isBlank() ? Optional.empty() : Optional.of(new RespostaLLM("OLLAMA (" + ollamaModel + ")", texto));
    }

    private HttpClient novoHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.min(timeoutMs, 1000)))
                .build();
    }
}
