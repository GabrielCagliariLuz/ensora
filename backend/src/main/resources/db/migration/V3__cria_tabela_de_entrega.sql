CREATE TABLE entrega (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id    BIGINT       NOT NULL REFERENCES usuario (id),
    titulo        VARCHAR(150) NOT NULL,
    prazo_data    DATE         NOT NULL,
    prazo_horario TIME,
    concluida     BOOLEAN      NOT NULL DEFAULT false,
    concluida_em  TIMESTAMPTZ,
    nota          VARCHAR(1000),
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_entrega_titulo_nao_vazio CHECK (btrim(titulo) <> ''),
    CONSTRAINT ck_entrega_conclusao CHECK (concluida = (concluida_em IS NOT NULL))
);

CREATE INDEX ix_entrega_usuario_prazo ON entrega (usuario_id, prazo_data);
