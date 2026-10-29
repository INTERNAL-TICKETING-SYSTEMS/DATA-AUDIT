package com.fasec.auditoria.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Value("${brevo.api.key:xkeysib-ac76be0a78505773bf7b559870a6c9195db276d66cba9f78727b49c}")
    private String brevoApiKey;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    public static class EmailRequest {
        private String destinatario;
        private String assunto;
        private String corpo;

        public String getDestinatario() { return destinatario; }
        public void setDestinatario(String destinatario) { this.destinatario = destinatario; }

        public String getAssunto() { return assunto; }
        public void setAssunto(String assunto) { this.assunto = assunto; }

        public String getCorpo() { return corpo; }
        public void setCorpo(String corpo) { this.corpo = corpo; }
    }

    @PostMapping("/email")
    public ResponseEntity<?> enviarEmail(@RequestBody EmailRequest request) {
        try {
            // Dispara o envio de forma assíncrona para não prender a resposta HTTP
            if (request.getDestinatario() != null && !request.getDestinatario().isEmpty()) {
                enviarAsync(request.getDestinatario(), request.getAssunto(), request.getCorpo());
            } else {
                System.out.println("[AVISO] Destinatario de e-mail vazio.");
            }

            return ResponseEntity.ok().body("Pedido de e-mail aceite e em processamento!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao processar e-mail: " + e.getMessage());
        }
    }

    @Async
    public void enviarAsync(String destinatario, String assunto, String corpo) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("name", "Data Audit FASEC", "email", "internalsystemsti@gmail.com"));
            body.put("to", List.of(Map.of("email", destinatario)));
            body.put("subject", assunto != null ? assunto : "Recuperação de Senha - Data Audit");
            body.put("htmlContent", "<p>" + (corpo != null ? corpo : "") + "</p>");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("[BREVO REAL] E-mail enviado com sucesso para: " + destinatario);
            } else {
                System.err.println("[ERRO BREVO] Falha ao enviar e-mail real para " + destinatario + ": " + response.getBody());
            }
        } catch (Exception e) {
            System.err.println("[ERRO BREVO] Exceção ao conectar à API do Brevo para " + destinatario + ": " + e.getMessage());
        }
    }
}