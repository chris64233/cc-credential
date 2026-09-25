package com.chris64233.cc.credential.domain;

public enum VerificationStatus {
    /** 核验时点早于签发时间 */
    NOT_ISSUED,
    VALID,
    SUSPENDED,
    REVOKED,
    EXPIRED
}
