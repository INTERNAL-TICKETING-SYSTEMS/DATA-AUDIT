DATA-AUDIT — Microsserviço de Auditoria Imutável e Segurança Forense
O DATA-AUDIT é um microsserviço independente e resiliente concebido para assegurar rastreabilidade integral, conformidade de regras de negócio em tempo real e custódia forense de registos transacionais.

O sistema implementa o padrão Append-Only Ledger: os registos gravados e assinados não podem ser alterados (UPDATE) ou eliminados (DELETE), combinando assinaturas criptográficas determinísticas na aplicação com bloqueios físicos diretamente na base de dados.

1. Visão Geral da Arquitetura
O sistema atua de forma autónoma através de uma interface REST desacoplada:

STI (Sistema de Chamados): Envia os eventos transacionais em formato JSON.

DATA-AUDIT Engine: Valida o contrato DTO, gera a assinatura digital SHA-256 e executa as regras ativas de conformidade (REG-001 a REG-004).

AuditHikariPool: Pool de ligações afinado para alta disponibilidade e baixa latência.

PostgreSQL 16 (audit_schema): Custódia física dos dados em tabelas imutáveis protegidas por triggers PL/pgSQL nativas.

2. Tecnologias Utilizadas
Linguagem & Plataforma: Java 17 LTS

Framework Principal: Spring Boot 3.x (Spring Web, Spring Data JPA, Bean Validation)

Base de Dados: PostgreSQL 16 (Schema audit_schema)

Gestão de Ligações: HikariCP (AuditHikariPool)

Documentação Interativa: OpenAPI 3 / Swagger UI

Virtualização e Orquestração: Docker e Docker Compose

3. Mecanismos de Integridade e Segurança Forense
3.1. Assinatura Criptográfica Forense (SHA-256)
No momento da ingestão do evento, o motor criptográfico gera uma assinatura canónica determinística calculada a partir dos dados do registo:

Hash = SHA-256(origem + entidade + idEntidade + tipoOperacao + autor + dataHoraEvento + estadoAtual)

O endpoint de verificação recalcula a assinatura em memória a partir dos dados persistidos e compara com o hash gravado. Caso exista divergência, o sistema alerta de imediato para a quebra de integridade.

3.2. Bloqueio Físico na Base de Dados (PL/pgSQL)
Triggers nativas no PostgreSQL impedem a execução de instruções de modificação ou remoção:

Função de Bloqueio: audit_schema.trg_bloquear_mutacao_audit()

Gatilho de Update: trg_audit_event_no_update (Bloqueia comandos UPDATE)

Gatilho de Delete: trg_audit_event_no_delete (Bloqueia comandos DELETE)

4. Motor Ativo de Regras de Conformidade
O sistema analisa ativamente as transações em tempo real:

REG-001 (Severidade Alta): Encerramento de chamado sem parecer técnico de resolução preenchido.

REG-002 (Severidade Crítica): Transição ilegal de estados do ciclo de vida.

REG-003 (Severidade Média): Inconsistência temporal e dessincronização cronológica.

REG-004 (Severidade Alta): Operação executada por perfil sem autorização atribuída.

5. Dicionário de Endpoints da API REST
URL Base: http://localhost:8081/api/v1/audit

POST /events — Ingestão de novos eventos transacionais (Retorna 201 Created).

GET /events/entidade/{entidade}/{idEntidade} — Linha do tempo cronológica da entidade.

GET /events/{id}/verificar-integridade — Verificação forense de integridade da assinatura.

GET /violations/pendentes — Listagem de violações de conformidade em aberto.

PUT /violations/{id}/resolver — Registo de despacho e encerramento de inconformidade.

6. Execução do Ambiente com Docker
Inicialização dos Serviços
Na raiz do projeto, execute:

docker compose up -d --build

Portas e Interfaces Disponíveis
API de Auditoria: http://localhost:8081

Consola Swagger UI: http://localhost:8081/swagger-ui.html

Ligação PostgreSQL: Porta 5432 (Base de dados: audit_db)

7. Scripts Automatizados de Teste
Disponíveis no diretório scripts/:

Validação Transacional Ponta a Ponta (4 Perfis):
Get-Content .\scripts\integracao_sti_fluxo_completo.ps1 -Raw | iex

Benchmark de Carga e Estresse:
Get-Content .\scripts\teste_carga_benchmark.ps1 -Raw | iex

8. Resultados do Teste de Carga e Confiabilidade (ATV-008)
Resultados homologados sob bateria contínua de escrita transacional:

Taxa de Sucesso: 100% (100 de 100 requisições)

Falhas ou Perda de Ligação: 0

Tempo Total de Processamento: 5.51 segundos

Throughput: 18.15 requisições por segundo

Latência Média: 54.15 ms

Latência Mínima: 10 ms

Latência Máxima: 219 ms