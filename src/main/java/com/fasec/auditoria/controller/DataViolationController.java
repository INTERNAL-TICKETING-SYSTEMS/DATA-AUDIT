package com.fasec.auditoria.controller;

import com.fasec.auditoria.model.DataViolation;
import com.fasec.auditoria.repository.DataViolationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit/violations")
public class DataViolationController {

    private final DataViolationRepository repository;

    public DataViolationController(DataViolationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<Page<DataViolation>> listarViolacoes(
            @PageableDefault(page = 0, size = 10, sort = "dataIdentificacao", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/severidade/{severidade}")
    public ResponseEntity<Page<DataViolation>> listarPorSeveridade(
            @PathVariable String severidade,
            @PageableDefault(page = 0, size = 10, sort = "dataIdentificacao", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findBySeveridade(severidade.toUpperCase(), pageable));
    }

    @PatchMapping("/{id}/resolver")
    public ResponseEntity<Map<String, Object>> resolverViolacao(@PathVariable Long id) {
        DataViolation violacao = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inconformidade não encontrada."));

        violacao.setStatusResolucao("RESOLVIDA");
        repository.save(violacao);

        return ResponseEntity.ok(Map.of(
                "id", violacao.getId(),
                "codigoRegra", violacao.getCodigoRegra(),
                "statusResolucao", violacao.getStatusResolucao(),
                "mensagem", "Inconformidade operacional tratada e resolvida com sucesso."
        ));
    }
}