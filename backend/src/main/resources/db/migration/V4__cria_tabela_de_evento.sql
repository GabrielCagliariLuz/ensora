CREATE TABLE evento (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id BIGINT       NOT NULL REFERENCES usuario (id),
    titulo     VARCHAR(150) NOT NULL,
    data       DATE         NOT NULL,
    inicio     TIME         NOT NULL,
    termino    TIME,
    local      VARCHAR(100),
    categoria  VARCHAR(20),
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_evento_titulo_nao_vazio CHECK (btrim(titulo) <> ''),
    CONSTRAINT ck_evento_termino_depois_do_inicio CHECK (termino IS NULL OR termino > inicio),
    CONSTRAINT ck_evento_categoria CHECK (categoria IN ('SAUDE', 'TRABALHO', 'PESSOAL', 'ESTUDO', 'CELEBRACAO'))
);

CREATE INDEX ix_evento_usuario_data ON evento (usuario_id, data);
