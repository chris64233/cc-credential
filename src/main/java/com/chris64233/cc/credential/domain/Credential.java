package com.chris64233.cc.credential.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "credential", uniqueConstraints = {
        @UniqueConstraint(columnNames = "credential_no"),
        @UniqueConstraint(columnNames = "idempotency_key"),
        @UniqueConstraint(columnNames = {"person_id", "type_code", "active_key"})
})
public class Credential {

    /** 非撤销凭证的当前标记，配合唯一约束保证同一人员同一资质类型至多一张当前凭证。 */
    public static final String ACTIVE_KEY = "ACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "credential_no", nullable = false, length = 64)
    private String credentialNo;

    @Column(name = "person_id", nullable = false, length = 64)
    private String personId;

    @Column(name = "type_code", nullable = false, length = 64)
    private String typeCode;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "active_key", length = 16)
    private String activeKey;

    @OneToMany(mappedBy = "credential", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CredentialEvidence> evidence = new ArrayList<>();

    @OneToMany(mappedBy = "credential", cascade = CascadeType.ALL)
    @OrderBy("seq ASC")
    private List<CredentialEvent> events = new ArrayList<>();

    protected Credential() {
    }

    public Credential(String credentialNo, String personId, String typeCode, Instant issuedAt,
                      Instant expiresAt, String idempotencyKey, String requestFingerprint) {
        this.credentialNo = credentialNo;
        this.personId = personId;
        this.typeCode = typeCode;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.activeKey = ACTIVE_KEY;
    }

    public void addEvidence(CredentialEvidence item) {
        evidence.add(item);
    }

    public void addEvent(CredentialEvent event) {
        events.add(event);
    }

    public void clearActiveKey() {
        this.activeKey = null;
    }

    public boolean isActiveMarked() {
        return ACTIVE_KEY.equals(activeKey);
    }

    public Long getId() {
        return id;
    }

    public String getCredentialNo() {
        return credentialNo;
    }

    public String getPersonId() {
        return personId;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public List<CredentialEvidence> getEvidence() {
        return evidence;
    }

    public List<CredentialEvent> getEvents() {
        return events;
    }
}
