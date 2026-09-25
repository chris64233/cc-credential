package com.chris64233.cc.credential.service;

import java.time.Instant;

public record EvidenceSummary(String item, String resultNo, Instant completedAt, Instant validUntil) {
}
