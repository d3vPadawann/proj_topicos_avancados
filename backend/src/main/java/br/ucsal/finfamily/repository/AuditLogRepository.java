package br.ucsal.finfamily.repository;

import br.ucsal.finfamily.domain.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop200ByOrderByDataHoraDescIdDesc();
}
