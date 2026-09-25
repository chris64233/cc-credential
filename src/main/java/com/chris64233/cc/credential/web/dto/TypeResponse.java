package com.chris64233.cc.credential.web.dto;

import com.chris64233.cc.credential.domain.CredentialType;

import java.util.List;

public record TypeResponse(String code, String name, int validityDays, List<String> requiredItems) {

    public static TypeResponse from(CredentialType type) {
        return new TypeResponse(type.getCode(), type.getName(), type.getValidityDays(),
                List.copyOf(type.getRequiredItems()));
    }
}
