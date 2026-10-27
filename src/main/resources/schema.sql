-- =====================================================================
-- SCRIPT DEFINITIVO - TODAS AS TABELAS DO SISTEMA DE TICKETS E AUDITORIA
-- =====================================================================

-- 1. DEPARTAMENTOS
CREATE TABLE IF NOT EXISTS departamentos (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    criado_em TIMESTAMP DEFAULT NOW()
);

-- 2. UTILIZADORES / CADASTROS
CREATE TABLE IF NOT EXISTS usuarios (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    email_pessoal VARCHAR(150),
    cpf VARCHAR(14) UNIQUE NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    perfil VARCHAR(50) DEFAULT 'SOLICITANTE',
    departamento_id INT REFERENCES departamentos(id) ON DELETE SET NULL,
    ativo BOOLEAN DEFAULT TRUE,
    criado_em TIMESTAMP DEFAULT NOW(),
    atualizado_em TIMESTAMP DEFAULT NOW()
);

-- 3. RECUPERAÇÃO DE SENHA (Tokens OTP via SMTP)
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id SERIAL PRIMARY KEY,
    usuario_id INT REFERENCES usuarios(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL,
    expiracao TIMESTAMP NOT NULL,
    usado BOOLEAN DEFAULT FALSE,
    criado_em TIMESTAMP DEFAULT NOW()
);

-- 4. CHAMADOS / TICKETS
CREATE TABLE IF NOT EXISTS chamados (
    id SERIAL PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    descricao TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'ABERTO', -- ABERTO, EM_ANDAMENTO, RESOLVIDO, FECHADO
    prioridade VARCHAR(30) DEFAULT 'MEDIA', -- BAIXA, MEDIA, ALTA, URGENTE
    solicitante_email VARCHAR(150) NOT NULL,
    tecnico_atribuido_email VARCHAR(150),
    departamento_id INT REFERENCES departamentos(id) ON DELETE SET NULL,
    criado_em TIMESTAMP DEFAULT NOW(),
    atualizado_em TIMESTAMP DEFAULT NOW()
);

-- 5. MENSAGENS E ANEXOS DO CHAT (Suporte a texto e imagens)
CREATE TABLE IF NOT EXISTS chamado_mensagens (
    id SERIAL PRIMARY KEY,
    chamado_id INT REFERENCES chamados(id) ON DELETE CASCADE,
    remetente_email VARCHAR(150) NOT NULL,
    mensagem TEXT,
    imagem_url VARCHAR(500), -- Link ou caminho do print/imagem enviado no chat
    criado_em TIMESTAMP DEFAULT NOW()
);

-- 6. FEEDBACKS (Melhorias e Críticas)
CREATE TABLE IF NOT EXISTS feedbacks (
    id SERIAL PRIMARY KEY,
    chamado_id INT REFERENCES chamados(id) ON DELETE SET NULL,
    usuario_email VARCHAR(150) NOT NULL,
    tipo VARCHAR(50) NOT NULL, -- 'MELHORIA' ou 'CRITICA'
    comentario TEXT NOT NULL,
    nota INT CHECK (nota >= 1 AND nota <= 5),
    criado_em TIMESTAMP DEFAULT NOW()
);

-- 7. AUDITORIA TEMPORÁRIA E HASHES
CREATE TABLE IF NOT EXISTS tb_audit_event (
    id SERIAL PRIMARY KEY,
    evento VARCHAR(255) NOT NULL,
    detalhes TEXT,
    hash_atual VARCHAR(255),
    hash_anterior VARCHAR(255),
    status_integridade VARCHAR(50), -- 'VALIDO', 'VIOLADO'
    criado_em TIMESTAMP DEFAULT NOW()
);

-- 8. VIOLAÇÕES DE DADOS
CREATE TABLE IF NOT EXISTS tb_data_violation (
    id SERIAL PRIMARY KEY,
    descricao_violation TEXT NOT NULL,
    dados_afetados TEXT,
    criado_em TIMESTAMP DEFAULT NOW()
);

-- 9. LOGS DE ATIVIDADE DO SISTEMA
CREATE TABLE IF NOT EXISTS tb_system_activity_logs (
    id SERIAL PRIMARY KEY,
    usuario_email VARCHAR(150),
    acao_realizada VARCHAR(255) NOT NULL,
    detalhes_ip_origem VARCHAR(50),
    criado_em TIMESTAMP DEFAULT NOW()
);