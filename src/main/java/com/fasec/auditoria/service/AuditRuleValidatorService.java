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

        String entidade = dto.entidade() != null ? dto.entidade().trim().toUpperCase() : "";
        String operacao = dto.tipoOperacao() != null ? dto.tipoOperacao().trim().toUpperCase() : "";
        String estadoAtual = dto.estadoAtual() != null ? dto.estadoAtual().toUpperCase() : "";

        boolean isChamado = "TICKET".equals(entidade) || "CHAMADO".equals(entidade);
        boolean isCriacao = "CREATE".equals(operacao) || "CRIACAO".equals(operacao);
        boolean isResolucaoOuFechamento = "RESOLUCAO".equals(operacao)
                || estadoAtual.contains("CONCLUIDO")
                || estadoAtual.contains("RESOLVIDO")
                || estadoAtual.contains("FECHADO");

        // REG-001: Fechamento/resolução de chamado sem parecer ou solução técnica
        if (isChamado && isResolucaoOuFechamento) {
            boolean temParecer = estadoAtual.contains("RESOLUCAO")
                    || estadoAtual.contains("PARECER")
                    || estadoAtual.contains("SOLUCAO");
            if (!temParecer) {
                violacoes.add(new DataViolation(
                    "REG-001",
                    "CRITICA",
                    "Tentativa de encerramento ou resolução de chamado sem descrição de parecer ou solução técnica.",
                    dto.entidade(),
                    dto.idEntidade()
                ));
            }
        }

        // REG-002: Transição ilegal direta para conclusão na criação
        if (isChamado && isCriacao
                && (estadoAtual.contains("CONCLUIDO") || estadoAtual.contains("RESOLVIDO") || estadoAtual.contains("FECHADO"))) {
            violacoes.add(new DataViolation(
                "REG-002",
                "ALTA",
                "Inconsistência de fluxo: chamado criado diretamente com status concluído/resolvido/fechado.",
                dto.entidade(),
                dto.idEntidade()
            ));
        }

        // REG-003: Falha de rastreabilidade de autoria
        String autor = dto.autor() != null ? dto.autor().trim().toUpperCase() : "";
        if (autor.isBlank() || "ANONIMO".equals(autor) || "SISTEMA_SEM_ID".equals(autor)) {
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