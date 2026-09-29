package com.fasec.auditoria.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Subsistema de Auditoria de Dados e Conformidade")
                        .version("1.0.0")
                        .description("API REST para ingestão, rastreabilidade imutável e validação de conformidade de eventos de mutação oriundos do STI.")
                        .contact(new Contact()
                                .name("Carlos Daniel")
                                .url("https://github.com/")));
    }
}