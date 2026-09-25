package com.chris64233.cc.credential.web.dto;

import com.chris64233.cc.credential.domain.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record EventRequest(
        @NotNull EventType type,
        @NotNull Instant effectiveAt,
        @NotBlank String reason) {
}
