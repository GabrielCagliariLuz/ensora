CREATE TABLE habito (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL REFERENCES usuario (id),
    nome        VARCHAR(100) NOT NULL,
    tipo        VARCHAR(10)  NOT NULL,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    excluido_em TIMESTAMPTZ,
    CONSTRAINT ck_habito_tipo CHECK (tipo IN ('DIARIO', 'SEMANAL')),
    CONSTRAINT ck_habito_nome_nao_vazio CHECK (btrim(nome) <> '')
);

CREATE INDEX ix_habito_usuario ON habito (usuario_id);

CREATE TABLE habito_meta (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    habito_id        BIGINT  NOT NULL REFERENCES habito (id),
    vezes_por_semana INTEGER NOT NULL,
    vigente_desde    DATE    NOT NULL,
    CONSTRAINT uk_habito_meta_vigencia UNIQUE (habito_id, vigente_desde),
    CONSTRAINT ck_habito_meta_vezes CHECK (vezes_por_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_habito_meta_segunda CHECK (EXTRACT(ISODOW FROM vigente_desde) = 1)
);

CREATE TABLE registro_habito (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    habito_id BIGINT NOT NULL REFERENCES habito (id),
    data      DATE   NOT NULL,
    CONSTRAINT uk_registro_habito_dia UNIQUE (habito_id, data)
);
