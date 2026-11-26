package com.fasec.auditoria.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.*;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chamados")
@CrossOrigin(origins = "*")
public class ChamadoController {

    private final JdbcTemplate jdbcTemplate;

    public ChamadoController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void garantirColunaSolucao() {
        try {
            jdbcTemplate.execute("ALTER TABLE chamados ADD COLUMN IF NOT EXISTS solucao TEXT");
        } catch (Exception e) {
            System.err.println("[ChamadoController] Aviso ao verificar coluna solucao: " + e.getMessage());
        }
    }

    public record ChamadoDTO(
            Long id,
            String titulo,
            String descricao,
            String status,
            String prioridade,
            String solicitante_email,
            String solicitanteEmail,
            String tecnico_atribuido_email,
            String responsavel,
            String solucao,
            Timestamp criado_em,
            Timestamp atualizado_em
    ) {}

    private static final RowMapper<ChamadoDTO> CHAMADO_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        String emailSolicitante = rs.getString("solicitante_email");
        String tecnico = rs.getString("tecnico_atribuido_email");
        return new ChamadoDTO(
                rs.getLong("id"),
                rs.getString("titulo"),
                rs.getString("descricao"),
                rs.getString("status"),
                rs.getString("prioridade"),
                emailSolicitante,
                emailSolicitante,
                tecnico,
                tecnico,
                rs.getString("solucao"),
                rs.getTimestamp("criado_em"),
                rs.getTimestamp("atualizado_em")
        );
    };

    private String asNullableString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    @GetMapping
    public ResponseEntity<?> listarChamados() {
        try {
            String sql = """
                    SELECT id, titulo, descricao, status, prioridade,
                           solicitante_email, tecnico_atribuido_email, solucao,
                           criado_em, atualizado_em
                    FROM chamados
                    ORDER BY criado_em DESC, id DESC
                    """;
            List<ChamadoDTO> chamados = jdbcTemplate.query(sql, CHAMADO_ROW_MAPPER);
            return ResponseEntity.ok(chamados);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Falha ao consultar chamados", "detalhes", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> criarChamado(@RequestBody Map<String, Object> req) {
        try {
            String titulo = asNullableString(req.get("titulo"));
            String descricao = asNullableString(req.get("descricao"));
            String prioridade = req.get("prioridade") != null
                    ? asNullableString(req.get("prioridade"))
                    : "MEDIA";
            String solicitanteEmail = req.get("solicitanteEmail") != null
                    ? asNullableString(req.get("solicitanteEmail"))
                    : asNullableString(req.get("solicitante_email"));

            if (titulo == null || titulo.isBlank() || descricao == null || descricao.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Título e descrição são obrigatórios."));
            }

            Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
            String sql = """
                    INSERT INTO chamados (
                        titulo, descricao, status, prioridade,
                        solicitante_email, criado_em, atualizado_em
                    )
                    VALUES (?, ?, 'ABERTO', ?, ?, ?, ?)
                    RETURNING id, titulo, descricao, status, prioridade,
                              solicitante_email, tecnico_atribuido_email, solucao,
                              criado_em, atualizado_em
                    """;

            ChamadoDTO criado = jdbcTemplate.queryForObject(
                    sql,
                    CHAMADO_ROW_MAPPER,
                    titulo,
                    descricao,
                    prioridade,
                    solicitanteEmail,
                    agora,
                    agora
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(criado);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Falha ao criar chamado", "detalhes", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> atualizarStatusChamado(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        try {
            String novoStatus = req.containsKey("status")
                    ? asNullableString(req.get("status"))
                    : asNullableString(req.get("novoStatus"));

            if (novoStatus == null || novoStatus.isBlank()) {
                novoStatus = "FECHADO";
            }

            Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
            int linhas = jdbcTemplate.update(
                    "UPDATE chamados SET status = ?, atualizado_em = ? WHERE id = ?",
                    novoStatus,
                    agora,
                    id
            );

            if (linhas == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("erro", "Chamado não encontrado."));
            }

            return ResponseEntity.ok(Map.of("mensagem", "Status atualizado com sucesso.", "status", novoStatus));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Falha ao atualizar status", "detalhes", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarChamado(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        try {
            String responsavel = req.containsKey("responsavel")
                    ? asNullableString(req.get("responsavel"))
                    : asNullableString(req.get("tecnico_atribuido_email"));
            String status = asNullableString(req.get("status"));
            String prioridade = asNullableString(req.get("prioridade"));
            String solucao = asNullableString(req.get("solucao"));
            Timestamp agora = Timestamp.valueOf(LocalDateTime.now());

            String sql = """
                    UPDATE chamados
                    SET tecnico_atribuido_email = COALESCE(?, tecnico_atribuido_email),
                        status = COALESCE(?, status),
                        prioridade = COALESCE(?, prioridade),
                        solucao = COALESCE(?, solucao),
                        atualizado_em = ?
                    WHERE id = ?
                    """;

            int linhas = jdbcTemplate.update(sql, responsavel, status, prioridade, solucao, agora, id);

            if (linhas == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("erro", "Chamado não encontrado."));
            }

            return ResponseEntity.ok(Map.of("mensagem", "Chamado atualizado com sucesso."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Falha ao atualizar chamado", "detalhes", e.getMessage()));
        }
    }
}