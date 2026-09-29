# DATA-AUDIT — Microsserviço de Auditoria Imutável e Segurança Forense

O **DATA-AUDIT** é um microsserviço independente e resiliente concebido para assegurar rastreabilidade integral, conformidade de regras de negócio em tempo real e custódia forense de registro transacionais.

O sistema implementa o padrão **Append-Only Ledger**: os registros gravados e assinados não podem ser alterados (`UPDATE`) ou excluídos (`DELETE`), combinando assinaturas criptográficas determinísticas na aplicação com bloqueios físicos diretamente no banco de dados.

---

## 1. Visão Geral da Arquitetura

O sistema atua de forma autônoma através de uma interface REST desacoplada:

* **STI (Sistema de Chamados):** Envia os eventos transacionais em formato JSON.
* **DATA-AUDIT Engine:** Valida o contrato DTO, gera a |assinatura digital SHA-256 e executa as regras ativas de conformidade (REG-001 a REG-004).
* **AuditHikariPool:** Pool de conexões dimensionado para alta disponibilidade e baixa latência (Máx: 15, Mín: 5).
* **PostgreSQL 16 (audit_schema):** Custódia física dos dados em tabelas imutáveis protegidas por triggers PL/pgSQL nativas.

---

## 2. Tecnologias Utilizadas

* **Linguagem & Plataforma:** Java 17 LTS
* **Framework Principal:** Spring Boot 3.x (Spring Web, Spring Data JPA, Bean Validation)
* **Banco de Dados:** PostgreSQL 16 (Schema `audit_schema`)
* **Gestão de Conexões:** HikariCP (`AuditHikariPool`)
* **Documentação Interativa:** OpenAPI 3 / Swagger UI
* **Virtualização e Orquestração:** Docker e Docker Compose

---

## 3. Mecanismos de Integridade e Segurança Forense

### 3.1. Assinatura Criptográfica Forense (SHA-256)
No momento da ingestão do evento, o motor criptográfico gera uma assinatura canônica determinística calculada a partir dos dados do registro:

> **Hash** = SHA-256(`origem` + `entidade` + `idEntidade` + `tipoOperacao` + `autor` + `dataHoraEvento` + `estadoAtual`)

O endpoint `GET /api/v1/audit/events/{id}/verificar-integridade` recalcula dinamicamente a assinatura em menória a partir dos dados persistidos e compara-a com o hash gravado. Caso exista qualquer discrepância, o sistema alerta de imediato para a quebra de integridade física.

### 3.2. Bloqueio Físico no Banco de Dados (PL/pgSQL)
Triggers nativas no PostgreSQL impedem a execução de instruç��es de modificação ou remoção:

* **Função de Bloqueio:** `audit_schema.trg_bloquear_mutacao_audit()`
* **Gatilho de Update:** `trg_audit_event_no_update` (Bloqueia comandos `UPDATE`)
* **Gatilho de Delete:** `trg_audit_event_no_delete` (Bloqueia comandos `DELETE`)

---

## 4. Motor Ativo de Regras de Conformidade (Compliance Engine)

O sistema analisa ativamente as transações em tempo real durante a ingestão:

| Código | Descrição da Regra | Severidade | Efeito do Motor |
| :--- | :--- | :--- | :--- |
| **REG-001** | Encerramento de chamado sem parecer técnico de resolução preenchido. | `ALTA` | Gera ocorrência pendente em `tb_data_violation` para mediação gerencial. |
| **REG-002** | Salto irregular de fluxo (ex: transição direta de `CRIADO` para `CONCLUIDO`). | `CRÍTICA` | Intercepta quebra de processo e alerta a governança. |
| **REG-003** | Inconsistência temporal (data futura ou defasagem severa de horário). | `MÉDIA` | Sinaliza dessincronização cronológica do sistema cliente. |
| **REG-004** | Acão executada por perfil desprovido de autorização para o estágio. | `ALTA` | Sinaliza suspeita de escalonamento indevido de privilégios. |

---

## 5. Dicionário de Endpoints da API REST

URL Base da API: `http://localhost:8081/api/v1/audit`

| Método | Endpoint | Desccição | Status Sucesso |
| :--- | :--- | :--- | :--- |
| `POST` | `/events` | Ingestão de novos eventos transacionais (com hash e validação ativa) | `201 Created` |
| `GET` | `/events/entidade/{entidade}/{idEntidade}` | Linha do tempo cronológica dos eventos de um determinado recurso | `200 OK` |
| `GET` | `/events/{id}/verificar-integridade` | Verificação forense da assinatura SHA-256 contra adulterações | `200 OK` |
| `GET` | `/violations/pendentes` | Listagem de inconformidades operacionais abertas | `200 OK` |
| `PUT� | `/violations/{id}/resolver` | Registro de parecer técnico saneador e encerramento de inconformidade | `200 OK` |

---

## 6. Execução do Ambiente com Docker

### 6.1. Inicialização dos Contêneires
Na pasta raiz do projeto, execute:

```bash
docker compose up -d --build
```

### 6.2. Portas e Interfaces Disponiveis

| Serviço | Interface / Rota | Porta Host |
| :--- | :--- | :--- |
| **DATA-AUDIT API** | `http://localhost:8081` | `8FD1` |
| **Console Swagger UI** | `http://localhost:8081/swagger-ui.html` | `8081` |
| **Banco de Dados PostgreSQL** | Conexão JDBC / TCP | `5432` |

---

## 7. Scripts Automatizados de Teste

Disponíveis no diretório `scripts/`:

* **Validação Transacional Ponta a Ponta (4 Perfis):**
  Simula o ciclo completo cobrindo Cliente, Técnico, Gerente e Diretor:
  ```powershell
  Get-Content .\scripts\integracao_sti_fluxo_completo.ps1 -Raw | iex
  ```

* **Benchmark de Carga e Estresse:**
  Dispara um lote contínuo de 100 requisiç��es consecutivas avaliando vazão, latência e o pool HikariCP: 
  ```powershell
  Get-Content .\scripts\teste_carga_benchmark.ps1 -Raw | iex
  ```

---

## 8. Resultados do Benchmark de Performance (ATV-008)

Mřtricas coletadas sob teste de estresse transacional com escrita e hashing em tempo real:

| Indicador de Performance | Resultado Obtido | Meta de SLA | Situação |
| :--- | :--- | :--- | :--- |
| **Taxa de Sucesso (HTTP 201)** | **100% (100 de 100)** | >= 99.0% | Em Conformidade |
| **Falhas / Conexões Perdidas** | **0** | 0 | Em Conformidade |
| **Duração Total do Lote** | **5.51 s** | <= 10.0 s | Em Conformidade |
| **Throughput (Vazão)** | **18.15 req/s** | >= 10.0 req/s | Em Conformidade |
| **Latência Média por Evento** | **54.15 ms** | <= 150.0 ms | Em Conformidade |
| **Latência Mínima Observada** | **10.0 ms** | — | Em Conformidade |
| **Latência Máxima (Pico)** | **219.0 ms** | <= 500.0 ms | Em Conformidade |