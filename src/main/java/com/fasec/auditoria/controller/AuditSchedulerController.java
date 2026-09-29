package com.fasec.auditoria.controller;

import com.fasec.auditoria.service.AuditSchedulerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit/schedulers")
public class AuditSchedulerController {

    private final AuditSchedulerService schedulerService;

    public AuditSchedulerController(AuditSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @PostMapping("/run-scan")
    public ResponseEntity<Map<String, Object>> dispararVarreduraManual() {
        int anomalias = schedulerService.executarVarreduraConformidade();
        return ResponseEntity.ok(Map.of(
            "status", "Varredura de integridade executada com sucesso",
            "novasAnomaliasIdentificadas", anomalias
        ));
    }
}