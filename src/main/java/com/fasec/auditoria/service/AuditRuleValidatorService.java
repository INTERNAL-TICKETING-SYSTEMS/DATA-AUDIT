package com.fasec.auditoria.service;

import com.fasec.auditoria.dto.AuditEventRequestDTO;
import com.fasec.auditoria.model.DataViolation;
import com.fasec.auditoria.repository.DataViolationRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditRuleValidatorService {

    private final DataViolationRepository violationRepository;

    public AuditRuleValidatorService(DataViolationRepository violationRepository) {
        this.violationRepository = violationRepository;
    }

    public List<DataViolation> validarEvento(AuditEventRequestDTO dto) {
        List<DataViolation> violacoes = new ArrayList<>();

        // REG-001: Fechamento de ticket sem parecer ou payload de resolução
        if ("TICKET".equalsIgnoreCase(dto.entidade()) && 
            dto.estadoAtual() != null && 
            (dto.estadoAtual().toUpperCase().contains("CONCLUIDO") || dto.estadoAtual().toUpperCase().contains("FECHADO"))) {
            
            if (!dto.estadoAtual().toUpperCase().contains("RESOLUCAO") && 
                !dto.estadoAtual().toUpperCase().contains("PARECER")) {
                violacoes.add(new DataViolation(
                    "REG-001",
                    "CRITICA",
                    "Tentativa de encerramento de chamado sem descrição de parecer ou resolução técnica.",
                    dto.entidade(),
                    dto.idEntidade()
                ));
            }
        }

        // REG-002: Transição ilegal direta para conclusão na criação
        if ("CREATE".equalsIgnoreCase(dto.tipoOperacao()) && 
            dto.estadoAtual() != null && 
            dto.estadoAtual().toUpperCase().contains("CONCLUIDO")) {
            violacoes.add(new DataViolation(
                "REG-002",
                "ALTA",
                "Inconsistência de fluxo: chamado criado diretamente com status CONCLUIDO.",
                dto.entidade(),
                dto.idEntidade()
            ));
        }

        // REG-003: Falha de rastreabilidade de autoria
        if (dto.autor() == null || dto.autor().trim().equalsIgnoreCase("ANONIMO") || dto.autor().trim().equalsIgnoreCase("SISTEMA_SEM_ID")) {
            violacoes.add(new DataViolation(
                "REG-003",
                "MEDIA",
                "Operação realizada sem identificação de autor autenticado válido.",
                dto.entidade(),
                dto.idEntidade()
            ));
        }

        if (!violacoes.isEmpty()) {
            return violationRepository.saveAll(violacoes);
        }

        return List.of();
    }
}