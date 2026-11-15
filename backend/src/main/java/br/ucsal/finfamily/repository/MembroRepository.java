package br.ucsal.finfamily.repository;

import br.ucsal.finfamily.domain.model.Membro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MembroRepository extends JpaRepository<Membro, Long> {
    List<Membro> findByGrupoIdOrderByIdAsc(Long grupoId);
    List<Membro> findByUsuarioId(Long usuarioId);
    List<Membro> findByEmailConvidadoIgnoreCase(String email);
    Optional<Membro> findByGrupoIdAndEmailConvidadoIgnoreCase(Long grupoId, String email);
}
