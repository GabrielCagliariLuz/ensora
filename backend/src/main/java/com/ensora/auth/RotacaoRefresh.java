package com.ensora.auth;

public record RotacaoRefresh(Long usuarioId, RefreshTokenEmitido novoRefreshToken) {
}
