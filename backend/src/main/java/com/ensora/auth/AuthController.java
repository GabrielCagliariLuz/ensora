package com.ensora.auth;

import com.ensora.auth.dto.LoginRequest;
import com.ensora.auth.dto.LoginResponse;
import com.ensora.auth.dto.RegistrarRequest;
import com.ensora.auth.dto.UsuarioResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
public class AuthController {

	public static final String NOME_COOKIE_REFRESH = "refresh_token";

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/registrar")
	@ResponseStatus(HttpStatus.CREATED)
	public UsuarioResponse registrar(@Valid @RequestBody RegistrarRequest requisicao) {
		return authService.registrar(requisicao);
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest requisicao,
			HttpServletRequest http) {
		return respostaDeSessao(authService.login(requisicao, http.getRemoteAddr()));
	}

	@PostMapping("/refresh")
	public ResponseEntity<LoginResponse> refresh(
			@CookieValue(name = NOME_COOKIE_REFRESH, required = false) String refreshToken) {
		return respostaDeSessao(authService.refresh(refreshToken));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
			@CookieValue(name = NOME_COOKIE_REFRESH, required = false) String refreshToken) {
		authService.logout(refreshToken);
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, cookieRefresh("", Duration.ZERO).toString())
				.build();
	}

	private ResponseEntity<LoginResponse> respostaDeSessao(SessaoCriada sessao) {
		LoginResponse corpo = new LoginResponse(sessao.tokenAcesso().valor(),
				sessao.tokenAcesso().expiraEmSegundos());
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE,
						cookieRefresh(sessao.refreshToken().valor(), sessao.refreshToken().validade()).toString())
				.body(corpo);
	}

	/**
	 * O refresh token só viaja neste cookie: HttpOnly (o JavaScript não lê), Secure, SameSite=Strict e restrito
	 * a /auth. Para apagar o cookie, o navegador exige os mesmos atributos de caminho; por isso um método só.
	 */
	private ResponseCookie cookieRefresh(String valor, Duration validade) {
		return ResponseCookie.from(NOME_COOKIE_REFRESH, valor)
				.httpOnly(true)
				.secure(true)
				.sameSite("Strict")
				.path("/auth")
				.maxAge(validade)
				.build();
	}

}
