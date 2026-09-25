package com.chris64233.cc.credential.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * 考核结果事件，写入后不可修改。
 */
@Entity
@Table(name = "exam_result", uniqueConstraints = @UniqueConstraint(columnNames = "external_no"))
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_no", nullable = false, length = 64)
    private String externalNo;

    @Column(name = "person_id", nullable = false, length = 64)
    private String personId;

    @Column(name = "item_code", nullable = false, length = 64)
    private String itemCode;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    protected ExamResult() {
    }

    public ExamResult(String externalNo, String personId, String itemCode, boolean passed,
                      Instant completedAt, Instant validUntil) {
        this.externalNo = externalNo;
        this.personId = personId;
        this.itemCode = itemCode;
        this.passed = passed;
        this.completedAt = completedAt;
        this.validUntil = validUntil;
    }

    public Long getId() {
        return id;
    }

    public String getExternalNo() {
        return externalNo;
    }

    public String getPersonId() {
        return personId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public boolean isPassed() {
        return passed;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getValidUntil() {
        return validUntil;
    }
}
