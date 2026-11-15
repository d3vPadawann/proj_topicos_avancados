package br.ucsal.finfamily.repository;

import br.ucsal.finfamily.domain.model.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {
    List<Orcamento> findByGrupoIdOrderByMesDescIdAsc(Long grupoId);
    List<Orcamento> findByGrupoIdAndMesOrderByIdAsc(Long grupoId, String mes);
    Optional<Orcamento> findByGrupoIdAndCategoriaIdAndMes(Long grupoId, Long categoriaId, String mes);
}
