package com.ensora.compartilhado.erro;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class ManipuladorDeErros {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErroResposta> aoValidarCorpo(MethodArgumentNotValidException ex,
			HttpServletRequest requisicao) {
		List<ErroCampo> campos = ex.getBindingResult().getFieldErrors().stream()
				.map(erro -> new ErroCampo(erro.getField(), erro.getDefaultMessage()))
				.toList();
		return validacao(campos, requisicao);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ErroResposta> aoValidarParametros(HandlerMethodValidationException ex,
			HttpServletRequest requisicao) {
		List<ErroCampo> campos = ex.getParameterValidationResults().stream()
				.flatMap(resultado -> resultado.getResolvableErrors().stream()
						.map(erro -> new ErroCampo(resultado.getMethodParameter().getParameterName(),
								erro.getDefaultMessage())))
				.toList();
		return validacao(campos, requisicao);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErroResposta> aoLerCorpoInvalido(HttpMessageNotReadableException ex,
			HttpServletRequest requisicao) {
		return resposta(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA",
				"Corpo da requisição ausente ou malformado", requisicao, null);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErroResposta> aoNaoAutenticar(AuthenticationException ex, HttpServletRequest requisicao) {
		// Exceção própria traz a mensagem escolhida pelo Service; as do Spring Security usam o texto padrão.
		String mensagem = ex instanceof NaoAutenticadoException ? ex.getMessage() : "Autenticação necessária";
		return resposta(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", mensagem, requisicao, null);
	}

	@ExceptionHandler(LimiteExcedidoException.class)
	public ResponseEntity<ErroResposta> aoExcederLimite(LimiteExcedidoException ex, HttpServletRequest requisicao) {
		return resposta(HttpStatus.TOO_MANY_REQUESTS, "LIMITE_EXCEDIDO", ex.getMessage(), requisicao, null);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErroResposta> aoNegarAcesso(AccessDeniedException ex, HttpServletRequest requisicao) {
		return resposta(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", "Acesso negado", requisicao, null);
	}

	@ExceptionHandler(AcessoNegadoException.class)
	public ResponseEntity<ErroResposta> aoNegarPorRegra(AcessoNegadoException ex, HttpServletRequest requisicao) {
		return resposta(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", ex.getMessage(), requisicao, null);
	}

	@ExceptionHandler(ConflitoException.class)
	public ResponseEntity<ErroResposta> aoConflitar(ConflitoException ex, HttpServletRequest requisicao) {
		return resposta(HttpStatus.CONFLICT, "CONFLITO", ex.getMessage(), requisicao, null);
	}

	@ExceptionHandler({ RecursoNaoEncontradoException.class, NoResourceFoundException.class })
	public ResponseEntity<ErroResposta> aoNaoEncontrar(Exception ex, HttpServletRequest requisicao) {
		String mensagem = ex instanceof RecursoNaoEncontradoException
				? ex.getMessage()
				: "Recurso não encontrado";
		return resposta(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", mensagem, requisicao, null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErroResposta> aoFalharInesperadamente(Exception ex, HttpServletRequest requisicao) {
		// Exceções padrão do Spring MVC (405, 415, etc.) já trazem o status correto.
		if (ex instanceof ErrorResponse padrao) {
			return resposta(padrao.getStatusCode(), "REQUISICAO_INVALIDA", "Requisição inválida",
					requisicao, null);
		}
		log.error("Erro inesperado em {}", requisicao.getRequestURI(), ex);
		return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
				"Erro interno. Tente novamente mais tarde.", requisicao, null);
	}

	private ResponseEntity<ErroResposta> validacao(List<ErroCampo> campos, HttpServletRequest requisicao) {
		return resposta(HttpStatus.BAD_REQUEST, "VALIDACAO", "Dados inválidos", requisicao, campos);
	}

	private ResponseEntity<ErroResposta> resposta(HttpStatusCode status, String erro, String mensagem,
			HttpServletRequest requisicao, List<ErroCampo> campos) {
		ErroResposta corpo = new ErroResposta(status.value(), erro, mensagem, requisicao.getRequestURI(), campos);
		return ResponseEntity.status(status).body(corpo);
	}

}
