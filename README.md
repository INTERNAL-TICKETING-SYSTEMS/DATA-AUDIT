# DATA-AUDIT — Microsserviço de Auditoria Imutável e Segurança Forense

O **DATA-AUDIT** é um microsserviço independente e resiliente concebido para prover rastreabilidade integral, conformidade de regras de negócio em tempo real e custódia forense de registros transacionais. 

O sistema implementa o padrão **Append-Only Ledger**: registros gravados e assinados não podem ser alterados (`UPDATE`) ou excluídos (`DELETE`), combinando assinaturas criptográficas determinísticas na aplicação com travas físicas no banco de dados.

---

## 1. Visão Geral da Arquitetura

```text
                          [ SISTEMA CLIENTE / CONSUMIDOR (STI) ]
                                            │
                                            │ Requisições HTTP REST (JSON)
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        DATA-AUDIT ENGINE (Spring Boot 3 / Java 17)                     │
│                                                                                        │
│  ┌─────────────────────────┐  ┌────────────────────────┐  ┌─────────────────────────┐  │
│  │  GlobalExceptionHandler │  │  Validação de Contrato │  │     HikariCP Tuning     │  │
│  │  (Padronização RFC-7807)│  │     (Bean Validation)  │  │  (AuditHikariPool - 15) │  │
│  └───────────┬─────────────┘  └───────────┬────────────┘  └────────────┬────────────┘  │
│              └────────────────────────────┼────────────────────────────┘               │
│                                           ▼                                            │
│                 ┌──────────────────────────────────────────────────┐                   │
│                 │   Motor Criptográfico: SHA-256 Determinístico    │                   │
│                 └─────────────────────────┬────────────────────────┘                   │
│                                           ▼                                            │
│                 ┌──────────────────────────────────────────────────┐                   │
│                 │ Motor Ativo de Conformidade: Regras REG-001..004 │                   │
│                 └─────────────────────────┬────────────────────────┘                   │
└───────────────────────────────────────────┼────────────────────────────────────────────┘
                                            │ Conexão TCP / JDBC
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                      POSTGRESQL 16 (Schema Isolado: audit_schema)                      │
│                                                                                        │
│  ┌────────────────────────────────────────┐  ┌──────────────────────────────────────┐  │
│  │            tb_audit_event              │  │          tb_data_violation           │  │
│  │   (Custódia imutável de transações)    │  │ (Alertas de quebra de conformidade)  │  │
│  └──────────────────┬─────────────────────┘  └──────────────────┬───────────────────┘  │
│                     │                                           │                      │
│     Triggers Nativas (PL/pgSQL)                      Índices Otimizados B-Tree         │
│     [Bloqueio físico de UPDATE e DELETE]             [Status/Data e Entidade/Data]     │
└────────────────────────────────────────────────────────────────────────────────────────┘
2. Tecnologias Utilizadas
Linguagem: Java 17 (LTS)

Framework: Spring Boot 3.x (Spring Web, Spring Data JPA, Bean Validation)

Banco de Dados: PostgreSQL 16 (Schema audit_schema)

Pool de Conexões: HikariCP (AuditHikariPool customizado)

Documentação Viva: OpenAPI 3 / Swagger UI

Contêineres: Docker e Docker Compose

Versionamento: Git e GitHub Projects

3. Mecanismos de Integridade e Segurança Forense
3.1. Assinatura Criptográfica Forense (SHA-256)
No momento da ingestão do evento, o sistema calcula uma assinatura canônica determinística a partir da concatenação dos dados essenciais:
Hash = SHA-256(origem + entidade + idEntidade + tipoOperacao + autor + dataHoraEvento + estadoAtual)
O endpoint GET /api/v1/audit/events/{id}/verificar-integridade reconstrói a assinatura a partir dos dados do banco e confronta com o hash persistido. Se qualquer caractere for modificado, a quebra de integridade é detectada imediatamente.

3.2. Travas Físicas em Nível de Banco (PL/pgSQL)
Triggers nativas no PostgreSQL bloqueiam qualquer tentativa direta de modificação ou exclusão de eventos:
CREATE OR REPLACE FUNCTION audit_schema.trg_bloquear_mutacao_audit()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Operacao violada: Registros de auditoria sao estritamente imutaveis (Append-Only).';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_event_no_update
BEFORE UPDATE ON audit_schema.tb_audit_event
FOR EACH ROW EXECUTE FUNCTION audit_schema.trg_bloquear_mutacao_audit();

CREATE TRIGGER trg_audit_event_no_delete
BEFORE DELETE ON audit_schema.tb_audit_event
FOR EACH ROW EXECUTE FUNCTION audit_schema.trg_bloquear_mutacao_audit();
Código,Descrição da Regra,Severidade,Efeito do Motor
REG-001,Encerramento de chamado sem parecer técnico de resolução preenchido.,ALTA,Gera ocorrência pendente em tb_data_violation para mediação gerencial.
REG-002,Salto irregular de fluxo (ex: transição direta de CRIADO para CONCLUIDO).,CRITICA,Intercepta quebra de processo e alerta a governança.
REG-003,Inconsistência temporal (data futura ou defasagem severa de horário).,MEDIA,Sinaliza dessincronização cronológica de sistema cliente.
REG-004,Ação executada por perfil desprovido de autorização para o estágio.,ALTA,Sinaliza suspeita de escalonamento indevido de privilégios.
5. Dicionário de Endpoints da API REST
URL Base: http://localhost:8081/api/v1/audit

5.1. Ingestão de Evento de Auditoria
Rota: POST /events

Status: 201 Created

Exemplo de Envio:
{
  "origem": "STI-CHAMADOS",
  "entidade": "TICKET",
  "idEntidade": "8841",
  "tipoOperacao": "FINALIZACAO",
  "autor": "tecnico.silva",
  "dataHoraEvento": "2026-09-29T10:15:30",
  "estadoAnterior": "{\"status\": \"EM_ATENDIMENTO\"}",
  "estadoAtual": "{\"status\": \"ENCERRADO\", \"parecer\": \"Troca de switch efetuada com sucesso.\"}",
  "metadados": "{\"ip\": \"192.168.1.120\", \"terminal\": \"TER-04\"}"
}
5.2. Linha do Tempo do Recurso (Timeline)
Rota: GET /events/entidade/{entidade}/{idEntidade}

Status: 200 OK

Descrição: Resgata o histórico auditado de um ticket ordenado de forma decrescente pela data do evento.

5.3. Verificação Forense de Integridade
Rota: GET /events/{id}/verificar-integridade

Status: 200 OK

Exemplo de Retorno:
{
  "idEvento": 1,
  "hashArmazenado": "4a5e1e07b8f1...",
  "hashRecalculado": "4a5e1e07b8f1...",
  "statusIntegridade": "INTEGRO"
}
5.4. Gestão de Violações
GET /violations/pendentes — Lista inconformidades operacionais pendentes de análise.

PUT /violations/{id}/resolver — Registra o despacho saneador do gestor.

5.5. Respostas de Erro Padronizadas (RFC 7807)
Erros de validação retornam status 400 Bad Request detalhando cada campo sem expor stack traces da JVM:
{
  "timestamp": "2026-09-29T14:47:00.202",
  "status": 400,
  "erro": "Erro de Validação de Dados",
  "detalhes": {
    "origem": "Origem é obrigatória",
    "tipoOperacao": "Tipo de Operação é obrigatório"
  }
}
6. Configuração e Execução do Ambiente
6.1. Pré-requisitos
Docker e Docker Compose instalados.

PowerShell ou Bash.

6.2. Inicialização dos Contêineres
Na pasta raiz do projeto:
docker compose up -d --build
6.3. Portas e AcessosServiçoEndereçoPorta HostDATA-AUDIT APIhttp://localhost:80818081Swagger UIhttp://localhost:8081/swagger-ui.html8081PostgreSQLlocalhost:543254327. Scripts Automatizados de TesteO repositório inclui rotinas automatizadas no diretório scripts/:7.1. Validação do Fluxo Ponta a Ponta (4 Personas)Simula o ciclo operacional completo (Cliente, Técnico, Gerente e Diretor):
Get-Content .\scripts\integracao_sti_fluxo_completo.ps1 -Raw | iex
7.2. Benchmark de Carga e Estresse
Dispara 100 requisições consecutivas avaliando vazão, latência e o pool HikariCP:
Get-Content .\scripts\teste_carga_benchmark.ps1 -Raw | iex
8. Resultados do Benchmark de Performance (ATV-008)Métricas coletadas sob teste de estresse de escrita em lote:Indicador de PerformanceResultado ObtidoMeta de SLASituaçãoTaxa de Sucesso (HTTP 201)100% (100 de 100)$\ge 99.0\%$Em ConformidadeFalhas / Conexões Perdidas0$0$Em ConformidadeDuração Total do Lote5.51 s$\le 10.0\text{ s}$Em ConformidadeVazão (Throughput)18.15 req/s$\ge 10.0\text{ req/s}$Em ConformidadeLatência Média por Evento54.15 ms$\le 150.0\text{ ms}$Em ConformidadeLatência Mínima Observada10.0 ms—Em ConformidadeLatência Máxima (Pico)219.0 ms$\le 500.0\text{ ms}$Em Conformidade