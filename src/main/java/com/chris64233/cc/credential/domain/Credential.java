package com.chris64233.cc.credential.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "credential")
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String credentialNo;

    @Column(nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(nullable = false, length = 256)
    private String requestFingerprint;

    @Column(nullable = false, length = 64)
    private String personId;

    @Column(nullable = false, length = 64)
    private String typeCode;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @OneToMany(mappedBy = "credential", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("item ASC")
    private List<CredentialEvidence> evidences = new ArrayList<>();

    protected Credential() {
    }

    public Credential(String credentialNo, String idempotencyKey, String requestFingerprint,
                      String personId, String typeCode, Instant issuedAt, Instant expiresAt) {
        this.credentialNo = credentialNo;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.personId = personId;
        this.typeCode = typeCode;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public void addEvidence(CredentialEvidence evidence) {
        evidence.attach(this);
        this.evidences.add(evidence);
    }

    public Long getId() {
        return id;
    }

    public String getCredentialNo() {
        return credentialNo;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
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

    public List<CredentialEvidence> getEvidences() {
        return evidences;
    }
}
