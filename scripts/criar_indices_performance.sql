-- =============================================================================
-- REFINEMENT: Índices B-Tree Compostos para Otimização de Consultas Frequentes
-- Schema: audit_schema
-- =============================================================================

-- 1. Otimização da busca de violações pendentes por severidade/data (Painel Gerencial)
CREATE INDEX IF NOT EXISTS idx_violation_status_data 
ON audit_schema.tb_data_violation (status_resolucao, data_identificacao DESC);

-- 2. Otimização da timeline de auditoria por entidade e ID (Painel Cliente / Técnico)
CREATE INDEX IF NOT EXISTS idx_audit_event_entity_timeline 
ON audit_schema.tb_audit_event (entidade, id_entidade, data_hora_evento DESC);
