package br.ucsal.finfamily.repository;

import br.ucsal.finfamily.domain.model.GrupoFamiliar;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GrupoFamiliarRepository extends JpaRepository<GrupoFamiliar, Long> {
    List<GrupoFamiliar> findByTitularId(Long titularId);
}
