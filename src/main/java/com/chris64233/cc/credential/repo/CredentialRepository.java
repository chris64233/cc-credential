package com.chris64233.cc.credential.repo;

import com.chris64233.cc.credential.domain.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    Optional<Credential> findByCredentialNo(String credentialNo);

    Optional<Credential> findByIdempotencyKey(String idempotencyKey);

    Optional<Credential> findByPersonIdAndTypeCodeAndActiveKey(String personId, String typeCode, String activeKey);
}
