package br.ucsal.finfamily.repository;

import br.ucsal.finfamily.domain.model.Ativo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AtivoRepository extends JpaRepository<Ativo, String> {
    Optional<Ativo> findByTickerIgnoreCase(String ticker);
}
