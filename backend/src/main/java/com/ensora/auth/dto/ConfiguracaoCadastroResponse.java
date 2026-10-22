package com.ensora.auth.dto;

/**
 * O que a tela de cadastro precisa saber antes de mostrar o formulário: se o cadastro está aberto e se ele pede um
 * código de convite.
 */
public record ConfiguracaoCadastroResponse(boolean habilitado, boolean exigeConvite) {
}
