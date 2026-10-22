-- Fuso horário de cada usuário (nome IANA, ex.: America/Sao_Paulo). Define o "hoje" dos Insights e o dia em
-- que um hábito foi criado ou excluído. As contas que já existem ficam no fuso que o servidor usava até aqui.
ALTER TABLE usuario ADD COLUMN fuso VARCHAR(50) NOT NULL DEFAULT 'America/Sao_Paulo';
