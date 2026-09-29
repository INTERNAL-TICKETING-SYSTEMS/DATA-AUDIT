# DATA-AUDIT — Microsserviço de Auditoria Imutável e Segurança Forense

[![Java](https://img.shields.io/badge/Java-17%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)](#)

O **DATA-AUDIT** é um microsserviço independente e resiliente concebido para prover rastreabilidade integral, compliance de regras de negócio em tempo real e custódia forense de registros transacionais. 

O sistema foi arquitetado como uma camada append-only desacoplada, servindo como motor de integridade e custódia para sistemas corporativos de missão crítica — operando de forma nativa e integrada ao ecossistema do **STI (Sistema de Chamados)**.

---

## 1. Visão Geral e Arquitetura

O sistema adota o padrão de projeto **Append-Only Ledger**: uma vez que um registro é aceito, persistido e assinado, ele nunca mais poderá sofrer alterações (`UPDATE`) ou deleções (`DELETE`). 

A auditoria opera de forma **ativa**: além de custodiar os dados, inspeciona o estado da transação em tempo de escrita, levantando inconformidades operacionais imediatamente.

                      [ SISTEMA CLIENTE / CONSUMIDOR (STI) ]
                                        │
                                        │ Requisições HTTP REST (JSON)
                                        ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐│                        DATA-AUDIT ENGINE (Spring Boot 3 / Java 17)                     ││                                                                                        ││  ┌─────────────────────────┐  ┌────────────────────────┐  ┌─────────────────────────┐  ││  │  GlobalExceptionHandler │  │  Validação de Contrato │  │     HikariCP Tuning     │  ││  │  (Padronização RFC-7807)│  │     (Bean Validation)  │  │  (AuditHikariPool - 15) │  ││  └───────────┬─────────────┘  └───────────┬────────────┘  └────────────┬────────────┘  ││              └────────────────────────────┼────────────────────────────┘               ││                                           ▼                                            ││                 ┌──────────────────────────────────────────────────┐                   ││                 │   Motor Criptográfico: SHA-256 Determinístico    │                   ││                 └─────────────────────────┬────────────────────────┘                   ││                                           ▼                                            ││                 ┌──────────────────────────────────────────────────┐                   ││                 │ Motor Ativo de Conformidade: Regras REG-001..004 │                   ││                 └─────────────────────────┬────────────────────────┘                   │└───────────────────────────────────────────┼────────────────────────────────────────────┘│ Conexão TCP / JDBC▼┌────────────────────────────────────────────────────────────────────────────────────────┐│                      POSTGRESQL 16 (Schema Isolado: audit_schema)                      ││                                                                                        ││  ┌────────────────────────────────────────┐  ┌──────────────────────────────────────┐  ││  │            tb_audit_event              │  │          tb_data_violation           │  ││  │   (Custódia imutável de transações)    │  │ (Alertas de quebra de conformidade)  │  ││  └──────────────────┬─────────────────────┘  └──────────────────┬───────────────────┘  ││                     │                                           │                      ││     Triggers Nativas (PL/pgSQL)                      Índices Otimizados B-Tree         ││     [Bloqueio físico de UPDATE e DELETE]             [Status/Data e Entidade/Data]     │└────────────────────────────────────────────────────────────────────────────────────────┘
---

## 2. Tecnologias Utilizadas

* **Linguagem:** Java 17 (Long-Term Support)
* **Framework:** Spring Boot 3.x (Spring Web, Spring Data JPA, Bean Validation)
* **Banco de Dados:** PostgreSQL 16
* **Pool de Conexões:** HikariCP (`AuditHikariPool` com limites de resiliência configurados)
* **Documentação de API:** OpenAPI 3 / Swagger UI
* **Contêineres:** Docker e Docker Compose
* **Versionamento de Código:** Git e GitHub com esteira de branches e issues rastreadas

---

## 3. Mecanismos de Segurança e Imutabilidade

A garantia de integridade do DATA-AUDIT atua em duas barreiras complementares:

### Barreira 1: Assinatura Criptográfica Forense (SHA-256)
No momento da ingestão do evento, o sistema calcula uma assinatura canônica determinística a partir da concatenação dos dados essenciais:

$$\text{Hash} = \text{SHA-256}(\text{origem} + \text{entidade} + \text{idEntidade} + \text{tipoOperacao} + \text{autor} + \text{dataHoraEvento} + \text{estadoAtual})$$

O endpoint `GET /api/v1/audit/events/{id}/verificar-integridade` reconstrói a assinatura a partir dos dados do banco e confronta com o hash persistido. Se qualquer caractere for modificado, a quebra de integridade é imediatamente detectada.

### Barreira 2: Travas Físicas em Nível de Banco (PL/pgSQL)
Mesmo que um operador ou invasor possua credenciais diretas no PostgreSQL, as seguintes triggers impedem alterações no schema de auditoria:

```sql
CREATE OR REPLACE FUNCTION audit_schema.trg_bloquear_mutacao_audit()
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
4. Motor Ativo de Regras de Conformidade (Compliance Engine)Diferente de sistemas de log tradicionais, o DATA-AUDIT analisa ativamente os dados no momento do registro:CódigoDescrição da RegraSeveridadeEfeito do MotorREG-001Encerramento de chamado sem parecer técnico de resolução preenchido.ALTAGera ocorrência pendente em tb_data_violation para mediação gerencial.REG-002Salto irregular de fluxo (ex: transição direta de CRIADO para CONCLUIDO).CRITICAIntercepta quebra de processo e alerta a governança.REG-003Inconsistência temporal (data futura ou defasagem severa de horário).MEDIASinaliza dessincronização cronológica de sistema cliente.REG-004Ação executada por perfil desprovido de autorização para o estágio.ALTASinaliza suspeita de escalonamento indevido de privilégios.5. Dicionário de Endpoints da API RESTURL Base da API: http://localhost:8081/api/v1/audit5.1. Ingestão de Evento de AuditoriaRota: POST /eventsStatus de Sucesso: 201 CreatedExemplo de Payload:JSON{
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
5.2. Linha do Tempo do Recurso (Timeline)Rota: GET /events/entidade/{entidade}/{idEntidade}Status: 200 OKUso: Resgata o histórico auditado de um ticket ou recurso com ordenação cronológica decrescente.5.3. Verificação Forense de IntegridadeRota: GET /events/{id}/verificar-integridadeStatus: 200 OKExemplo de Retorno:JSON{
  "idEvento": 1,
  "hashArmazenado": "4a5e1e...b8f1",
  "hashRecalculado": "4a5e1e...b8f1",
  "statusIntegridade": "INTEGRO"
}
5.4. Gestão de ViolaçõesGET /violations/pendentes — Lista inconformidades operacionais abertas.PUT /violations/{id}/resolver — Registra a tratativa do gestor e resolve a anomalia.5.5. Respostas Padronizadas de Erro (RFC 7807)Erros de validação retornam HTTP 400 com detalhes sem expor a pilha de execução (stack trace):JSON{
  "timestamp": "2026-09-29T14:47:00.202",
  "status": 400,
  "erro": "Erro de Validação de Dados",
  "detalhes": {
    "origem": "Origem é obrigatória",
    "tipoOperacao": "Tipo de Operação é obrigatório"
  }
}
6. Configuração e Execução do Ambiente6.1. Pré-requisitosDocker e Docker ComposeTerminal PowerShell ou Bash6.2. Inicialização dos ContêineresNa pasta raiz do projeto:Bashdocker compose up -d --build
6.3. Portas e ServiçosServiçoInterface / RotaPorta HostDATA-AUDIT APIhttp://localhost:80818081Documentação Swaggerhttp://localhost:8081/swagger-ui.html8081Banco PostgreSQLJDBC / TCP54327. Scripts Automatizados de TesteO projeto possui scripts automatizados em PowerShell dentro da pasta scripts/:7.1. Validação Ponta a Ponta das 4 PersonasSimula a jornada transacional completa (Cliente abrindo, Técnico resolvendo, Gerente tratando anomalia e Diretor auditando a integridade):PowerShellGet-Content .\scripts\integracao_sti_fluxo_completo.ps1 -Raw | iex
7.2. Benchmark de Carga e EstresseDispara um lote contínuo de 100 requisições calculando latência e estabilidade:PowerShellGet-Content .\scripts\teste_carga_benchmark.ps1 -Raw | iex
8. Resultados do Benchmark de Performance (ATV-008)Resultados coletados no teste de estresse transacional com escrita e hashing em tempo real:Indicador de PerformanceResultado ObtidoSLA EstabelecidoSituaçãoDisponibilidade / Taxa de Sucesso100% (100 de 100)$\ge 99.0\%$Em ConformidadeFalhas / Conexões Perdidas0$0$Em ConformidadeDuração Total do Lote5.51 s$\le 10.0\text{ s}$Em ConformidadeTaxa de Transferência (Throughput)18.15 req/s$\ge 10.0\text{ req/s}$Em ConformidadeLatência Média por Evento54.15 ms$\le 150.0\text{ ms}$Em ConformidadeLatência Mínima Observada10.0 ms—Em ConformidadeLatência Máxima (Pico)219.0 ms$\le 500.0\text{ ms}$Em Conformidade