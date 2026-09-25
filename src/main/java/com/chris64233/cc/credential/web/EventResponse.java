package com.chris64233.cc.credential.web;

import com.chris64233.cc.credential.domain.CredentialEvent;
import com.chris64233.cc.credential.domain.EventAction;

import java.time.Instant;

public record EventResponse(Long id,
                            EventAction action,
                            Instant effectiveAt,
                            String reason,
                            Instant recordedAt) {

    public static EventResponse from(CredentialEvent event) {
        return new EventResponse(event.getId(), event.getAction(), event.getEffectiveAt(),
                event.getReason(), event.getRecordedAt());
    }
}
