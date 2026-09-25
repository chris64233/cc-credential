package com.chris64233.cc.credential.repo;

import com.chris64233.cc.credential.domain.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    Optional<ExamResult> findByExternalNo(String externalNo);

    Optional<ExamResult> findTopByPersonIdAndItemCodeAndCompletedAtLessThanEqualOrderByCompletedAtDesc(
            String personId, String itemCode, Instant completedAt);
}
