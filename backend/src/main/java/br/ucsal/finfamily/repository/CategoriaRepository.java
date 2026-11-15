package br.ucsal.finfamily.repository;

import br.ucsal.finfamily.domain.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByGrupoIdOrderByNomeAsc(Long grupoId);
    List<Categoria> findByGrupoIdAndPaiIsNullOrderByNomeAsc(Long grupoId);
    List<Categoria> findByPaiIdOrderByNomeAsc(Long paiId);
    Optional<Categoria> findFirstByGrupoIdAndNomeIgnoreCase(Long grupoId, String nome);
}
