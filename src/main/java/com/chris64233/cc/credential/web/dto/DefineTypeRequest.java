package com.chris64233.cc.credential.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DefineTypeRequest(
        @NotBlank String code,
        @NotBlank String name,
        @Min(1) int validityDays,
        @NotEmpty List<@NotBlank String> requiredItems) {
}
