package com.fasec.auditoria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@SpringBootApplication
@EnableScheduling
@RestController
public class AuditoriaDadosApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditoriaDadosApplication.class, args);
    }

    @GetMapping("/api/v1/health")
    public Map<String, String> status() {
        return Map.of(
            "status", "UP",
            "servico", "Subsistema de Auditoria de Dados",
            "versao", "0.0.1-SNAPSHOT"
        );
    }
}