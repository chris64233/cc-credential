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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * 签发时实际采用的考核证据快照，随凭证保存，不随后续考核结果变化。
 */
@Entity
@Table(name = "credential_evidence",
        uniqueConstraints = @UniqueConstraint(columnNames = {"credential_id", "item"}))
public class CredentialEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credential_id", nullable = false)
    private Credential credential;

    @Column(nullable = false, length = 64)
    private String item;

    @Column(nullable = false, length = 64)
    private String resultNo;

    @Column(nullable = false)
    private Instant completedAt;

    @Column(nullable = false)
    private Instant validUntil;

    protected CredentialEvidence() {
    }

    public CredentialEvidence(String item, String resultNo, Instant completedAt, Instant validUntil) {
        this.item = item;
        this.resultNo = resultNo;
        this.completedAt = completedAt;
        this.validUntil = validUntil;
    }

    void attach(Credential credential) {
        this.credential = credential;
    }

    public Long getId() {
        return id;
    }

    public String getItem() {
        return item;
    }

    public String getResultNo() {
        return resultNo;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getValidUntil() {
        return validUntil;
    }
}
