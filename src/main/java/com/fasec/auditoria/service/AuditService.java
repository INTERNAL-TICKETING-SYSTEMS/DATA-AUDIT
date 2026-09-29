package com.fasec.auditoria.service;

import com.fasec.auditoria.dto.AuditEventRequestDTO;
import com.fasec.auditoria.model.AuditEvent;
import com.fasec.auditoria.repository.AuditEventRepository;
import com.fasec.auditoria.util.HashUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditEventRepository repository;
    private final AuditRuleValidatorService validatorService;

    public AuditService(AuditEventRepository repository, AuditRuleValidatorService validatorService) {
        this.repository = repository;
        this.validatorService = validatorService;
    }

    @Transactional
    public AuditEvent registrarEvento(AuditEventRequestDTO dto) {
        // 1. Gera o hash criptográfico de integridade forense
        String hash = HashUtil.gerarHashSha256(
            dto.origem(),
            dto.entidade(),
            dto.idEntidade(),
            dto.tipoOperacao(),
            dto.autor(),
            dto.dataHoraEvento().toString(),
            dto.estadoAtual()
        );

        AuditEvent evento = new AuditEvent(
            dto.origem(),
            dto.entidade(),
            dto.idEntidade(),
            dto.tipoOperacao(),
            dto.autor(),
            dto.dataHoraEvento(),
            dto.estadoAnterior(),
            dto.estadoAtual(),
            dto.metadados(),
            hash
        );

        AuditEvent salvo = repository.save(evento);

        // 2. Aciona o motor de validação de regras de integridade operacional
        validatorService.validarEvento(dto);

        return salvo;
    }

    public Page<AuditEvent> listarPaginado(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<AuditEvent> listarPorEntidadePaginado(String entidade, String idEntidade, Pageable pageable) {
        return repository.findByEntidadeAndIdEntidade(entidade, idEntidade, pageable);
    }

    public List<AuditEvent> listarTodos() {
        return repository.findAll();
    }

    public List<AuditEvent> listarPorEntidade(String entidade, String idEntidade) {
        return repository.findByEntidadeAndIdEntidade(entidade, idEntidade);
    }
}