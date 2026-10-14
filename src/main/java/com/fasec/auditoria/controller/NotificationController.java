package com.fasec.auditoria.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired(required = false)
    private JavaMailSender mailSender;

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
            if (mailSender != null) {
                enviarAsync(request.getDestinatario(), request.getAssunto(), request.getCorpo());
            } else {
                System.out.println("[AVISO] JavaMailSender não configurado. Apenas simulando.");
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
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("internalsystemsti@gmail.com");
            message.setTo(destinatario);
            message.setSubject(assunto);
            message.setText(corpo);

            mailSender.send(message);
            System.out.println("[SMTP REAL] E-mail enviado com sucesso para: " + destinatario);
        } catch (Exception e) {
            System.err.println("[ERRO SMTP] Falha ao enviar e-mail real para " + destinatario + ": " + e.getMessage());
        }
    }
}