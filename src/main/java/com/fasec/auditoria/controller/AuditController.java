package com.fasec.auditoria.controller;

import com.fasec.auditoria.dto.AuditEventRequestDTO;
import com.fasec.auditoria.model.AuditEvent;
import com.fasec.auditoria.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit/events")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping
    public ResponseEntity<AuditEvent> registrarEvento(@Valid @RequestBody AuditEventRequestDTO dto) {
        AuditEvent eventoSalvo = auditService.registrarEvento(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoSalvo);
    }

    @GetMapping
    public ResponseEntity<Page<AuditEvent>> listarPaginado(
            @PageableDefault(page = 0, size = 10, sort = "dataHoraEvento", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditService.listarPaginado(pageable));
    }

    @GetMapping("/{entidade}/{idEntidade}")
    public ResponseEntity<Page<AuditEvent>> listarPorEntidadePaginado(
            @PathVariable String entidade,
            @PathVariable String idEntidade,
            @PageableDefault(page = 0, size = 10, sort = "dataHoraEvento", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditService.listarPorEntidadePaginado(entidade, idEntidade, pageable));
    }

    @GetMapping("/{id}/verificar-integridade")
    public ResponseEntity<Map<String, Object>> verificarIntegridade(@PathVariable Long id) {
        return ResponseEntity.ok(auditService.verificarIntegridade(id));
    }
}