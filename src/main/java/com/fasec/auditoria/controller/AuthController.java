package com.fasec.auditoria.controller;

import com.fasec.auditoria.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer parseDepartamentoId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int deptId = Integer.parseInt(raw.trim());
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM departamentos WHERE id = ?",
                    Integer.class,
                    deptId
            );
            return (exists != null && exists > 0) ? deptId : null;
        } catch (Exception ex) {
            return null;
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registrarUtilizador(@RequestBody RegisterRequest req) {
        try {
            String checkSql = "SELECT COUNT(*) FROM usuarios WHERE cpf = ? OR email = ?";
            Integer count = jdbcTemplate.queryForObject(
                    checkSql, Integer.class, req.getCpf(), req.getEmail()
            );

            if (count != null && count > 0) {
                return ResponseEntity.badRequest()
                        .body("Erro: Já existe um utilizador registado com este CPF ou E-mail.");
            }

            Integer deptId = parseDepartamentoId(req.getDepartamentoId());

            String sql = """
                    INSERT INTO usuarios (
                        nome, email, email_pessoal, cpf,
                        senha_hash, perfil, departamento_id,
                        ativo, criado_em, atualizado_em
                    )
                    VALUES (
                        ?, ?, ?, ?,
                        ?, 'SOLICITANTE', ?,
                        TRUE, NOW(), NOW()
                    )
                    """;

            jdbcTemplate.update(
                    sql,
                    req.getNome(),
                    req.getEmail(),
                    req.getEmailPessoal(),
                    req.getCpf(),
                    req.getSenha(),
                    deptId
            );

            return ResponseEntity.ok().body("Utilizador registado com sucesso no PostgreSQL!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Erro ao registar utilizador: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String identificador = credentials.get("identificador");

            String sql = "SELECT id, nome, email, perfil FROM usuarios WHERE email = ? OR cpf = ?";
            List<Map<String, Object>> usuarios = jdbcTemplate.queryForList(sql, identificador, identificador);

            if (usuarios.isEmpty()) {
                return ResponseEntity.status(404).body("Utilizador não encontrado no sistema.");
            }

            Map<String, Object> usuario = usuarios.get(0);
            return ResponseEntity.ok(Map.of(
                    "mensagem", "Login efetuado com sucesso!",
                    "utilizador", usuario
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro no login: " + e.getMessage());
        }
    }
}