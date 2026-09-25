package com.chris64233.cc.credential.service;

import com.chris64233.cc.credential.domain.VerificationStatus;

import java.time.Instant;
import java.util.List;

public record VerificationView(
        String credentialNo,
        String personId,
        String typeCode,
        Instant issuedAt,
        Instant expiresAt,
        Instant asOf,
        VerificationStatus status,
        List<EvidenceSummary> evidence) {

    public record EvidenceSummary(String externalNo, String itemCode,
                                  Instant completedAt, Instant validUntil) {
    }
}
