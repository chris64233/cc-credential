package com.chris64233.cc.credential.repo;

import com.chris64233.cc.credential.domain.ActiveCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActiveCredentialRepository extends JpaRepository<ActiveCredential, Long> {

    Optional<ActiveCredential> findByPersonIdAndTypeCode(String personId, String typeCode);

    void deleteByCredentialNo(String credentialNo);
}
