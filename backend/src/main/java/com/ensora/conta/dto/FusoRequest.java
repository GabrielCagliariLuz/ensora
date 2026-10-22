package com.ensora.conta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FusoRequest(@NotBlank @Size(max = 50) String fuso) {
}
