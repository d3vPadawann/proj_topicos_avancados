package br.ucsal.finfamily.repository;

import br.ucsal.finfamily.domain.model.OperacaoInvestimento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OperacaoInvestimentoRepository extends JpaRepository<OperacaoInvestimento, Long> {
    List<OperacaoInvestimento> findByContaCorretoraGrupoIdOrderByDataDescIdDesc(Long grupoId);
    List<OperacaoInvestimento> findByContaCorretoraIdOrderByDataDescIdDesc(Long contaId);
}
