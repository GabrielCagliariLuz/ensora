package com.ensora.evento;

/**
 * Categoria opcional do evento. Cada uma tem uma cor fixa definida no front; evento sem categoria usa a cor neutra.
 * Os valores são gravados por nome e o banco os confere (ck_evento_categoria): mudar a lista exige migração.
 */
public enum CategoriaEvento {
	SAUDE,
	TRABALHO,
	PESSOAL,
	ESTUDO,
	CELEBRACAO
}
