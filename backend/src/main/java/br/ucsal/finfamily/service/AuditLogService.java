package br.ucsal.finfamily.service;

import br.ucsal.finfamily.domain.model.AuditLog;
import br.ucsal.finfamily.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servico de Auditoria e Logs Tecnicos de Seguranca (UC13, RNF-003).
 * Exclusivo para consulta pelo perfil SYSADMIN, sem expor dados financeiros descriptografados.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog registrar(String acao, String modulo, String emailUsuario, String nivel, String detalhesTecnicos) {
        AuditLog log = new AuditLog(acao, modulo, emailUsuario, nivel, detalhesTecnicos);
        return auditLogRepository.save(log);
    }

    public List<AuditLog> listarLogsRecentes() {
        return auditLogRepository.findTop200ByOrderByDataHoraDescIdDesc();
    }
}
