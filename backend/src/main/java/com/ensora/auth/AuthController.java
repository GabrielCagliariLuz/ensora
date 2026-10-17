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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
		SessaoCriada sessao = authService.login(requisicao, http.getRemoteAddr());

		// O refresh token só viaja neste cookie: HttpOnly (o JavaScript não lê), Secure, SameSite=Strict e restrito a /auth.
		ResponseCookie cookie = ResponseCookie.from(NOME_COOKIE_REFRESH, sessao.refreshToken().valor())
				.httpOnly(true)
				.secure(true)
				.sameSite("Strict")
				.path("/auth")
				.maxAge(sessao.refreshToken().validade())
				.build();

		LoginResponse corpo = new LoginResponse(sessao.tokenAcesso().valor(),
				sessao.tokenAcesso().expiraEmSegundos());
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.body(corpo);
	}

}
