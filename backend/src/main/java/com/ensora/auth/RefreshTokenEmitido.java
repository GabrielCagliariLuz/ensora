package com.ensora.auth;

import java.time.Duration;

public record RefreshTokenEmitido(String valor, Duration validade) {
}
