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

import java.time.Instant;

/**
 * 凭证状态事件，写入后不可改写（系统不提供任何更新/删除入口）。
 */
@Entity
@Table(name = "credential_event")
public class CredentialEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credential_id", nullable = false)
    private Credential credential;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventAction action;

    @Column(nullable = false)
    private Instant effectiveAt;

    @Column(nullable = false, length = 256)
    private String reason;

    @Column(nullable = false)
    private Instant recordedAt;

    protected CredentialEvent() {
    }

    public CredentialEvent(Credential credential, EventAction action, Instant effectiveAt,
                           String reason, Instant recordedAt) {
        this.credential = credential;
        this.action = action;
        this.effectiveAt = effectiveAt;
        this.reason = reason;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public Credential getCredential() {
        return credential;
    }

    public EventAction getAction() {
        return action;
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
