package com.chris64233.cc.credential.service;

import java.time.Instant;
import java.util.List;

public record VerificationView(String credentialNo,
                               String personId,
                               String typeCode,
                               Instant issuedAt,
                               Instant expiresAt,
                               Instant verifiedAt,
                               VerificationStatus status,
                               List<EvidenceSummary> evidence) {
}
