package com.chris64233.cc.credential.repo;

import com.chris64233.cc.credential.domain.CredentialType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredentialTypeRepository extends JpaRepository<CredentialType, Long> {

    Optional<CredentialType> findByCode(String code);
}
