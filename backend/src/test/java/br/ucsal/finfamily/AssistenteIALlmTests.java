package br.ucsal.finfamily;

import br.ucsal.finfamily.domain.model.Comando;
import br.ucsal.finfamily.service.AssistenteIAService;
import br.ucsal.finfamily.service.ClienteLLM;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Valida a integracao do Assistente com o LLM (RF-012, Secao 11.2 e 11.3) usando um
 * ClienteLLM simulado, sem depender de rede nem de chave de API.
 */
@SpringBootTest
class AssistenteIALlmTests {

    @Autowired
    private AssistenteIAService assistenteIAService;

    @MockBean
    private ClienteLLM clienteLLM;

    private void llmResponde(String json) {
        when(clienteLLM.gerarJson(anyString()))
                .thenReturn(Optional.of(new ClienteLLM.RespostaLLM("GEMINI (teste)", json)));
    }

    @Test
    @DisplayName("Secao 11.2: os campos do card vem do JSON devolvido pelo LLM")
    void deveMontarCardComOsCamposDevolvidosPeloLlm() {
        String ontem = LocalDate.now().minusDays(1).toString();
        llmResponde("""
                {"intencao":"registrar","tipo":"DESPESA","valor":45.9,"descricao":"Padaria Pão Quente",
                 "data":"%s","conta":"Conta Digital Conjunta","categoria":"Alimentação",
                 "parcelas":1,"recorrente":false,"busca":"","periodo":"mes_atual"}
                """.formatted(ontem));

        Comando cmd = assistenteIAService.interpretar(1L, "gastei 45,90 na padaria pão quente ontem pela conjunta");

        assertEquals("GEMINI (teste)", cmd.getProvedorIA());
        assertEquals("registrar", cmd.getIntencao());
        assertEquals(0, new BigDecimal("45.90").compareTo(cmd.getValor()));
        assertEquals("Padaria Pão Quente", cmd.getDescricao(), "A descricao deve ser a do LLM, nao a do motor de regras");
        assertEquals(LocalDate.now().minusDays(1), cmd.getData());
        assertEquals("Conta Digital Conjunta", cmd.getConta());
        assertNotNull(cmd.getContaId(), "O nome da conta devolvido pelo LLM deve ser associado a uma conta do grupo");
        assertTrue(cmd.getRequerConfirmacao(), "RN-006: registro exige confirmacao do card");
    }

    @Test
    @DisplayName("Secao 11.2: consulta interpretada pelo LLM gera o filtro de busca")
    void deveInterpretarConsultaPeloLlm() {
        llmResponde("""
                {"intencao":"consultar","tipo":"DESPESA","valor":null,"descricao":null,"data":null,
                 "conta":null,"categoria":null,"parcelas":1,"recorrente":false,"busca":"Uber","periodo":"mes_passado"}
                """);

        Comando cmd = assistenteIAService.interpretar(1L, "quanto saiu de uber no mês passado?");

        assertEquals("consultar", cmd.getIntencao());
        assertEquals("Uber", cmd.getBusca());
        assertEquals("mes_passado", cmd.getPeriodo());
        assertFalse(cmd.getRequerConfirmacao(), "RN-006: consultas apenas leem dados");
    }

    @Test
    @DisplayName("Secao 11.3: sem resposta do LLM, o motor local de contingencia assume")
    void deveUsarContingenciaQuandoLlmIndisponivel() {
        when(clienteLLM.gerarJson(anyString())).thenReturn(Optional.empty());

        Comando cmd = assistenteIAService.interpretar(1L, "Registrar gasto de R$ 45,90 na padaria hoje no cartão de crédito");

        assertEquals(AssistenteIAService.PROVEDOR_CONTINGENCIA, cmd.getProvedorIA());
        assertEquals(0, new BigDecimal("45.90").compareTo(cmd.getValor()));
    }

    @Test
    @DisplayName("Secao 11.3: resposta invalida do LLM tambem aciona a contingencia")
    void deveUsarContingenciaQuandoLlmDevolveJsonInvalido() {
        llmResponde("desculpe, não consegui entender");

        Comando cmd = assistenteIAService.interpretar(1L, "Gastei 32,50 com Uber hoje no cartão");

        assertEquals(AssistenteIAService.PROVEDOR_CONTINGENCIA, cmd.getProvedorIA());
        assertEquals(0, new BigDecimal("32.50").compareTo(cmd.getValor()));
    }

    @Test
    @DisplayName("Secao 11.3: pedido de recomendacao de investimento e recusado")
    void deveRecusarPedidoForaDoEscopo() {
        llmResponde("{\"intencao\":\"fora_escopo\"}");

        Comando cmd = assistenteIAService.interpretar(1L, "vale a pena comprar bitcoin agora?");

        assertEquals("fora_escopo", cmd.getIntencao());
        assertFalse(cmd.getRequerConfirmacao());
    }

    @Test
    @DisplayName("UC12 fluxo 3a: comando nao entendido pede para reformular")
    void devePedirParaReformularQuandoLlmNaoEntende() {
        llmResponde("{\"intencao\":\"nao_entendido\"}");

        assertThrows(IllegalArgumentException.class,
                () -> assistenteIAService.interpretar(1L, "blá blá blá"));
    }
}
