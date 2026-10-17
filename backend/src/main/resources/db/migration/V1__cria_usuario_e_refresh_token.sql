CREATE TABLE usuario (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_email_minusculo CHECK (email = lower(email))
);

CREATE TABLE refresh_token (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL REFERENCES usuario (id),
    token_hash  VARCHAR(255) NOT NULL,
    expira_em   TIMESTAMPTZ  NOT NULL,
    revogado_em TIMESTAMPTZ,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX ix_refresh_token_usuario ON refresh_token (usuario_id);
