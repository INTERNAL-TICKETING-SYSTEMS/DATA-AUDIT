package com.fasec.auditoria.controller;

import com.fasec.auditoria.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.Types;
import java.util.UUID;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/register")
    public ResponseEntity<?> registrarUtilizador(@RequestBody RegisterRequest req) {
        try {
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
                userId,
                req.getNome(),
                req.getEmail(),
                req.getEmailPessoal(),
                req.getCpf(),
                dataNasc,
                req.getGenero(),
                req.getSenha(),
                req.getTelefoneWhatsapp(),
                deptId
            };

            int[] types = new int[] {
                Types.OTHER, // id (UUID)
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.DATE,    // data_nascimento
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.OTHER  // departamento_id (UUID)
            };

            jdbcTemplate.update(sql, params, types);

            return ResponseEntity.ok().body("Utilizador registado com sucesso no PostgreSQL!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao registar utilizador: " + e.getMessage());
        }
    }
}