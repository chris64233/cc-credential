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
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "credential_type", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class CredentialType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "validity_days", nullable = false)
    private int validityDays;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "credential_type_item", joinColumns = @JoinColumn(name = "type_id"))
    @Column(name = "item_code", nullable = false, length = 64)
    @OrderColumn(name = "item_order")
    private List<String> requiredItems = new ArrayList<>();

    protected CredentialType() {
    }

    public CredentialType(String code, String name, int validityDays, List<String> requiredItems) {
        this.code = code;
        this.name = name;
        this.validityDays = validityDays;
        this.requiredItems = new ArrayList<>(requiredItems);
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

    public int getValidityDays() {
        return validityDays;
    }

    public List<String> getRequiredItems() {
        return requiredItems;
    }
}
