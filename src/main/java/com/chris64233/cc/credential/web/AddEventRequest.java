package com.chris64233.cc.credential.web;

import com.chris64233.cc.credential.domain.EventAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record AddEventRequest(@NotNull EventAction action,
                              @NotNull Instant effectiveAt,
                              @NotBlank String reason) {
}
