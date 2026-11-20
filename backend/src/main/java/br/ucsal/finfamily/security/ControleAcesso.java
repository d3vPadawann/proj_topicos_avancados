package br.ucsal.finfamily.security;

import br.ucsal.finfamily.domain.enums.PapelFamiliar;
import br.ucsal.finfamily.domain.model.*;
import br.ucsal.finfamily.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Controle de acesso por grupo familiar e por usuario (RF-001, OE4, Secao 2.9).
 *
 * O SecurityConfig decide QUAIS perfis podem chamar cada rota. Esta classe complementa
 * verificando SE o registro pedido (conta, transacao, membro...) pertence ao grupo do
 * usuario logado, que vem sempre do token JWT e nunca de parametros enviados pelo navegador.
 */
@Component
@Transactional(readOnly = true)
public class ControleAcesso {

    /** Dados do usuario autenticado, extraidos do token JWT. */
    public record UsuarioLogado(Long usuarioId, Long grupoId, String email, String papel) {
        public boolean isTitular() {
            return PapelFamiliar.TITULAR.name().equals(papel);
        }
    }

    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;
    private final TagRepository tagRepository;
    private final TransacaoRepository transacaoRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final MembroRepository membroRepository;
    private final NotificacaoRepository notificacaoRepository;

    public ControleAcesso(ContaRepository contaRepository,
                          CategoriaRepository categoriaRepository,
                          TagRepository tagRepository,
                          TransacaoRepository transacaoRepository,
                          OrcamentoRepository orcamentoRepository,
                          MembroRepository membroRepository,
                          NotificacaoRepository notificacaoRepository) {
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
        this.tagRepository = tagRepository;
        this.transacaoRepository = transacaoRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.membroRepository = membroRepository;
        this.notificacaoRepository = notificacaoRepository;
    }

    /** Usuario logado; lanca excecao se a requisicao nao tiver token valido. */
    public UsuarioLogado atual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Map<?, ?> p)) {
            throw new AuthenticationCredentialsNotFoundException("Faça login para continuar.");
        }
        return new UsuarioLogado(
                ((Number) p.get("usuarioId")).longValue(),
                ((Number) p.get("grupoId")).longValue(),
                String.valueOf(p.get("email")),
                String.valueOf(p.get("papel"))
        );
    }

    public Long grupoId() {
        return atual().grupoId();
    }

    public Long usuarioId() {
        return atual().usuarioId();
    }

    // ------------------------------------------------------------------
    // Verificacoes de posse por grupo familiar
    // ------------------------------------------------------------------

    public void exigirConta(Long contaId) {
        if (contaId == null) return;
        Conta conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada."));
        exigirMesmoGrupo(conta.getGrupo());
    }

    /** Categorias sem grupo sao globais (padrao do sistema) e podem ser usadas por todos. */
    public void exigirCategoriaUtilizavel(Long categoriaId) {
        if (categoriaId == null) return;
        Categoria c = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));
        if (c.getGrupo() != null) {
            exigirMesmoGrupo(c.getGrupo());
        }
    }

    public void exigirCategoriaDoGrupo(Long categoriaId) {
        if (categoriaId == null) return;
        Categoria c = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));
        exigirMesmoGrupo(c.getGrupo());
    }

    public void exigirTag(Long tagId) {
        if (tagId == null) return;
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("Tag não encontrada."));
        exigirMesmoGrupo(tag.getGrupo());
    }

    public void exigirTags(Collection<Long> tagIds) {
        if (tagIds != null) tagIds.forEach(this::exigirTag);
    }

    /** O Titular exclui qualquer transacao do grupo; o Membro so as que ele mesmo lancou. */
    public void exigirTransacaoExcluivel(Long transacaoId) {
        Transacao t = transacaoRepository.findById(transacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada."));
        exigirMesmoGrupo(t.getConta() != null ? t.getConta().getGrupo() : null);
        UsuarioLogado eu = atual();
        if (!eu.isTitular() && (t.getAutor() == null || !Objects.equals(t.getAutor().getId(), eu.usuarioId()))) {
            throw new AccessDeniedException("O perfil MEMBRO só pode excluir as transações que ele mesmo lançou.");
        }
    }

    public void exigirOrcamento(Long orcamentoId) {
        Orcamento o = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado."));
        exigirMesmoGrupo(o.getGrupo());
    }

    public Membro exigirMembroDoGrupo(Long membroId) {
        Membro m = membroRepository.findById(membroId)
                .orElseThrow(() -> new IllegalArgumentException("Membro não encontrado."));
        exigirMesmoGrupo(m.getGrupo());
        return m;
    }

    /** Usuario indicado como proprietario de uma conta precisa participar do grupo. */
    public void exigirUsuarioDoGrupo(Long usuarioId) {
        if (usuarioId == null) return;
        Long grupo = grupoId();
        boolean participa = membroRepository.findByUsuarioId(usuarioId).stream()
                .anyMatch(m -> m.getGrupo() != null && Objects.equals(m.getGrupo().getId(), grupo));
        if (!participa) {
            throw new AccessDeniedException("O usuário informado não participa do seu grupo familiar.");
        }
    }

    /** So o proprio convidado responde ao convite. */
    public void exigirConviteDoUsuario(Long membroId) {
        Membro m = membroRepository.findById(membroId)
                .orElseThrow(() -> new IllegalArgumentException("Convite não encontrado."));
        UsuarioLogado eu = atual();
        boolean ehDoUsuario = (m.getUsuario() != null && Objects.equals(m.getUsuario().getId(), eu.usuarioId()))
                || (m.getEmailConvidado() != null && m.getEmailConvidado().equalsIgnoreCase(eu.email()));
        if (!ehDoUsuario) {
            throw new AccessDeniedException("Somente o próprio convidado pode responder a este convite.");
        }
    }

    public void exigirNotificacao(Long notificacaoId) {
        Notificacao n = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada."));
        if (n.getGrupo() != null) {
            exigirMesmoGrupo(n.getGrupo());
        } else if (n.getUsuario() == null || !Objects.equals(n.getUsuario().getId(), usuarioId())) {
            throw new AccessDeniedException("Esta notificação pertence a outro usuário.");
        }
    }

    /** RNF-004 (LGPD): portabilidade e exclusao valem apenas para os dados do proprio usuario. */
    public void exigirProprioUsuario(Long usuarioId) {
        if (!Objects.equals(usuarioId, usuarioId())) {
            throw new AccessDeniedException("Pela LGPD, você só pode exportar ou excluir os seus próprios dados.");
        }
    }

    /** Papeis que o Titular pode atribuir a membros do grupo (nunca TITULAR ou SYSADMIN). */
    public PapelFamiliar exigirPapelAtribuivel(PapelFamiliar papel) {
        if (papel == null) return null;
        if (papel != PapelFamiliar.MEMBRO && papel != PapelFamiliar.AUDITOR) {
            throw new AccessDeniedException("O Titular só pode atribuir os papéis MEMBRO ou AUDITOR.");
        }
        return papel;
    }

    private void exigirMesmoGrupo(GrupoFamiliar grupo) {
        if (grupo == null || !Objects.equals(grupo.getId(), grupoId())) {
            throw new AccessDeniedException("Este registro pertence a outro grupo familiar.");
        }
    }
}
