package com.chris64233.cc.credential.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.Set;

public record DefineTypeRequest(@NotBlank String code,
                                @NotBlank String name,
                                @Positive long validityDays,
                                @NotEmpty Set<@NotBlank String> requiredItems) {
}
