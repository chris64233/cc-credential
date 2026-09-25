package com.chris64233.cc.credential.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 考核结果事件，写入后不可修改（系统不提供任何更新入口）。
 */
@Entity
@Table(name = "exam_result")
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String resultNo;

    @Column(nullable = false, length = 64)
    private String personId;

    @Column(nullable = false, length = 64)
    private String item;

    @Column(nullable = false)
    private boolean passed;

    @Column(nullable = false)
    private Instant completedAt;

    @Column(nullable = false)
    private Instant validUntil;

    protected ExamResult() {
    }

    public ExamResult(String resultNo, String personId, String item, boolean passed,
                      Instant completedAt, Instant validUntil) {
        this.resultNo = resultNo;
        this.personId = personId;
        this.item = item;
        this.passed = passed;
        this.completedAt = completedAt;
        this.validUntil = validUntil;
    }

    public Long getId() {
        return id;
    }

    public String getResultNo() {
        return resultNo;
    }

    public String getPersonId() {
        return personId;
    }

    public String getItem() {
        return item;
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
