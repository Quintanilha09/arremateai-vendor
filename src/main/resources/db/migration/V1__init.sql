-- ============================================================================
-- E16-H7: Schema inicial do arremateai-vendor
-- ----------------------------------------------------------------------------
-- Deriva das entidades JPA em com.arremateai.vendor.domain:
--   - Usuario, DocumentoVendedor, CodigoVerificacao, HistoricoStatusVendedor
--
-- Uso de CREATE TABLE IF NOT EXISTS: o banco `arremateai` ja esta em uso
-- compartilhado entre os microsservicos durante a migracao; a idempotencia
-- garante que esta baseline nao quebre ambientes existentes. Cada servico
-- possui seu proprio `flyway_schema_history_*` (ver application.yml).
-- ============================================================================

-- Usuario (inclui campos de vendedor PJ)
CREATE TABLE IF NOT EXISTS usuario (
    id UUID PRIMARY KEY,
    nome VARCHAR(200) NOT NULL,
    email VARCHAR(200) NOT NULL,
    senha VARCHAR(500) NOT NULL,
    telefone VARCHAR(20),
    cpf VARCHAR(14),
    avatar_url VARCHAR(500),
    tipo VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL,
    cnpj VARCHAR(18),
    razao_social VARCHAR(300),
    nome_fantasia VARCHAR(300),
    inscricao_estadual VARCHAR(20),
    email_corporativo VARCHAR(200),
    email_corporativo_verificado BOOLEAN,
    status_vendedor VARCHAR(30),
    motivo_rejeicao TEXT,
    aprovado_por UUID,
    aprovado_em TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT uk_usuario_cnpj UNIQUE (cnpj),
    CONSTRAINT fk_usuario_aprovado_por FOREIGN KEY (aprovado_por) REFERENCES usuario(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_usuario_email ON usuario (email);

-- Documento do vendedor (upload de comprovantes)
CREATE TABLE IF NOT EXISTS documento_vendedor (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    nome_arquivo VARCHAR(500) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    tamanho_bytes BIGINT,
    mime_type VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    motivo_rejeicao TEXT,
    analisado_por UUID,
    analisado_em TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_documento_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_documento_analisado_por FOREIGN KEY (analisado_por) REFERENCES usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_documento_vendedor_usuario_id ON documento_vendedor (usuario_id);
CREATE INDEX IF NOT EXISTS idx_documento_vendedor_status ON documento_vendedor (status);

-- Codigo de verificacao (2FA / confirmacao de email)
CREATE TABLE IF NOT EXISTS codigo_verificacao (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    codigo VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verificado BOOLEAN,
    dados_cadastro TEXT,
    created_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_codigo_verificacao_email ON codigo_verificacao (email);
CREATE INDEX IF NOT EXISTS idx_codigo_verificacao_expires_at ON codigo_verificacao (expires_at);

-- Historico de mudancas de status do vendedor (trilha de auditoria)
CREATE TABLE IF NOT EXISTS historico_status_vendedor (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    status_anterior VARCHAR(30),
    status_novo VARCHAR(30) NOT NULL,
    motivo TEXT,
    alterado_por UUID,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_historico_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_historico_alterado_por FOREIGN KEY (alterado_por) REFERENCES usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_historico_status_vendedor_usuario_id
    ON historico_status_vendedor (usuario_id);
CREATE INDEX IF NOT EXISTS idx_historico_status_vendedor_created_at
    ON historico_status_vendedor (created_at DESC);
