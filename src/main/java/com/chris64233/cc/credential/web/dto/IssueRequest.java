package com.chris64233.cc.credential.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record IssueRequest(
        @NotBlank String idempotencyKey,
        @NotBlank String personId,
        @NotBlank String typeCode,
        Instant issuedAt) {
}
