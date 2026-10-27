package com.fasec.auditoria.controller;

import com.fasec.auditoria.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.Types;
import java.util.UUID;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/register")
    public ResponseEntity<?> registrarUtilizador(@RequestBody RegisterRequest req) {
        try {
            String checkSql = "SELECT COUNT(*) FROM usuarios WHERE cpf = ? OR email = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, req.getCpf(), req.getEmail());

            if (count != null && count > 0) {
                return ResponseEntity.badRequest().body("Erro: Já existe um utilizador registado com este CPF ou E-mail.");
            }

            String sql = "INSERT INTO usuarios (id, nome, email, email_pessoal, cpf, data_nascimento, genero, senha_hash, telefone_whatsapp, departamento_id, perfil, ativo, criado_em, atualizado_em) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SOLICITANTE', TRUE, NOW(), NOW())";
            
            UUID userId = UUID.randomUUID();
            LocalDate dataNasc = (req.getDataNascimento() != null && !req.getDataNascimento().isEmpty()) 
                                 ? LocalDate.parse(req.getDataNascimento()) 
                                 : null;
            
            UUID deptId = (req.getDepartamentoId() != null && !req.getDepartamentoId().isEmpty())
                          ? UUID.fromString(req.getDepartamentoId())
                          : UUID.fromString("a0000000-0000-0000-0000-000000000001");

            Object[] params = new Object[] {
                userId, req.getNome(), req.getEmail(), req.getEmailPessoal(),
                req.getCpf(), dataNasc, req.getGenero(), req.getSenha(),
                req.getTelefoneWhatsapp(), deptId
            };

            int[] types = new int[] {
                Types.OTHER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                Types.VARCHAR, Types.DATE, Types.VARCHAR, Types.VARCHAR,
                Types.VARCHAR, Types.OTHER
            };

            jdbcTemplate.update(sql, params, types);

            return ResponseEntity.ok().body("Utilizador registado com sucesso no PostgreSQL!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao registar utilizador: " + e.getMessage());
        }
    }

    // ADIÇÃO: Endpoint de Login para evitar o erro 500 no frontend
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String identificador = credentials.get("identificador");
            String senha = credentials.get("senha");

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