package br.ucsal.finfamily.controller;

import br.ucsal.finfamily.domain.model.Usuario;
import br.ucsal.finfamily.security.ControleAcesso;
import br.ucsal.finfamily.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST para Autenticacao e Gestao de Sessao (RF-001) e Conformidade LGPD (RNF-004).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final ControleAcesso controleAcesso;

    public AuthController(AuthService authService, ControleAcesso controleAcesso) {
        this.authService = authService;
        this.controleAcesso = controleAcesso;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String senha = payload.get("senha");
        return ResponseEntity.ok(authService.login(email, senha));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registrar(@RequestBody Map<String, String> payload) {
        String nome = payload.get("nome");
        String email = payload.get("email");
        String senha = payload.get("senha");
        String nomeGrupo = payload.get("nomeGrupo");
        String cpf = payload.get("documentoSensivel");
        return ResponseEntity.ok(authService.registrarUsuario(nome, email, senha, nomeGrupo, cpf));
    }

    @GetMapping("/demo-users")
    public ResponseEntity<List<Usuario>> listarUsuariosDemo() {
        return ResponseEntity.ok(authService.listarUsuariosDemo());
    }

    /**
     * RNF-004 (LGPD): Portabilidade de Dados Pessoais (Download em formato JSON).
     */
    @GetMapping("/lgpd/export/{usuarioId}")
    public ResponseEntity<Map<String, Object>> exportarDadosLgpd(@PathVariable Long usuarioId) {
        controleAcesso.exigirProprioUsuario(usuarioId);
        return ResponseEntity.ok(authService.exportarDadosLGPD(usuarioId));
    }

    /**
     * RNF-004 (LGPD): Direito ao Esquecimento (Exclusao irreversivel de conta).
     */
    @DeleteMapping("/lgpd/delete/{usuarioId}")
    public ResponseEntity<Map<String, Object>> excluirContaLgpd(@PathVariable Long usuarioId) {
        controleAcesso.exigirProprioUsuario(usuarioId);
        return ResponseEntity.ok(authService.excluirContaIrreversivelLGPD(usuarioId));
    }
}
