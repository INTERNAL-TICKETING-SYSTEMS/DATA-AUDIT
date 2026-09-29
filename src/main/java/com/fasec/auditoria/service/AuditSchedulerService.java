package com.fasec.auditoria.service;

import com.fasec.auditoria.model.AuditEvent;
import com.fasec.auditoria.model.DataViolation;
import com.fasec.auditoria.repository.AuditEventRepository;
import com.fasec.auditoria.repository.DataViolationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(AuditSchedulerService.class);

    private final AuditEventRepository auditEventRepository;
    private final DataViolationRepository dataViolationRepository;

    public AuditSchedulerService(AuditEventRepository auditEventRepository, 
                                 DataViolationRepository dataViolationRepository) {
        this.auditEventRepository = auditEventRepository;
        this.dataViolationRepository = dataViolationRepository;
    }

    // Executa periodicamente a cada 5 minutos
    @Scheduled(fixedRate = 300000)
    public void rotinaAgendadaVarredura() {
        log.info("[SCHEDULER] Iniciando varredura automatizada de integridade temporal...");
        executarVarreduraConformidade();
    }

    public int executarVarreduraConformidade() {
        // Marco temporal: registros anteriores a 48 horas
        LocalDateTime limiteTemporal = LocalDateTime.now().minusHours(48);

        // O PostgreSQL filtra diretamente no disco, trazendo apenas os tickets candidatos
        List<AuditEvent> eventosCandidatos = auditEventRepository
                .findByEntidadeAndDataHoraEventoBefore("TICKET", limiteTemporal);

        int anomaliasDetectadas = 0;

        for (AuditEvent evento : eventosCandidatos) {
            if (evento.getEstadoAtual() != null && evento.getEstadoAtual().toUpperCase().contains("ABERTO")) {

                boolean jaRegistrado = dataViolationRepository.findByEntidadeAfetadaAndIdEntidadeAfetada(
                        evento.getEntidade(), evento.getIdEntidade()
                ).stream().anyMatch(v -> "REG-004".equals(v.getCodigoRegra()));

                if (!jaRegistrado) {
                    DataViolation violacao = new DataViolation(
                        "REG-004",
                        "MEDIA",
                        "Alerta de Estagnação: Chamado em estado ABERTO por mais de 48 horas sem movimentação operacional.",
                        evento.getEntidade(),
                        evento.getIdEntidade()
                    );
                    dataViolationRepository.save(violacao);
                    anomaliasDetectadas++;
                    log.warn("[SCHEDULER] Inconformidade REG-004 gerada para a entidade {} id {}", 
                            evento.getEntidade(), evento.getIdEntidade());
                }
            }
        }

        log.info("[SCHEDULER] Varredura finalizada. Anomalias identificadas nesta rodada: {}", anomaliasDetectadas);
        return anomaliasDetectadas;
    }
}