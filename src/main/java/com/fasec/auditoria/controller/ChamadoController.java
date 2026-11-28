package com.fasec.auditoria.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/v1/chamados")
@CrossOrigin(origins = "*")
public class ChamadoController {

    private final JdbcTemplate jdbcTemplate;
    private final Path uploadRootPath;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public ChamadoController(
            JdbcTemplate jdbcTemplate,
            @Value("${app.upload.dir:/app/uploads}") String uploadDir
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.uploadRootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void inicializarEstrutura() {
        try {
            jdbcTemplate.execute("ALTER TABLE chamados ADD COLUMN IF NOT EXISTS solucao TEXT");
            jdbcTemplate.execute("ALTER TABLE chamados ADD COLUMN IF NOT EXISTS protocolo VARCHAR(50)");
            jdbcTemplate.execute("ALTER TABLE chamados ADD COLUMN IF NOT EXISTS categoria VARCHAR(100)");
            jdbcTemplate.execute("ALTER TABLE chamados ADD COLUMN IF NOT EXISTS solicitante_departamento VARCHAR(180)");
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS chamado_mensagens (
                        id SERIAL PRIMARY KEY,
                        chamado_id INT REFERENCES chamados(id) ON DELETE CASCADE,
                        remetente_email VARCHAR(150) NOT NULL,
                        mensagem TEXT,
                        imagem_url VARCHAR(500),
                        criado_em TIMESTAMP DEFAULT NOW()
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS feedbacks (
                        id SERIAL PRIMARY KEY,
                        chamado_id INT REFERENCES chamados(id) ON DELETE SET NULL,
                        usuario_email VARCHAR(150) NOT NULL,
                        tipo VARCHAR(50) NOT NULL,
                        comentario TEXT NOT NULL,
                        nota INT CHECK (nota >= 1 AND nota <= 5),
                        criado_em TIMESTAMP DEFAULT NOW()
                    )
                    """);
            Files.createDirectories(uploadRootPath);
        } catch (Exception e) {
            System.err.println("[ChamadoController] Aviso na inicialização: " + e.getMessage());
        }
    }

    public record ChamadoDTO(
            Long id,
            String protocolo,
            String titulo,
            String descricao,
            String categoria,
            String status,
            String prioridade,
            String solicitante_email,
            String solicitanteEmail,
            String solicitanteDepartamento,
            String tecnico_atribuido_email,
            String responsavel,
            String solucao,
            Timestamp criado_em,
            Timestamp atualizado_em
    ) {}

    public record MensagemDTO(
            Long id,
            Long chamado_id,
            String remetente_email,
            String mensagem,
            String imagem_url,
            Timestamp criado_em
    ) {}

    public record FeedbackDTO(
            Long id,
            Long chamado_id,
            String usuario_email,
            String tipo,
            String comentario,
            Integer nota,
            Timestamp criado_em
    ) {}

    public record MembroEquipeDTO(
            Long id,
            String nome,
            String email,
            String perfil,
            String departamento,
            boolean ativo,
            boolean disponivel
    ) {}

    private static final RowMapper<ChamadoDTO> CHAMADO_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        String emailSolicitante = rs.getString("solicitante_email");
        String tecnico = rs.getString("tecnico_atribuido_email");
        return new ChamadoDTO(
                rs.getLong("id"),
                rs.getString("protocolo"),
                rs.getString("titulo"),
                rs.getString("descricao"),
                rs.getString("categoria"),
                rs.getString("status"),
                rs.getString("prioridade"),
                emailSolicitante,
                emailSolicitante,
                rs.getString("solicitante_departamento"),
                tecnico,
                tecnico,
                rs.getString("solucao"),
                rs.getTimestamp("criado_em"),
                rs.getTimestamp("atualizado_em")
        );
    };

    private static final RowMapper<MensagemDTO> MENSAGEM_ROW_MAPPER = (ResultSet rs, int rowNum) -> new MensagemDTO(
            rs.getLong("id"),
            rs.getLong("chamado_id"),
            rs.getString("remetente_email"),
            rs.getString("mensagem"),
            rs.getString("imagem_url"),
            rs.getTimestamp("criado_em")
    );

    private static final RowMapper<FeedbackDTO> FEEDBACK_ROW_MAPPER = (ResultSet rs, int rowNum) -> new FeedbackDTO(
            rs.getLong("id"),
            (Long) rs.getObject("chamado_id"),
            rs.getString("usuario_email"),
            rs.getString("tipo"),
            rs.getString("comentario"),
            (Integer) rs.getObject("nota"),
            rs.getTimestamp("criado_em")
    );

    private void notificarSse(String tipoEvento, Object payload) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(tipoEvento).data(payload));
            } catch (Exception ex) {
                emitters.remove(emitter);
            }
        }
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChamados() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("conectado").data(Map.of("status", "ok")));
        } catch (Exception ex) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    private String asNullableString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private Long asNullableLong(Object value) {
        if (value == null) return null;
        try {
            return Long.valueOf(String.valueOf(value).replaceAll("\\D", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer asNullableInt(Object value) {
        if (value == null) return null;
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/equipe")
    public ResponseEntity<?> listarEquipe() {
        try {
            String sql = """
                    SELECT u.id, u.nome, u.email, u.perfil,
                           COALESCE(d.nome, 'Suporte Técnico STI') AS departamento,
                           COALESCE(u.ativo, TRUE) AS ativo
                    FROM usuarios u
                    LEFT JOIN departamentos d ON d.id = u.departamento_id
                    WHERE COALESCE(u.ativo, TRUE) = TRUE
                      AND (
                        UPPER(COALESCE(u.perfil, '')) LIKE '%TEC%'
                        OR UPPER(COALESCE(u.perfil, '')) LIKE '%GEST%'
                        OR UPPER(COALESCE(u.perfil, '')) LIKE '%ADMIN%'
                        OR LOWER(COALESCE(u.email, '')) IN (
                            'carlos.daniel@sti.chamados.com',
                            'joao.pedro@sti.chamados.com',
                            'guilherme.ferreira@sti.chamados.com',
                            'wanderson.maior@sti.chamados.com',
                            'luigue.brandao@sti.chamados.com',
                            'elias.junior@sti.chamados.com'
                        )
                      )
                    ORDER BY u.nome ASC
                    """;

            List<MembroEquipeDTO> equipe = jdbcTemplate.query(sql, (rs, rowNum) -> {
                boolean isAtivo = rs.getBoolean("ativo");
                return new MembroEquipeDTO(
                        rs.getLong("id"),
                        rs.getString("nome"),
                        rs.getString("email"),
                        rs.getString("perfil"),
                        rs.getString("departamento"),
                        isAtivo,
                        isAtivo
                );
            });

            return ResponseEntity.ok(equipe);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Falha ao carregar equipe", "detalhes", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listarChamados(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String email
    ) {
        try {
            boolean restringirAoSolicitante = "usuario".equalsIgnoreCase(role)
                    && email != null && !email.isBlank();

            if (restringirAoSolicitante) {
                String sql = """
                        SELECT id, protocolo, titulo, descricao, categoria, status, prioridade,
                               solicitante_email, solicitante_departamento, tecnico_atribuido_email,
                               solucao, criado_em, atualizado_em
                        FROM chamados
                        WHERE LOWER(solicitante_email) = LOWER(?)
                        ORDER BY criado_em DESC, id DESC
                        """;
                return ResponseEntity.ok(jdbcTemplate.query(sql, CHAMADO_ROW_MAPPER, email.trim()));
            }

            String sql = """
                    SELECT id, protocolo, titulo, descricao, categoria, status, prioridade,
                           solicitante_email, solicitante_departamento, tecnico_atribuido_email,
                           solucao, criado_em, atualizado_em
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
            String protocolo = asNullableString(req.get("protocolo"));
            String titulo = asNullableString(req.get("titulo"));
            String descricao = asNullableString(req.get("descricao"));
            String categoria = req.get("categoria") != null ? asNullableString(req.get("categoria")) : "Geral";
            String prioridade = req.get("prioridade") != null
                    ? asNullableString(req.get("prioridade"))
                    : "MEDIA";
            String solicitanteEmail = req.get("solicitanteEmail") != null
                    ? asNullableString(req.get("solicitanteEmail"))
                    : asNullableString(req.get("solicitante_email"));
            String solicitanteDepartamento = req.get("solicitanteDepartamento") != null
                    ? asNullableString(req.get("solicitanteDepartamento"))
                    : asNullableString(req.get("solicitante_departamento"));

            if (titulo == null || titulo.isBlank() || descricao == null || descricao.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Título e descrição são obrigatórios."));
            }

            if (protocolo == null || protocolo.isBlank()) {
                protocolo = "STI-" + LocalDateTime.now().getYear() + "-" + (int) (10000 + Math.random() * 90000);
            }

            Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
            String sql = """
                    INSERT INTO chamados (
                        protocolo, titulo, descricao, categoria, status, prioridade,
                        solicitante_email, solicitante_departamento, criado_em, atualizado_em
                    )
                    VALUES (?, ?, ?, ?, 'ABERTO', ?, ?, ?, ?, ?)
                    RETURNING id, protocolo, titulo, descricao, categoria, status, prioridade,
                              solicitante_email, solicitante_departamento, tecnico_atribuido_email,
                              solucao, criado_em, atualizado_em
                    """;

            ChamadoDTO criado = jdbcTemplate.queryForObject(
                    sql,
                    CHAMADO_ROW_MAPPER,
                    protocolo,
                    titulo,
                    descricao,
                    categoria,
                    prioridade,
                    solicitanteEmail,
                    solicitanteDepartamento,
                    agora,
                    agora
            );

            notificarSse("chamado-criado", criado);
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

            notificarSse("chamado-atualizado", Map.of("id", id, "status", novoStatus));
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

            notificarSse("chamado-atualizado", Map.of("id", id));
            return ResponseEntity.ok(Map.of("mensagem", "Chamado atualizado com sucesso."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Falha ao atualizar chamado", "detalhes", e.getMessage()));
        }
    }

    @GetMapping("/{id}/mensagens")
    public ResponseEntity<?> listarMensagens(@PathVariable Long id) {
        try {
            String sql = """
                    SELECT id, chamado_id, remetente_email, mensagem, imagem_url, criado_em
                    FROM chamado_mensagens
                    WHERE chamado_id = ?
                    ORDER BY criado_em ASC, id ASC
                    """;
            List<MensagemDTO> mensagens = jdbcTemplate.query(sql, MENSAGEM_ROW_MAPPER, id);
            return ResponseEntity.ok(mensagens);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Falha ao listar mensagens", "detalhes", e.getMessage()));
        }
    }

    @PostMapping("/{id}/mensagens")
    public ResponseEntity<?> criarMensagem(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        try {
            String remetenteEmail = req.get("remetenteEmail") != null
                    ? asNullableString(req.get("remetenteEmail"))
                    : asNullableString(req.get("remetente_email"));
            String mensagem = asNullableString(req.get("mensagem"));
            String imagemUrl = req.get("imagemUrl") != null
                    ? asNullableString(req.get("imagemUrl"))
                    : asNullableString(req.get("imagem_url"));

            if ((mensagem == null || mensagem.isBlank()) && (imagemUrl == null || imagemUrl.isBlank())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Informe uma mensagem ou imagem."));
            }

            Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
            String sql = """
                    INSERT INTO chamado_mensagens (chamado_id, remetente_email, mensagem, imagem_url, criado_em)
                    VALUES (?, ?, ?, ?, ?)
                    RETURNING id, chamado_id, remetente_email, mensagem, imagem_url, criado_em
                    """;

            MensagemDTO salva = jdbcTemplate.queryForObject(
                    sql,
                    MENSAGEM_ROW_MAPPER,
                    id,
                    remetenteEmail != null && !remetenteEmail.isBlank() ? remetenteEmail : "sistema@sti.chamados.com",
                    mensagem,
                    imagemUrl,
                    agora
            );

            jdbcTemplate.update("UPDATE chamados SET atualizado_em = ? WHERE id = ?", agora, id);
            notificarSse("mensagem-criada", salva);
            return ResponseEntity.status(HttpStatus.CREATED).body(salva);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Falha ao salvar mensagem", "detalhes", e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImagem(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Arquivo vazio."));
            }

            Files.createDirectories(uploadRootPath);
            String originalName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "imagem.png");
            String safeName = UUID.randomUUID() + "-" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path destino = uploadRootPath.resolve(safeName).normalize();

            if (!destino.startsWith(uploadRootPath)) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Caminho inválido."));
            }

            Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            String url = "/api/v1/chamados/arquivos/" + safeName;
            return ResponseEntity.ok(Map.of("url", url, "arquivo", safeName));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Falha ao salvar imagem no volume", "detalhes", e.getMessage()));
        }
    }

    @GetMapping("/arquivos/{nomeArquivo:.+}")
    public ResponseEntity<Resource> obterArquivo(@PathVariable String nomeArquivo) {
        try {
            Path arquivoPath = uploadRootPath.resolve(nomeArquivo).normalize();
            if (!arquivoPath.startsWith(uploadRootPath) || !Files.exists(arquivoPath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(arquivoPath.toUri());
            String contentType = Files.probeContentType(arquivoPath);
            return ResponseEntity.ok()
                    .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/feedbacks")
    public ResponseEntity<?> listarFeedbacks() {
        try {
            String sql = """
                    SELECT id, chamado_id, usuario_email, tipo, comentario, nota, criado_em
                    FROM feedbacks
                    ORDER BY criado_em DESC, id DESC
                    """;
            List<FeedbackDTO> lista = jdbcTemplate.query(sql, FEEDBACK_ROW_MAPPER);
            return ResponseEntity.ok(lista);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Falha ao listar feedbacks", "detalhes", e.getMessage()));
        }
    }

    @PostMapping("/feedbacks")
    public ResponseEntity<?> criarFeedback(@RequestBody Map<String, Object> req) {
        try {
            Long chamadoId = asNullableLong(req.get("chamadoId") != null ? req.get("chamadoId") : req.get("chamado_id"));
            String usuarioEmail = req.get("usuarioEmail") != null
                    ? asNullableString(req.get("usuarioEmail"))
                    : asNullableString(req.get("usuario_email"));
            String tipo = asNullableString(req.get("tipo"));
            String comentario = asNullableString(req.get("comentario"));
            Integer nota = asNullableInt(req.get("nota"));

            if (comentario == null || comentario.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Comentário é obrigatório."));
            }

            if (nota != null && (nota < 1 || nota > 5)) {
                nota = Math.max(1, Math.min(5, nota));
            }

            Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
            String sql = """
                    INSERT INTO feedbacks (chamado_id, usuario_email, tipo, comentario, nota, criado_em)
                    VALUES (?, ?, ?, ?, ?, ?)
                    RETURNING id, chamado_id, usuario_email, tipo, comentario, nota, criado_em
                    """;

            FeedbackDTO salvo = jdbcTemplate.queryForObject(
                    sql,
                    FEEDBACK_ROW_MAPPER,
                    chamadoId,
                    usuarioEmail != null && !usuarioEmail.isBlank() ? usuarioEmail : "usuario@sti.chamados.com",
                    tipo != null && !tipo.isBlank() ? tipo : "AVALIACAO",
                    comentario,
                    nota,
                    agora
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Falha ao salvar feedback", "detalhes", e.getMessage()));
        }
    }
}