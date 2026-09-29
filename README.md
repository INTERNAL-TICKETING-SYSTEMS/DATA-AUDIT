# Subsistema de Auditoria de Dados e Conformidade (Data Audit Service)

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://www.docker.com/)
[![Swagger](https://img.shields.io/badge/Documentação-Swagger%20UI-85EA2D.svg)](http://localhost:8081/swagger-ui/index.html)

---

## 📌 Visão Geral do Projeto

O **Subsistema de Auditoria de Dados** é um microsserviço desacoplado desenvolvido no âmbito do Estágio Supervisionado Obrigatório (**FASEC** - Modalidade: *Projeto + Produto Computacional*). 

O sistema atua como uma camada independente de governança, integridade e conformidade para o **STI (Sistema de Tickets Interno)**, capturando mutações em entidades transacionais, assegurando trilhas imutáveis e preparando o ecossistema para detecção automatizada de anomalias operacionais.

---

## 🏛️ Arquitetura e Decisões Técnicas

┌─────────────────┐       HTTP / JSON (REST)       ┌──────────────────────────────┐
│                 │ ─────────────────────────────> │  Subsistema de Auditoria     │
│   STI (Tickets) │                                │  (Spring Boot 3 + Java 21)   │
│                 │ <───────────────────────────── │  Porta: 8081                 │
└─────────────────┘       201 Created (Hash)       └──────────────┬───────────────┘
│
Persistência (JPA)
│
▼
┌──────────────────────────────┐
│   PostgreSQL 16 (audit_db)   │
│   • Schema: audit_schema     │
│   • Triggers: Append-Only    │
└──────────────────────────────┘

### Pilares de Engenharia Implementados:
- **Imutabilidade Estrita (Append-Only):** Regra de negócio aplicada via *Trigger* nativa no PostgreSQL (`audit_schema.fn_bloquear_mutacao_auditoria`), bloqueando operações de `UPDATE` e `DELETE` nos registros de log.
- **Auditoria Forense (SHA-256):** Cada evento persistido calcula uma assinatura criptográfica baseada em seus atributos de estado, permitindo validação matemática contra adulterações na base de dados.
- **Desempenho e Paginação:** Endpoints de leitura utilizam paginação via `Pageable` do Spring Data JPA, prevenindo sobrecarga de memória em consultas com alto volume de registros.
- **Resiliência e RFC 7807:** Interceptador global de exceções (`GlobalExceptionHandler`) com respostas estruturadas para requisições com dados inconsistentes (HTTP 400).

---

## 🚀 Como Executar o Ambiente

### Pré-requisitos:
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado e ativo.
- Git.

### Passo a passo:

1. Clone o repositório:
```bash
git clone [https://github.com/SEU_USUARIO/DATA-AUDIT.git](https://github.com/SEU_USUARIO/DATA-AUDIT.git)
cd DATA-AUDIT