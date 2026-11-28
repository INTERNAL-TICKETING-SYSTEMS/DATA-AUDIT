package com.fasec.auditoria.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/ver-tabela")
@CrossOrigin(origins = "*")
public class VisualizadorTabelasController {

    private static final Set<String> TABELAS_PERMITIDAS = Set.of("departamentos");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/{nomeTabela}")
    public ResponseEntity<?> verTabela(
            @PathVariable String nomeTabela,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader
    ) {
        String tabelaLimpa = nomeTabela == null ? "" : nomeTabela.trim().toLowerCase();
        String role = roleHeader == null ? "" : roleHeader.trim().toUpperCase();

        if (!"GESTOR".equals(role) && !"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("erro", "Acesso negado."));
        }

        if (!TABELAS_PERMITIDAS.contains(tabelaLimpa)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("erro", "Consulta direta não permitida para esta tabela."));
        }

        try {
            String sql = "SELECT * FROM " + tabelaLimpa + " LIMIT 50";
            List> registros = jdbcTemplate.queryForList(sql);
            return ResponseEntity.ok(registros);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "erro", "Não foi possível consultar a tabela.",
                    "detalhes", e.getMessage()
            ));
        }
    }
}