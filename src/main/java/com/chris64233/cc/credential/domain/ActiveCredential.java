package com.chris64233.cc.credential.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 当前有效凭证指针：(personId, typeCode) 唯一约束保证同一人员同一资质类型
 * 并发签发时只能产生一张当前有效凭证。
 */
@Entity
@Table(name = "active_credential",
        uniqueConstraints = @UniqueConstraint(columnNames = {"personId", "typeCode"}))
public class ActiveCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String personId;

    @Column(nullable = false, length = 64)
    private String typeCode;

    @Column(nullable = false, unique = true, length = 64)
    private String credentialNo;

    protected ActiveCredential() {
    }

    public ActiveCredential(String personId, String typeCode, String credentialNo) {
        this.personId = personId;
        this.typeCode = typeCode;
        this.credentialNo = credentialNo;
    }

    public Long getId() {
        return id;
    }

    public String getPersonId() {
        return personId;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public String getCredentialNo() {
        return credentialNo;
    }

    public void pointTo(String credentialNo) {
        this.credentialNo = credentialNo;
    }
}
