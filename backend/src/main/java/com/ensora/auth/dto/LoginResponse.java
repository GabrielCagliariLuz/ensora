package com.ensora.auth.dto;

public record LoginResponse(String tokenAcesso, long expiraEmSegundos) {
}
