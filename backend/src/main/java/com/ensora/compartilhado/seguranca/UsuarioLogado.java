package com.ensora.compartilhado.seguranca;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marca um parâmetro {@code Long} do Controller que deve receber o id do usuário autenticado. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UsuarioLogado {
}
