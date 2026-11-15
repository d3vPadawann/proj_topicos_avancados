package br.ucsal.finfamily.repository;

import br.ucsal.finfamily.domain.enums.TipoConta;
import br.ucsal.finfamily.domain.model.Conta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContaRepository extends JpaRepository<Conta, Long> {
    List<Conta> findByGrupoIdOrderByIdAsc(Long grupoId);
    List<Conta> findByGrupoIdAndTipo(Long grupoId, TipoConta tipo);
    List<Conta> findByProprietarioId(Long proprietarioId);
}
