package com.chris64233.cc.credential.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 签发时实际采用的考核证据快照，随凭证保存、不再变化。
 */
@Entity
@Table(name = "credential_evidence")
public class CredentialEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credential_id", nullable = false)
    private Credential credential;

    @Column(name = "external_no", nullable = false, length = 64)
    private String externalNo;

    @Column(name = "item_code", nullable = false, length = 64)
    private String itemCode;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    protected CredentialEvidence() {
    }

    public CredentialEvidence(Credential credential, String externalNo, String itemCode,
                              Instant completedAt, Instant validUntil) {
        this.credential = credential;
        this.externalNo = externalNo;
        this.itemCode = itemCode;
        this.completedAt = completedAt;
        this.validUntil = validUntil;
    }

    public Long getId() {
        return id;
    }

    public String getExternalNo() {
        return externalNo;
    }

    public String getItemCode() {
        return itemCode;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getValidUntil() {
        return validUntil;
    }
}
