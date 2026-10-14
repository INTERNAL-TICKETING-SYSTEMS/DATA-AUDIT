-- Extensões necessárias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Tabela de Departamentos / Setores do DETRAN-TO (Com suporte a Blocos)
CREATE TABLE IF NOT EXISTS departamentos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sigla VARCHAR(30) NOT NULL UNIQUE,
    nome VARCHAR(150) NOT NULL,
    bloco VARCHAR(50),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Tabela de Utilizadores (Integrada com o Frontend e Contas Especiais)
CREATE TABLE IF NOT EXISTS usuarios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    email_pessoal VARCHAR(180),
    cpf VARCHAR(14) UNIQUE,
    matricula VARCHAR(50) UNIQUE,
    data_nascimento DATE,
    genero VARCHAR(30),
    senha_hash VARCHAR(255) NOT NULL,
    telefone_whatsapp VARCHAR(20) NOT NULL,
    perfil VARCHAR(30) NOT NULL DEFAULT 'SOLICITANTE' CHECK (perfil IN ('ADMIN', 'TECNICO', 'GESTOR', 'SOLICITANTE', 'usuario', 'tecnico', 'gestor')),
    departamento_id UUID REFERENCES departamentos(id) ON DELETE SET NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    atualizado_em TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. Tabela de Recuperações OTP (E-mail / Senha)
CREATE TABLE IF NOT EXISTS recuperacoes_otp (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID REFERENCES usuarios(id) ON DELETE CASCADE,
    codigo VARCHAR(6) NOT NULL,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. Tabela de Ativos de TI (Inventário)
CREATE TABLE IF NOT EXISTS ativos_ti (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patrimonio VARCHAR(50) NOT NULL UNIQUE,
    tipo VARCHAR(50) NOT NULL,
    descricao TEXT,
    usuario_responsavel_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    departamento_id UUID REFERENCES departamentos(id) ON DELETE SET NULL,
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 5. Tabela Central de Chamados (Helpdesk STI)
CREATE TABLE IF NOT EXISTS chamados (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    protocolo VARCHAR(50) UNIQUE,
    titulo VARCHAR(200) NOT NULL,
    descricao TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'aberto',
    prioridade VARCHAR(20) NOT NULL DEFAULT 'media',
    solicitante_id UUID REFERENCES usuarios(id) ON DELETE RESTRICT,
    tecnico_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    departamento_id UUID REFERENCES departamentos(id) ON DELETE SET NULL,
    solucao TEXT,
    patrimonio_ativo VARCHAR(50),
    pecas_trocadas TEXT,
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    atualizado_em TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 6. Tabela de Interações / Chat dos Chamados
CREATE TABLE IF NOT EXISTS chamados_interacoes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    chamado_id UUID REFERENCES chamados(id) ON DELETE CASCADE,
    autor_id UUID REFERENCES usuarios(id) ON DELETE RESTRICT,
    mensagem TEXT NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 7. Tabela de Logs de Auditoria Imutável (Data-Audit)
CREATE TABLE IF NOT EXISTS auditoria_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    entidade VARCHAR(100) NOT NULL,
    id_entidade VARCHAR(100),
    tipo_operacao VARCHAR(50) NOT NULL,
    autor VARCHAR(180),
    estado_anterior JSONB,
    estado_atual JSONB,
    metadados JSONB,
    ip_origem VARCHAR(45),
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ==========================================
-- SEEDS: SETORES OFICIAIS DO DETRAN-TO (SETORES_DETRAN)
-- ==========================================
INSERT INTO departamentos (sigla, nome, bloco) VALUES 
-- BLOCO 4
('PROTO', 'Protocolo', 'Bloco 4'),
('TRANSP', 'Transporte', 'Bloco 4'),
('ALMOX', 'Almoxarifado', 'Bloco 4'),
('ADM', 'Administração', 'Bloco 4'),
('FIN', 'Financeiro', 'Bloco 4'),
('PLAN', 'Planejamento', 'Bloco 4'),
('DIRFIN', 'Diretor Financeiro', 'Bloco 4'),
('DIRADM', 'Diretor Administrativo', 'Bloco 4'),
('CONTL', 'Contratos e Licitações', 'Bloco 4'),
('RESSARC', 'Ressarcimento', 'Bloco 4'),
('PATR', 'Patrimônio', 'Bloco 4'),

-- BLOCO 3
('AT-CNH', 'Atendimento CNH', 'Bloco 3'),
('INST-ID', 'Instituto de Identificação', 'Bloco 3'),
('CONF-CNH', 'Conferência CNH', 'Bloco 3'),
('JMED', 'Junta Médica', 'Bloco 3'),
('ACNH', 'Administrativo CNH', 'Bloco 3'),
('GER-CNH', 'Gerência CNH', 'Bloco 3'),
('REC-CNH', 'Recepção CNH', 'Bloco 3'),

-- BLOCO 2
('REC-VEI', 'Recepção Veículos', 'Bloco 2'),
('AT-VEI', 'Atendimento Veículos', 'Bloco 2'),
('DESP-VEI', 'Despachantes Veículos', 'Bloco 2'),
('AP-JUR', 'Apoio Jurídico', 'Bloco 2'),
('DOPER', 'DOPER', 'Bloco 2'),
('ASTEC', 'ASTEC', 'Bloco 2'),
('DIPAC', 'Diretoria de Postos de Atendimento das CIRETRANS', 'Bloco 2'),
('GER-VEI', 'Gerência de Veículos', 'Bloco 2'),
('DOPER-SET', 'Diretoria de Operações', 'Bloco 2'),

-- BLOCO 1
('GGP', 'Gerência de Gestão de Pessoas', 'Bloco 1'),
('GSRN', 'Gerência de Sistemas de Registros Nacionais', 'Bloco 1'),
('RENAVAM', 'RENAVAM', 'Bloco 1'),
('MULTAS', 'Multas', 'Bloco 1'),
('RENAINF', 'RENAINF', 'Bloco 1'),
('RENACH', 'RENACH', 'Bloco 1'),
('OUVID', 'Ouvidoria', 'Bloco 1'),
('CALL', 'Call Center', 'Bloco 1'),
('NUC-INT', 'Núcleo de Inteligência', 'Bloco 1'),
('AP-CIR', 'Apoio CIRETRANS', 'Bloco 1'),
('GER-CIR', 'Gerência de CIRETRANS', 'Bloco 1'),
('DICPA', 'Diretoria de CIRETRANS e Postos de Atendimento', 'Bloco 1'),
('DTEC', 'Diretoria Técnica', 'Bloco 1'),

-- ANEXO
('CETRAN', 'CETRAN', 'Anexo'),
('JARI', 'JARI', 'Anexo'),
('ESTAT', 'Estatísticas', 'Anexo'),
('DEF-AUT', 'Defesa e Autuação', 'Anexo'),
('CRED', 'Credenciamento', 'Anexo'),
('GER-CRED', 'Gerência de Credenciamento', 'Anexo'),
('CORREG', 'Corregedoria', 'Anexo'),
('ENG-TRAF', 'Engenharia de Tráfego', 'Anexo'),
('ED-TRANS', 'Educação para o Trânsito', 'Anexo'),
('LEILAO', 'Leilão', 'Anexo'),
('FISC', 'Fiscalização', 'Anexo'),
('BEXAM', 'Banca Examinadores', 'Anexo'),
('SALA-PROV', 'Sala de Provas', 'Anexo'),
('REC-BAN', 'Recepção Bancas', 'Anexo'),

-- BLOCO DA PRESIDÊNCIA
('ASSEC', 'Assessoria de Comunicação', 'Bloco da Presidência'),
('ASGAB', 'Assessoria de Gabinete', 'Bloco da Presidência'),
('PRESID', 'Presidência', 'Bloco da Presidência'),
('VICE-PRES', 'Vice Presidência', 'Bloco da Presidência'),
('AP-JUR-PRES', 'Assessoria Jurídica', 'Bloco da Presidência')

ON CONFLICT (sigla) DO NOTHING;

-- ==========================================
-- SEEDS: UTILIZADORES ESPECIAIS & TÉCNICOS
-- ==========================================

-- 1. Solicitante Padrão (Vinculado ao Financeiro - Bloco 4)
INSERT INTO usuarios (nome, email, email_pessoal, senha_hash, telefone_whatsapp, perfil, departamento_id)
SELECT 
    'Ana Carolina Mendes',
    'servidor@sti.chamados.com',
    'ana.mendes@empresa.com.br',
    '123456',
    '(11) 98765-4321',
    'usuario',
    id FROM departamentos WHERE sigla = 'FIN'
ON CONFLICT (email) DO NOTHING;

-- 2. Carlos Daniel Santos (Técnico STI / Presidência ou DTEC)
INSERT INTO usuarios (nome, email, senha_hash, telefone_whatsapp, perfil, departamento_id)
SELECT 
    'Carlos Daniel Santos',
    'carlos.daniel@sti.chamados.com',
    '.\ati@!#$%2020',
    '(63) 98400-2020',
    'tecnico',
    id FROM departamentos WHERE sigla = 'DTEC'
ON CONFLICT (email) DO NOTHING;

-- 3. João Pedro Moreira (Técnico STI)
INSERT INTO usuarios (nome, email, senha_hash, telefone_whatsapp, perfil, departamento_id)
SELECT 
    'João Pedro Moreira',
    'joao.pedro@sti.chamados.com',
    '.\ati@!#$%2020',
    '(63) 98400-2021',
    'tecnico',
    id FROM departamentos WHERE sigla = 'DTEC'
ON CONFLICT (email) DO NOTHING;

-- 4. Guilherme Ferreira (Técnico STI)
INSERT INTO usuarios (nome, email, senha_hash, telefone_whatsapp, perfil, departamento_id)
SELECT 
    'Guilherme Ferreira',
    'guilherme.ferreira@sti.chamados.com',
    '.\ati@!#$%2020',
    '(63) 98400-2022',
    'tecnico',
    id FROM departamentos WHERE sigla = 'DTEC'
ON CONFLICT (email) DO NOTHING;

-- 5. Wanderson Alves (Chefe / Técnico de TI)
INSERT INTO usuarios (nome, email, senha_hash, telefone_whatsapp, perfil, departamento_id)
SELECT 
    'Wanderson Alves',
    'wanderson.maior@sti.chamados.com',
    '.\ati@!#$%2020',
    '(63) 98400-1001',
    'tecnico',
    id FROM departamentos WHERE sigla = 'DTEC'
ON CONFLICT (email) DO NOTHING;

-- 6. Luigue Soares Brandão (Diretor Administrativo / Gestor)
INSERT INTO usuarios (nome, email, senha_hash, telefone_whatsapp, perfil, departamento_id)
SELECT 
    'Luigue Soares Brandão',
    'luigue.brandao@sti.chamados.com',
    '123456',
    '(63) 98400-3001',
    'gestor',
    id FROM departamentos WHERE sigla = 'DIRADM'
ON CONFLICT (email) DO NOTHING;

-- 7. Elias Nunes da Silva Junior (Gerente Administrativo / Gestor)
INSERT INTO usuarios (nome, email, senha_hash, telefone_whatsapp, perfil, departamento_id)
SELECT 
    'Elias Nunes da Silva Junior',
    'elias.junior@sti.chamados.com',
    '123456',
    '(63) 98400-3002',
    'gestor',
    id FROM departamentos WHERE sigla = 'ADM'
ON CONFLICT (email) DO NOTHING;