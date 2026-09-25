package com.chris64233.cc.credential.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * 凭证状态事件，写入后不可改写。
 */
@Entity
@Table(name = "credential_event",
        uniqueConstraints = @UniqueConstraint(columnNames = {"credential_id", "seq"}))
public class CredentialEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credential_id", nullable = false)
    private Credential credential;

    @Column(name = "seq", nullable = false)
    private int seq;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 16)
    private EventType eventType;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @Column(name = "reason", nullable = false, length = 512)
    private String reason;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected CredentialEvent() {
    }

    public CredentialEvent(Credential credential, int seq, EventType eventType,
                           Instant effectiveAt, String reason, Instant recordedAt) {
        this.credential = credential;
        this.seq = seq;
        this.eventType = eventType;
        this.effectiveAt = effectiveAt;
        this.reason = reason;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public int getSeq() {
        return seq;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Instant getEffectiveAt() {
        return effectiveAt;
    }

    public String getReason() {
        return reason;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
