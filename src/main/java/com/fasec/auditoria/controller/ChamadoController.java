package com.fasec.auditoria.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chamados")
@CrossOrigin(origins = "*")
public class ChamadoController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 1. Listar todos os chamados sincronizados
    @GetMapping
    public ResponseEntity<?> listarChamados() {
        try {
            String sql = "SELECT id, titulo, descricao, status, prioridade, solicitante_email, criado_em, atualizado_em FROM chamados ORDER BY criado_em DESC";
            List> chamados = jdbcTemplate.queryForList(sql);
            return ResponseEntity.ok(chamados);
        } catch (Exception e) {
            System.err.println("[ChamadoController] Erro ao listar chamados: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao listar chamados: " + e.getMessage());
        }
    }

    // 2. Criar um novo chamado
    @PostMapping
    public ResponseEntity<?> criarChamado(@RequestBody Map<String, Object> req) {
        try {
            String titulo = (String) req.get("titulo");
            String descricao = (String) req.get("descricao");
            String prioridade = req.get("prioridade") != null ? String.valueOf(req.get("prioridade")) : "MEDIA";
            String solicitanteEmail = (String) req.get("solicitanteEmail");

            String sql = "INSERT INTO chamados (titulo, descricao, status, prioridade, solicitante_email, criado_em, atualizado_em) " +
                         "VALUES (?, ?, 'ABERTO', ?, ?, ?, ?)";

            LocalDateTime agora = LocalDateTime.now();
            Timestamp ts = Timestamp.valueOf(agora);
            jdbcTemplate.update(sql, titulo, descricao, prioridade, solicitanteEmail, ts, ts);

            return ResponseEntity.status(201).body(Map.of(
                    "mensagem", "Chamado aberto e salvo com sucesso!",
                    "titulo", titulo != null ? titulo : "",
                    "status", "ABERTO",
                    "prioridade", prioridade,
                    "solicitanteEmail", solicitanteEmail != null ? solicitanteEmail : ""
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao criar chamado: " + e.getMessage());
        }
    }

    // 3. Atualizar status ou fechar/resolver chamado
    @PutMapping("/{id}/status")
    public ResponseEntity<?> atualizarStatusChamado(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        try {
            String novoStatus = req.containsKey("status") ? (String) req.get("status") : (String) req.get("novoStatus");
            if (novoStatus == null) {
                novoStatus = "FECHADO";
            }

            String sql = "UPDATE chamados SET status = ?, atualizado_em = ? WHERE id = ?";
            int linhasAfetadas = jdbcTemplate.update(sql, novoStatus, Timestamp.valueOf(LocalDateTime.now()), id);

            if (linhasAfetadas > 0) {
                return ResponseEntity.ok(Map.of("mensagem", "Estado do chamado atualizado com sucesso!"));
            } else {
                return ResponseEntity.status(404).body("Chamado não encontrado.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao atualizar status: " + e.getMessage());
        }
    }

    // 4. Atualizar dados gerais / Atribuir responsável
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarChamado(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        try {
            String responsavel = req.containsKey("responsavel") ? (String) req.get("responsavel") : null;
            String status = req.containsKey("status") ? (String) req.get("status") : null;
            String prioridade = req.containsKey("prioridade") ? (String) req.get("prioridade") : null;

            String sql = "UPDATE chamados SET " +
                         "status = COALESCE(?, status), " +
                         "prioridade = COALESCE(?, prioridade), " +
                         "atualizado_em = ? " +
                         "WHERE id = ?";

            int linhasAfetadas = jdbcTemplate.update(sql, status, prioridade, Timestamp.valueOf(LocalDateTime.now()), id);

            if (linhasAfetadas > 0) {
                return ResponseEntity.ok(Map.of("mensagem", "Chamado atualizado com sucesso!"));
            } else {
                return ResponseEntity.status(404).body("Chamado não encontrado.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao atualizar chamado: " + e.getMessage());
        }
    }
}