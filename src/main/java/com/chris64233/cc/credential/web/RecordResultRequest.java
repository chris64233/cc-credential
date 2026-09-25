package com.chris64233.cc.credential.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RecordResultRequest(@NotBlank String resultNo,
                                  @NotBlank String personId,
                                  @NotBlank String item,
                                  boolean passed,
                                  @NotNull Instant completedAt,
                                  @NotNull Instant validUntil) {
}
