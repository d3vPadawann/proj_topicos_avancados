package br.ucsal.finfamily.repository;

import br.ucsal.finfamily.domain.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    List<Notificacao> findByGrupoIdOrderByCriadaEmDescIdDesc(Long grupoId);
    List<Notificacao> findByGrupoIdAndLidaFalseOrderByCriadaEmDesc(Long grupoId);
}
