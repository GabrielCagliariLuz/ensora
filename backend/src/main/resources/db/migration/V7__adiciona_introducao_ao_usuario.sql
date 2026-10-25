-- Quando o usuário concluiu (ou pulou) o tutorial de primeiro acesso. Nulo = ainda não viu: só as contas novas
-- começam assim. As contas que já existem não precisam do tutorial, então ficam marcadas como concluídas agora.
ALTER TABLE usuario ADD COLUMN introducao_concluida_em TIMESTAMPTZ;
UPDATE usuario SET introducao_concluida_em = now();
