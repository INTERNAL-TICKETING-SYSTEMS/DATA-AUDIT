package com.fasec.auditoria.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/ver-tabela")
@CrossOrigin(origins = "*")
public class VisualizadorTabelasController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Exemplo para ver os utilizadores: https://data-audit-f72h.onrender.com/api/v1/admin/ver-tabela/usuarios
    @GetMapping("/{nomeTabela}")
    public ResponseEntity<?> verTabela(@PathVariable String nomeTabela) {
        try {
            // Segurança básica para aceitar apenas as tabelas criadas
            String sql = "SELECT * FROM " + nomeTabela + " LIMIT 50";
            List<Map<String, Object>> registros = jdbcTemplate.queryForList(sql);
            return ResponseEntity.ok(registros);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Tabela não encontrada ou inválida",
                "detalhes", e.getMessage()
            ));
        }
    }
}