# DATA-AUDIT — Microsserviço de Auditoria Imutável e Segurança Forense

O **DATA-AUDIT** é um microsserviço independente e resiliente concebido para prover rastreabilidade integral, conformidade de regras operacionais em tempo real e custódia forense de registos transacionais para o ecossistema do **STI (Sistema de Chamados)**.

A arquitetura implementa o padrão **Append-Only Ledger**: após persistido e assinado, nenhum evento transacional pode sofrer mutação (`UPDATE`) ou eliminação (`DELETE`), assegurando garantias criptográficas na camada de aplicação e bloqueios rígidos em nível de motor de base de dados.

---

## 1. Visão Arquitetural

```text
[ STI - Sistema de Chamados ]
              │
              │  HTTP REST / JSON
              ▼
┌────────────────────────────────────────────────────────┐
│               DATA-AUDIT (Spring Boot 3)               │
│                                                        │
│  • GlobalExceptionHandler (RFC 7807)                   │
│  • Bean Validation & Contratos DTO                     │
│  • Assinatura Canónica SHA-256                         │
│  • Motor de Regras Ativo (REG-001 a REG-004)           │
│  • AuditHikariPool (Max: 15 / Idle: 5 / Timeout: 20s)  │
└───────────────────────────┬────────────────────────────┘
                            │  JDBC / HikariCP
                            ▼
┌────────────────────────────────────────────────────────┐
│              PostgreSQL 16 (audit_schema)              │
│                                                        │
│  • tb_audit_event (Ledger imutável de eventos)         │
│  • tb_data_violation (Registo ativo de inconformidades)│
│  • Triggers PL/pgSQL (Bloqueio estrito UPDATE/DELETE)  │
│  • Índices Compostos B-Tree de Alta Performance        │
└────────────────────────────────────────────────────────┘
Componente,Especificação,Papel Arquitetural
Plataforma,Java 17 LTS / Spring Boot 3.x,Núcleo do microsserviço
Base de Dados,PostgreSQL 16,Repositório físico no schema audit_schema
Conexões,HikariCP (AuditHikariPool),Gestão dimensionada de ligações e resiliência
Criptografia,SHA-256 Canónico,Garantia de integridade e verificação forense
API Docs,OpenAPI 3.0 / Swagger UI,Catálogo e teste de endpoints
Infraestrutura,Docker & Docker Compose,Orquestração e isolamento dos serviços
3. Segurança Forense e Integridade
Assinatura Digital Determinística
A assinatura é apurada na ingestão através do encadeamento estrito dos campos canónicos:
Hash = SHA-256(origem + entidade + idEntidade + tipoOperacao + autor + dataHoraEvento + estadoAtual)
O endpoint GET /api/v1/audit/events/{id}/verificar-integridade reconstrói o hash em memória a partir dos dados gravados e afere o estado frente ao valor persistido original.
Proteção Ativa na Base de Dados (PL/pgSQL)
Triggers nativas impedem alterações mesmo sob credenciais administrativas:CREATE OR REPLACE FUNCTION audit_schema.trg_bloquear_mutacao_audit()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Operação violada: Registros de auditoria são estritamente imutáveis (Append-Only).';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_event_no_update
BEFORE UPDATE ON audit_schema.tb_audit_event
FOR EACH ROW EXECUTE FUNCTION audit_schema.trg_bloquear_mutacao_audit();

CREATE TRIGGER trg_audit_event_no_delete
BEFORE DELETE ON audit_schema.tb_audit_event
FOR EACH ROW EXECUTE FUNCTION audit_schema.trg_bloquear_mutacao_audit();
Código,Descrição da Regra,Severidade,Efeito no Sistema
REG-001,Encerramento de chamado sem parecer técnico preenchido.,ALTA,Gera notificação pendente em tb_data_violation.
REG-002,Salto irregular de fluxo (ex: direto de CRIADO para CONCLUIDO).,CRITICA,Interceta inconformidade e aciona revisão de processo.
REG-003,Inconsistência cronológica (data futura ou desvio de relógio).,MEDIA,Regista aviso de dessincronização temporal.
REG-004,Ação executada por perfil desprovido de privilégio adequado.,ALTA,Sinaliza suspeita de escalonamento indevido.
5. Dicionário de Endpoints REST
URL Base: http://localhost:8081/api/v1/audit

Ingestão de Evento
Rota: POST /events

Status Esperado: 201 Created

Corpo do Pedido:{
  "origem": "STI-CHAMADOS",
  "entidade": "TICKET",
  "idEntidade": "1001",
  "tipoOperacao": "FINALIZACAO",
  "autor": "tecnico.suporte",
  "dataHoraEvento": "2026-09-29T10:00:00",
  "estadoAnterior": "{\"status\": \"EM_ATENDIMENTO\"}",
  "estadoAtual": "{\"status\": \"FECHADO\", \"parecer\": \"Atendimento normalizado.\"}",
  "metadados": "{\"ipOrigem\": \"10.0.0.45\"}"
}
onsulta de Histórico e Linha do Tempo
Rota: GET /events/entidade/{entidade}/{idEntidade}

Status Esperado: 200 OK

Descrição: Obtém todos os eventos cronológicos de um recurso com base no índice composto idx_audit_event_entity_timeline.

Auditoria Forense
Rota: GET /events/{id}/verificar-integridade

Status Esperado: 200 OK

Exemplo de Resposta:{
  "idEvento": 1,
  "hashArmazenado": "4a5e1e07b8f1...",
  "hashRecalculado": "4a5e1e07b8f1...",
  "statusIntegridade": "INTEGRO"
}
Gestão de Violações
GET /violations/pendentes — Apresenta ocorrências ativas filtradas por idx_violation_status_data.

PUT /violations/{id}/resolver — Regista a tratativa e saneamento pelo gestor responsável.

6. Procedimento de Execução
Inicialização com Docker
Na pasta raiz do projeto:docker compose up -d --build
Endereços Disponíveis
Interface da API: http://localhost:8081

Documentação Swagger UI: http://localhost:8081/swagger-ui.html

PostgreSQL: localhost:5432 (Base de dados: audit_db)

7. Scripts Automatizados de Teste
Disponíveis no diretório scripts/ para execução no PowerShell:

Validação do Ciclo Completo (4 Personas):Get-Content .\scripts\integracao_sti_fluxo_completo.ps1 -Raw | iex
Teste de Carga e Benchmark:Get-Content .\scripts\teste_carga_benchmark.ps1 -Raw | iex
Indicador,Resultado Homologado,SLA de Projeto,Avaliação
Taxa de Sucesso (HTTP 201),100% (100 de 100),≥99.0%,Conforme
Falhas / Timeouts,0,0,Conforme
Tempo Total do Lote,5.51 s,≤10.0 s,Conforme
Vazão Média (Throughput),18.15 req/s,≥10.0 req/s,Conforme
Latência Média por Evento,54.15 ms,≤150.0 ms,Conforme
Latência Mínima,10.0 ms,—,Conforme
Latência Máxima,219.0 ms,≤500.0 ms,Conforme