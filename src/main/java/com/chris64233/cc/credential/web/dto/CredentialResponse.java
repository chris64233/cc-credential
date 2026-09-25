package com.chris64233.cc.credential.web.dto;

import com.chris64233.cc.credential.domain.Credential;

import java.time.Instant;
import java.util.List;

public record CredentialResponse(String credentialNo, String personId, String typeCode,
                                 Instant issuedAt, Instant expiresAt,
                                 List<EvidenceItem> evidence) {

    public record EvidenceItem(String externalNo, String itemCode,
                               Instant completedAt, Instant validUntil) {
    }

    public static CredentialResponse from(Credential credential) {
        List<EvidenceItem> evidence = credential.getEvidence().stream()
                .map(item -> new EvidenceItem(item.getExternalNo(), item.getItemCode(),
                        item.getCompletedAt(), item.getValidUntil()))
                .toList();
        return new CredentialResponse(credential.getCredentialNo(), credential.getPersonId(),
                credential.getTypeCode(), credential.getIssuedAt(), credential.getExpiresAt(), evidence);
    }
}
