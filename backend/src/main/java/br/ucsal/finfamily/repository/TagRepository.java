package br.ucsal.finfamily.repository;

import br.ucsal.finfamily.domain.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByGrupoIdOrderByNomeAsc(Long grupoId);
}
