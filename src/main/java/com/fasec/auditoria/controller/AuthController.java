package com.fasec.auditoria.controller;

import com.fasec.auditoria.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
            String emailNormalizado = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : "";
            String cpfLimpo = req.getCpf() != null ? req.getCpf().trim() : "";

            if (emailNormalizado.isBlank() || cpfLimpo.isBlank()) {
                return ResponseEntity.badRequest().body("CPF e e-mail são obrigatórios.");
            }

            String checkSql = "SELECT COUNT(*) FROM usuarios WHERE cpf = ? OR LOWER(email) = ?";
            Integer count = jdbcTemplate.queryForObject(
                    checkSql, Integer.class, cpfLimpo, emailNormalizado
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
                    emailNormalizado,
                    req.getEmailPessoal(),
                    cpfLimpo,
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
            String senha = credentials.get("senha");

            if (identificador == null || identificador.isBlank() || senha == null || senha.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("erro", "Informe e-mail/CPF e senha."));
            }

            String idTrim = identificador.trim();
            String sql = """
                    SELECT u.id, u.nome, u.email, u.email_pessoal, u.cpf,
                           u.perfil, u.senha_hash, u.ativo,
                           COALESCE(d.nome, 'Sem setor definido') AS departamento
                    FROM usuarios u
                    LEFT JOIN departamentos d ON d.id = u.departamento_id
                    WHERE LOWER(u.email) = LOWER(?) OR u.cpf = ?
                    """;

            List<Map<String, Object>> usuarios = jdbcTemplate.queryForList(sql, idTrim, idTrim);

            if (usuarios.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("erro", "Usuário não cadastrado. Realize o Primeiro Acesso."));
            }

            Map<String, Object> usuario = usuarios.get(0);
            Boolean ativo = (Boolean) usuario.get("ativo");
            if (ativo != null && !ativo) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("erro", "Usuário inativo."));
            }

            String senhaSalva = usuario.get("senha_hash") != null ? String.valueOf(usuario.get("senha_hash")) : "";
            if (!senha.equals(senhaSalva)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("erro", "Senha inválida."));
            }

            usuario.remove("senha_hash");
            return ResponseEntity.ok(Map.of(
                    "mensagem", "Login efetuado com sucesso!",
                    "utilizador", usuario
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("erro", "Erro no login: " + e.getMessage()));
        }
    }
}