package com.chris64233.cc.credential.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "credential_type")
public class CredentialType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false)
    private long validityDays;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "credential_type_required_item", joinColumns = @JoinColumn(name = "type_id"))
    @Column(name = "item", nullable = false, length = 64)
    private Set<String> requiredItems = new LinkedHashSet<>();

    protected CredentialType() {
    }

    public CredentialType(String code, String name, long validityDays, Set<String> requiredItems) {
        this.code = code;
        this.name = name;
        this.validityDays = validityDays;
        this.requiredItems = new LinkedHashSet<>(requiredItems);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public long getValidityDays() {
        return validityDays;
    }

    public Set<String> getRequiredItems() {
        return requiredItems;
    }
}
