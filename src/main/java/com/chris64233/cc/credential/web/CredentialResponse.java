package com.chris64233.cc.credential.web;

import com.chris64233.cc.credential.domain.Credential;
import com.chris64233.cc.credential.service.EvidenceSummary;

import java.time.Instant;
import java.util.List;

public record CredentialResponse(String credentialNo,
                                 String personId,
                                 String typeCode,
                                 Instant issuedAt,
                                 Instant expiresAt,
                                 List<EvidenceSummary> evidence) {

    public static CredentialResponse from(Credential credential) {
        return new CredentialResponse(
                credential.getCredentialNo(),
                credential.getPersonId(),
                credential.getTypeCode(),
                credential.getIssuedAt(),
                credential.getExpiresAt(),
                credential.getEvidences().stream()
                        .map(e -> new EvidenceSummary(e.getItem(), e.getResultNo(), e.getCompletedAt(), e.getValidUntil()))
                        .toList());
    }
}
