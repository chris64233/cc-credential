package com.chris64233.cc.credential.service;

import com.chris64233.cc.credential.domain.ActiveCredential;
import com.chris64233.cc.credential.domain.Credential;
import com.chris64233.cc.credential.domain.CredentialEvidence;
import com.chris64233.cc.credential.domain.CredentialType;
import com.chris64233.cc.credential.domain.ExamResult;
import com.chris64233.cc.credential.repo.ActiveCredentialRepository;
import com.chris64233.cc.credential.repo.CredentialRepository;
import com.chris64233.cc.credential.repo.CredentialTypeRepository;
import com.chris64233.cc.credential.repo.ExamResultRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * 在独立事务中执行一次签发尝试，供 {@link CredentialService} 在唯一约束冲突时整体重试。
 */
@Component
public class IssuanceExecutor {

    private final CredentialTypeRepository typeRepository;
    private final ExamResultRepository examResultRepository;
    private final CredentialRepository credentialRepository;
    private final ActiveCredentialRepository activeCredentialRepository;

    public IssuanceExecutor(CredentialTypeRepository typeRepository,
                            ExamResultRepository examResultRepository,
                            CredentialRepository credentialRepository,
                            ActiveCredentialRepository activeCredentialRepository) {
        this.typeRepository = typeRepository;
        this.examResultRepository = examResultRepository;
        this.credentialRepository = credentialRepository;
        this.activeCredentialRepository = activeCredentialRepository;
    }

    @Transactional
    public Credential execute(IssueCommand command) {
        var existing = credentialRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            Credential credential = existing.get();
            if (!credential.getRequestFingerprint().equals(command.fingerprint())) {
                throw new ConflictException("幂等键已被不同内容的请求使用");
            }
            return credential;
        }

        CredentialType type = typeRepository.findByCode(command.typeCode())
                .orElseThrow(() -> new NotFoundException("资质类型不存在: " + command.typeCode()));
        if (type.getRequiredItems().isEmpty()) {
            throw new BusinessRuleException("资质类型未定义必需考核项目: " + command.typeCode());
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(type.getValidityDays(), ChronoUnit.DAYS);

        Credential credential = new Credential(
                "CRD-" + UUID.randomUUID(),
                command.idempotencyKey(),
                command.fingerprint(),
                command.personId(),
                command.typeCode(),
                now,
                expiresAt);

        for (String item : type.getRequiredItems()) {
            ExamResult latest = examResultRepository
                    .findFirstByPersonIdAndItemOrderByCompletedAtDescIdDesc(command.personId(), item)
                    .orElseThrow(() -> new BusinessRuleException("缺少必需考核项目的结果: " + item));
            if (!latest.isPassed()) {
                throw new BusinessRuleException("项目最新考核结果未通过: " + item);
            }
            if (!latest.getValidUntil().isAfter(now)) {
                throw new BusinessRuleException("项目最新考核结果已过期: " + item);
            }
            credential.addEvidence(new CredentialEvidence(
                    item, latest.getResultNo(), latest.getCompletedAt(), latest.getValidUntil()));
        }

        var pointer = activeCredentialRepository
                .findByPersonIdAndTypeCode(command.personId(), command.typeCode());
        if (pointer.isPresent()) {
            Credential current = credentialRepository
                    .findByCredentialNo(pointer.get().getCredentialNo())
                    .orElseThrow(() -> new IllegalStateException("当前有效凭证指针失效"));
            if (current.getExpiresAt().isAfter(now)) {
                throw new ConflictException("该人员已持有此资质类型的当前有效凭证: " + current.getCredentialNo());
            }
            pointer.get().pointTo(credential.getCredentialNo());
        } else {
            activeCredentialRepository.save(new ActiveCredential(
                    command.personId(), command.typeCode(), credential.getCredentialNo()));
        }

        try {
            return credentialRepository.saveAndFlush(credential);
        } catch (DataIntegrityViolationException e) {
            throw new ConcurrentIssuanceException(e);
        }
    }

    /**
     * 唯一约束（幂等键 / 当前有效指针）冲突，提示外层以新事务重试。
     */
    public static class ConcurrentIssuanceException extends RuntimeException {

        ConcurrentIssuanceException(Throwable cause) {
            super(cause);
        }
    }
}
