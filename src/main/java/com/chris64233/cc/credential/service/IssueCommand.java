package com.chris64233.cc.credential.service;

public record IssueCommand(String idempotencyKey, String personId, String typeCode) {

    public String fingerprint() {
        return personId + "|" + typeCode;
    }
}
