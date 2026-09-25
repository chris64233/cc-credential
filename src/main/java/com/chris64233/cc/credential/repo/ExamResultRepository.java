package com.chris64233.cc.credential.repo;

import com.chris64233.cc.credential.domain.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    Optional<ExamResult> findByResultNo(String resultNo);

    Optional<ExamResult> findFirstByPersonIdAndItemOrderByCompletedAtDescIdDesc(String personId, String item);

    List<ExamResult> findByPersonId(String personId);
}
