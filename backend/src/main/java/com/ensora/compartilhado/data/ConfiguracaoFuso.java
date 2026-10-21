package com.ensora.compartilhado.data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class ConfiguracaoFuso {

	/**
	 * Fuso usado para transformar instantes (criado_em, excluido_em) no dia do usuário. É fixo no servidor porque o app
	 * é de uso pessoal (seção 12); com vários usuários, passaria a ser uma configuração por usuário.
	 */
	@Bean
	public ZoneId fusoDoUsuario(@Value("${ensora.fuso}") String fuso) {
		return ZoneId.of(fuso);
	}

}
