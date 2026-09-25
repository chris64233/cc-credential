package com.chris64233.cc.credential.repo;

import com.chris64233.cc.credential.domain.CredentialEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CredentialEventRepository extends JpaRepository<CredentialEvent, Long> {

    List<CredentialEvent> findByCredentialIdOrderByEffectiveAtAscIdAsc(Long credentialId);
}
