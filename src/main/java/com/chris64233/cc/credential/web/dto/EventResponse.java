package com.chris64233.cc.credential.web.dto;

import com.chris64233.cc.credential.domain.CredentialEvent;
import com.chris64233.cc.credential.domain.EventType;

import java.time.Instant;

public record EventResponse(String credentialNo, int seq, EventType type,
                            Instant effectiveAt, String reason, Instant recordedAt) {

    public static EventResponse from(CredentialEvent event, String credentialNo) {
        return new EventResponse(credentialNo, event.getSeq(), event.getEventType(),
                event.getEffectiveAt(), event.getReason(), event.getRecordedAt());
    }
}
