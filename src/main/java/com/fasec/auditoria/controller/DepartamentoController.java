package com.fasec.auditoria.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/departamentos")
@CrossOrigin(origins = "*")
public class DepartamentoController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public ResponseEntity<?> listarDepartamentos() {
        try {
            String sql = "SELECT id, nome FROM departamentos ORDER BY nome ASC";
            List<Map<String, Object>> deps = jdbcTemplate.queryForList(sql);
            return ResponseEntity.ok(deps);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }
}