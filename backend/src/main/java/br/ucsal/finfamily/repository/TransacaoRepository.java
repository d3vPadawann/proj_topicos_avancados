package br.ucsal.finfamily.repository;

import br.ucsal.finfamily.domain.enums.TipoTransacao;
import br.ucsal.finfamily.domain.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    List<Transacao> findByContaGrupoIdOrderByDataDescIdDesc(Long grupoId);
    List<Transacao> findByContaIdOrderByDataDescIdDesc(Long contaId);
    List<Transacao> findByContaGrupoIdAndDataBetweenOrderByDataDescIdDesc(Long grupoId, LocalDate inicio, LocalDate fim);
    List<Transacao> findByContaGrupoIdAndTipoAndDataBetween(Long grupoId, TipoTransacao tipo, LocalDate inicio, LocalDate fim);
    boolean existsByHashConciliacao(String hashConciliacao);
    List<Transacao> findByAutorId(Long autorId);
}
