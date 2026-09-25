package com.chris64233.cc.credential.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RecordResultRequest(
        @NotBlank String externalNo,
        @NotBlank String personId,
        @NotBlank String itemCode,
        boolean passed,
        @NotNull Instant completedAt,
        @NotNull Instant validUntil) {
}
