package com.chris64233.cc.credential.web;

import jakarta.validation.constraints.NotBlank;

public record IssueCredentialRequest(@NotBlank String idempotencyKey,
                                     @NotBlank String personId,
                                     @NotBlank String typeCode) {
}
