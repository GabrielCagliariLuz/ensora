ALTER TABLE entrega ADD COLUMN concluida_data DATE;

-- Linhas já concluídas ganham a data (em UTC) do instante da conclusão; sem isso o novo CHECK falharia nelas.
UPDATE entrega SET concluida_data = (concluida_em AT TIME ZONE 'UTC')::date WHERE concluida_em IS NOT NULL;

ALTER TABLE entrega DROP CONSTRAINT ck_entrega_conclusao;
ALTER TABLE entrega ADD CONSTRAINT ck_entrega_conclusao
    CHECK (concluida = (concluida_em IS NOT NULL) AND concluida = (concluida_data IS NOT NULL));
