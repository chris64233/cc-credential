package com.chris64233.cc.credential.web.dto;

import com.chris64233.cc.credential.domain.ExamResult;

import java.time.Instant;

public record ResultResponse(String externalNo, String personId, String itemCode, boolean passed,
                             Instant completedAt, Instant validUntil) {

    public static ResultResponse from(ExamResult result) {
        return new ResultResponse(result.getExternalNo(), result.getPersonId(), result.getItemCode(),
                result.isPassed(), result.getCompletedAt(), result.getValidUntil());
    }
}
