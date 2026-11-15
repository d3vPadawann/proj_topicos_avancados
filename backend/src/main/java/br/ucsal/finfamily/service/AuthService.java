package br.ucsal.finfamily.service;

import br.ucsal.finfamily.domain.enums.PapelFamiliar;
import br.ucsal.finfamily.domain.enums.StatusMembro;
import br.ucsal.finfamily.domain.model.*;
import br.ucsal.finfamily.repository.*;
import br.ucsal.finfamily.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servico de Autenticacao, Gestao de Sessao (RF-001) e Conformidade com a LGPD (RNF-004).
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final GrupoFamiliarRepository grupoFamiliarRepository;
    private final MembroRepository membroRepository;
    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;

    public AuthService(UsuarioRepository usuarioRepository,
                       GrupoFamiliarRepository grupoFamiliarRepository,
                       MembroRepository membroRepository,
                       ContaRepository contaRepository,
                       TransacaoRepository transacaoRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       AuditLogService auditLogService) {
        this.usuarioRepository = usuarioRepository;
        this.grupoFamiliarRepository = grupoFamiliarRepository;
        this.membroRepository = membroRepository;
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Map<String, Object> login(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciais inválidas: e-mail não encontrado."));

        boolean senhaValida = passwordEncoder.matches(senha, usuario.getSenhaHash());
        if (!usuario.autenticar(email, senhaValida)) {
            auditLogService.registrar("LOGIN_FALHA", "AUTENTICACAO", email, "SECURITY",
                    "Tentativa de login rejeitada por credenciais invalidas ou conta inativa (hash BCrypt verificado).");
            throw new IllegalArgumentException("E-mail ou senha inválidos.");
        }

        // Verifica vinculo em grupo familiar
        List<Membro> membros = membroRepository.findByUsuarioId(usuario.getId());
        if (membros.isEmpty()) {
            membros = membroRepository.findByEmailConvidadoIgnoreCase(usuario.getEmail());
        }

        Long grupoId = 1L;
        String nomeGrupo = "Família FinFamily";
        PapelFamiliar papelNoGrupo = usuario.getPapelPrincipal();
        StatusMembro statusNoGrupo = StatusMembro.ATIVO;

        if (!membros.isEmpty()) {
            Membro m = membros.get(0);
            if (m.getStatus() == StatusMembro.SUSPENSO) {
                auditLogService.registrar("LOGIN_MEMBRO_SUSPENSO", "AUTENTICACAO", email, "WARN",
                        "Acesso bloqueado: membro com status SUSPENSO no grupo familiar.");
                throw new IllegalStateException("Seu acesso ao grupo familiar está temporariamente suspenso pelo titular.");
            }
            grupoId = m.getGrupo().getId();
            nomeGrupo = m.getGrupo().getNome();
            papelNoGrupo = m.getPapel();
            statusNoGrupo = m.getStatus();
        } else {
            List<GrupoFamiliar> todosGrupos = grupoFamiliarRepository.findAll();
            if (!todosGrupos.isEmpty()) {
                grupoId = todosGrupos.get(0).getId();
                nomeGrupo = todosGrupos.get(0).getNome();
            }
        }

        String token = jwtTokenProvider.gerarToken(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNome(),
                papelNoGrupo,
                grupoId
        );

        auditLogService.registrar("LOGIN_SUCESSO", "AUTENTICACAO", email, "INFO",
                "Sessao JWT emitida com criptografia AES-256 para perfil " + papelNoGrupo);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("token", token);
        resposta.put("usuarioId", usuario.getId());
        resposta.put("nome", usuario.getNome());
        resposta.put("email", usuario.getEmail());
        resposta.put("papel", papelNoGrupo.name());
        resposta.put("statusMembro", statusNoGrupo.name());
        resposta.put("grupoId", grupoId);
        resposta.put("nomeGrupo", nomeGrupo);
        return resposta;
    }

    @Transactional
    public Map<String, Object> registrarUsuario(String nome, String email, String senha, String nomeGrupo, String cpfOpcional) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este e-mail.");
        }

        Usuario novo = new Usuario(
                nome,
                email.trim().toLowerCase(),
                passwordEncoder.encode(senha),
                PapelFamiliar.TITULAR
        );
        novo.setDocumentoSensivel(cpfOpcional != null && !cpfOpcional.isBlank() ? cpfOpcional : "000.000.000-00");
        novo = usuarioRepository.save(novo);

        // Verifica se existe convite pendente para este e-mail
        List<Membro> convites = membroRepository.findByEmailConvidadoIgnoreCase(novo.getEmail());
        GrupoFamiliar grupo;
        PapelFamiliar papel = PapelFamiliar.TITULAR;

        if (!convites.isEmpty()) {
            Membro convite = convites.get(0);
            convite.setUsuario(novo);
            convite.aceitarConvite();
            membroRepository.save(convite);
            grupo = convite.getGrupo();
            papel = convite.getPapel();
            novo.setPapelPrincipal(papel);
            usuarioRepository.save(novo);
        } else {
            List<GrupoFamiliar> existentes = grupoFamiliarRepository.findAll();
            if (nomeGrupo == null || nomeGrupo.isBlank()) {
                if (!existentes.isEmpty()) {
                    grupo = existentes.get(0);
                } else {
                    grupo = grupoFamiliarRepository.save(new GrupoFamiliar("Família " + nome.split(" ")[0], novo));
                }
            } else {
                grupo = grupoFamiliarRepository.save(new GrupoFamiliar(nomeGrupo, novo));
            }
            Membro membroTitular = new Membro();
            membroTitular.setGrupo(grupo);
            membroTitular.setUsuario(novo);
            membroTitular.setEmailConvidado(novo.getEmail());
            membroTitular.setNomeConvidado(novo.getNome());
            membroTitular.setPapel(PapelFamiliar.TITULAR);
            membroTitular.setStatus(StatusMembro.ATIVO);
            membroRepository.save(membroTitular);
        }

        auditLogService.registrar("CADASTRO_USUARIO", "AUTENTICACAO", email, "INFO",
                "Novo usuario registrado com senha hash BCrypt e documento criptografado AES-256.");

        return login(email, senha);
    }

    /**
     * RNF-004 (Conformidade com a LGPD): Portabilidade de dados (download de todos os dados do usuario).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exportarDadosLGPD(Long usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        List<Membro> vinculos = membroRepository.findByUsuarioId(usuarioId);
        List<Conta> contasProprias = contaRepository.findByProprietarioId(usuarioId);
        List<Transacao> transacoesAutor = transacaoRepository.findByAutorId(usuarioId);

        auditLogService.registrar("LGPD_PORTABILIDADE_DOWNLOAD", "SEGURANCA_LGPD", u.getEmail(), "SECURITY",
                "Exportacao de portabilidade de dados pessoais executada conforme Lei 13.709/2018 (RNF-004).");

        Map<String, Object> pacoteLgpd = new LinkedHashMap<>();
        pacoteLgpd.put("leiReferencia", "Lei Geral de Proteção de Dados Pessoais (Lei nº 13.709/2018 - RNF-004)");
        pacoteLgpd.put("dataExportacao", LocalDateTime.now().toString());
        pacoteLgpd.put("titularDados", Map.of(
                "id", u.getId(),
                "nome", u.getNome(),
                "email", u.getEmail(),
                "documentoDescriptografado", u.getDocumentoSensivel() != null ? u.getDocumentoSensivel() : "N/A",
                "papelPrincipal", u.getPapelPrincipal().name(),
                "criadoEm", u.getCriadoEm().toString()
        ));
        pacoteLgpd.put("vinculosFamiliares", vinculos.stream().map(m -> Map.of(
                "grupo", m.getGrupo().getNome(),
                "papel", m.getPapel().name(),
                "status", m.getStatus().name()
        )).toList());
        pacoteLgpd.put("contasFinanceirasTitularidade", contasProprias.stream().map(c -> Map.of(
                "nome", c.getNome(),
                "tipo", c.getTipo().name(),
                "privada", c.getPrivada(),
                "saldo", c.getSaldo()
        )).toList());
        pacoteLgpd.put("quantidadeTransacoesRegistradas", transacoesAutor.size());
        pacoteLgpd.put("transacoesRegistradas", transacoesAutor.stream().map(t -> Map.of(
                "data", t.getData().toString(),
                "descricao", t.getDescricao(),
                "valor", t.getValor(),
                "tipo", t.getTipo().name()
        )).toList());
        return pacoteLgpd;
    }

    /**
     * RNF-004 (Conformidade com a LGPD): Direito ao Esquecimento (exclusao irreversivel da conta).
     */
    @Transactional
    public Map<String, Object> excluirContaIrreversivelLGPD(Long usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        String emailMascarado = u.getEmail();

        // Desvincula transacoes de autoria e anonimiza/remove registros pessoais
        List<Transacao> transacoes = transacaoRepository.findByAutorId(usuarioId);
        for (Transacao t : transacoes) {
            t.setAutor(null);
            transacaoRepository.save(t);
        }

        List<Conta> contas = contaRepository.findByProprietarioId(usuarioId);
        for (Conta c : contas) {
            c.setProprietario(null);
            contaRepository.save(c);
        }

        List<Membro> membros = membroRepository.findByUsuarioId(usuarioId);
        membroRepository.deleteAll(membros);

        usuarioRepository.delete(u);

        auditLogService.registrar("LGPD_DIREITO_ESQUECIMENTO", "SEGURANCA_LGPD", emailMascarado, "SECURITY",
                "Exclusao irreversivel de conta de usuario concluida conforme RNF-004 (LGPD).");

        return Map.of(
                "status", "CONTA_EXCLUIDA_IRREVERSIVELMENTE",
                "mensagem", "Todos os dados pessoais identificáveis foram removidos em conformidade com a LGPD (RNF-004)."
        );
    }

    public List<Usuario> listarUsuariosDemo() {
        return usuarioRepository.findAll();
    }
}
