package com.fasec.auditoria.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chamados")
@CrossOrigin(origins = "*")
public class ChamadoController {

    @GetMapping
    public ResponseEntity<List<Object>> listarChamados() {
        // Retorna uma lista vazia temporariamente para evitar o erro 500
        return ResponseEntity.ok(List.of());
    }

    @PostMapping
    public ResponseEntity<?> criarChamado(@RequestBody Object novoChamado) {
        // Lógica básica para aceitar a criação do chamado
        return ResponseEntity.status(201).body(novoChamado);
    }
}