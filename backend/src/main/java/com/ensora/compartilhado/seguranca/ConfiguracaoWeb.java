package com.ensora.compartilhado.seguranca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Em produção, um único serviço entrega a SPA e a API na mesma origem. A API fica sob {@code ensora.api.prefixo}
 * (ex.: /api), e tudo que não é API nem arquivo (ex.: /calendario) cai no index.html para a SPA resolver a rota.
 */
@Configuration
public class ConfiguracaoWeb implements WebMvcConfigurer {

	private final String prefixo;

	public ConfiguracaoWeb(@Value("${ensora.api.prefixo:}") String prefixo) {
		// Um valor como "C:/.../api" (conversão de caminho do shell) vira erro claro, e não um erro do Spring Security.
		if (!prefixo.isEmpty() && !prefixo.matches("/[A-Za-z0-9._-]+")) {
			throw new IllegalStateException("ensora.api.prefixo deve ser vazio ou começar com / (ex.: /api): " + prefixo);
		}
		this.prefixo = prefixo;
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvedores) {
		resolvedores.add(new UsuarioLogadoResolver());
	}

	@Override
	public void configurePathMatch(PathMatchConfigurer configurador) {
		if (!prefixo.isEmpty()) {
			// Só os controllers ganham o prefixo: os arquivos estáticos e a SPA ficam na raiz.
			configurador.addPathPrefix(prefixo, HandlerTypePredicate.forAnnotation(RestController.class));
		}
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registro) {
		// Arquivos com hash no nome podem ser cacheados por um ano.
		registro.addResourceHandler("/assets/**")
				.addResourceLocations("classpath:/static/assets/")
				.setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic());
		// O resto (index.html, sw.js, manifest) não é cacheado, para a versão nova aparecer no carregamento.
		registro.addResourceHandler("/**")
				.addResourceLocations("classpath:/static/")
				.setCacheControl(CacheControl.noCache())
				.resourceChain(true)
				.addResolver(new FallbackParaSpa(prefixo));
	}

	/**
	 * Caminho que não é arquivo nem API cai no index.html. Arquivo ausente (com extensão) e caminho desconhecido
	 * sob o prefixo da API continuam 404.
	 */
	static final class FallbackParaSpa extends PathResourceResolver {

		private final String prefixoSemBarra;

		FallbackParaSpa(String prefixo) {
			this.prefixoSemBarra = prefixo.startsWith("/") ? prefixo.substring(1) : prefixo;
		}

		@Override
		protected Resource getResource(String caminho, Resource localizacao) throws IOException {
			Resource recurso = super.getResource(caminho, localizacao);
			if (recurso != null || !ehRotaDaSpa(caminho)) {
				return recurso;
			}
			Resource indice = new ClassPathResource("static/index.html");
			return indice.exists() ? indice : null;
		}

		private boolean ehRotaDaSpa(String caminho) {
			if (!prefixoSemBarra.isEmpty()
					&& (caminho.equals(prefixoSemBarra) || caminho.startsWith(prefixoSemBarra + "/"))) {
				return false;
			}
			return !caminho.substring(caminho.lastIndexOf('/') + 1).contains(".");
		}
	}

}
