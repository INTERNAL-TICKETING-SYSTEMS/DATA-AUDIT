package com.fasec.auditoria;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@SpringBootApplication
@EnableScheduling
@EnableAsync
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

    // Garante automaticamente que a coluna 'responsavel' existe na tabela 'chamados' no Render
    @Bean
    CommandLineRunner initDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE chamados ADD COLUMN IF NOT EXISTS responsavel VARCHAR(255);");
                System.out.println("[DB MIGRATION] Coluna 'responsavel' verificada/criada com sucesso!");
            } catch (Exception e) {
                System.err.println("[DB MIGRATION] Erro ao garantir a coluna responsavel: " + e.getMessage());
            }
        };
    }
}