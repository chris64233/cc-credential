package com.chris64233.cc.credential.web.dto;

import com.chris64233.cc.credential.domain.VerificationStatus;
import com.chris64233.cc.credential.service.VerificationView;

import java.time.Instant;
import java.util.List;

public record VerifyResponse(String credentialNo, String personId, String typeCode,
                             Instant issuedAt, Instant expiresAt, Instant asOf,
                             VerificationStatus status, List<EvidenceItem> evidence) {

    public record EvidenceItem(String externalNo, String itemCode,
                               Instant completedAt, Instant validUntil) {
    }

    public static VerifyResponse from(VerificationView view) {
        List<EvidenceItem> evidence = view.evidence().stream()
                .map(item -> new EvidenceItem(item.externalNo(), item.itemCode(),
                        item.completedAt(), item.validUntil()))
                .toList();
        return new VerifyResponse(view.credentialNo(), view.personId(), view.typeCode(),
                view.issuedAt(), view.expiresAt(), view.asOf(), view.status(), evidence);
    }
}
