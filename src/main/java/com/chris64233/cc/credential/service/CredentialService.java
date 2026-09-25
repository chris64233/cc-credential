package com.chris64233.cc.credential.service;

import com.chris64233.cc.credential.domain.Credential;
import com.chris64233.cc.credential.domain.CredentialEvent;
import com.chris64233.cc.credential.domain.CredentialType;
import com.chris64233.cc.credential.domain.EventAction;
import com.chris64233.cc.credential.domain.ExamResult;
import com.chris64233.cc.credential.repo.ActiveCredentialRepository;
import com.chris64233.cc.credential.repo.CredentialEventRepository;
import com.chris64233.cc.credential.repo.CredentialRepository;
import com.chris64233.cc.credential.repo.CredentialTypeRepository;
import com.chris64233.cc.credential.repo.ExamResultRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CredentialService {

    private static final int MAX_ISSUANCE_ATTEMPTS = 3;

    private final CredentialTypeRepository typeRepository;
    private final ExamResultRepository examResultRepository;
    private final CredentialRepository credentialRepository;
    private final CredentialEventRepository eventRepository;
    private final ActiveCredentialRepository activeCredentialRepository;
    private final IssuanceExecutor issuanceExecutor;

    public CredentialService(CredentialTypeRepository typeRepository,
                             ExamResultRepository examResultRepository,
                             CredentialRepository credentialRepository,
                             CredentialEventRepository eventRepository,
                             ActiveCredentialRepository activeCredentialRepository,
                             IssuanceExecutor issuanceExecutor) {
        this.typeRepository = typeRepository;
        this.examResultRepository = examResultRepository;
        this.credentialRepository = credentialRepository;
        this.eventRepository = eventRepository;
        this.activeCredentialRepository = activeCredentialRepository;
        this.issuanceExecutor = issuanceExecutor;
    }

    @Transactional
    public CredentialType defineType(String code, String name, long validityDays, Set<String> requiredItems) {
        if (validityDays <= 0) {
            throw new BusinessRuleException("有效期天数必须为正数");
        }
        if (requiredItems == null || requiredItems.isEmpty()) {
            throw new BusinessRuleException("资质类型必须定义至少一个必需考核项目");
        }
        typeRepository.findByCode(code).ifPresent(t -> {
            throw new ConflictException("资质类型编码已存在: " + code);
        });
        return typeRepository.save(new CredentialType(code, name, validityDays, requiredItems));
    }

    @Transactional
    public ExamResult recordResult(String resultNo, String personId, String item, boolean passed,
                                   Instant completedAt, Instant validUntil) {
        if (!validUntil.isAfter(completedAt)) {
            throw new BusinessRuleException("结果有效截止时间必须晚于完成时间");
        }
        examResultRepository.findByResultNo(resultNo).ifPresent(r -> {
            throw new ConflictException("外部结果号已存在: " + resultNo);
        });
        return examResultRepository.save(
                new ExamResult(resultNo, personId, item, passed, completedAt, validUntil));
    }

    /**
     * 签发凭证。唯一约束冲突时以全新事务重试：重放场景返回原凭证，并发场景得到明确的冲突结果。
     */
    public Credential issue(IssueCommand command) {
        for (int attempt = 1; ; attempt++) {
            try {
                return issuanceExecutor.execute(command);
            } catch (IssuanceExecutor.ConcurrentIssuanceException | DataIntegrityViolationException e) {
                if (attempt == MAX_ISSUANCE_ATTEMPTS) {
                    throw new ConflictException("并发签发冲突，请重试");
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public Optional<Credential> findByIdempotencyKey(String idempotencyKey) {
        return credentialRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Transactional
    public CredentialEvent addEvent(String credentialNo, EventAction action, Instant effectiveAt, String reason) {
        Credential credential = credentialRepository.findByCredentialNo(credentialNo)
                .orElseThrow(() -> new NotFoundException("凭证不存在: " + credentialNo));
        List<CredentialEvent> events = eventRepository.findByCredentialIdOrderByEffectiveAtAscIdAsc(credential.getId());

        if (events.stream().anyMatch(e -> e.getAction() == EventAction.REVOKE)) {
            throw new BusinessRuleException("凭证已撤销，撤销为终态不可再变更");
        }
        if (effectiveAt.isBefore(credential.getIssuedAt())) {
            throw new BusinessRuleException("状态事件生效时间不得早于签发时间");
        }
        if (!events.isEmpty() && effectiveAt.isBefore(events.getLast().getEffectiveAt())) {
            throw new BusinessRuleException("状态事件生效时间不得早于上一事件");
        }
        if (effectiveAt.isAfter(credential.getExpiresAt())) {
            throw new BusinessRuleException("状态事件生效时间不得晚于凭证自然到期时间");
        }

        VerificationStatus current = statusAt(credential, events, effectiveAt);
        switch (action) {
            case SUSPEND -> {
                if (current != VerificationStatus.ACTIVE) {
                    throw new BusinessRuleException("仅当前有效的凭证可以暂停");
                }
            }
            case RESUME -> {
                if (current != VerificationStatus.SUSPENDED) {
                    throw new BusinessRuleException("仅暂停状态的凭证可以恢复");
                }
            }
            case REVOKE -> {
                // 撤销在任意非终态下均允许
            }
        }

        CredentialEvent event = eventRepository.save(
                new CredentialEvent(credential, action, effectiveAt, reason, Instant.now()));
        if (action == EventAction.REVOKE) {
            activeCredentialRepository.deleteByCredentialNo(credentialNo);
        }
        return event;
    }

    @Transactional(readOnly = true)
    public VerificationView verify(String credentialNo, Instant at) {
        Credential credential = credentialRepository.findByCredentialNo(credentialNo)
                .orElseThrow(() -> new NotFoundException("凭证不存在: " + credentialNo));
        List<CredentialEvent> events = eventRepository.findByCredentialIdOrderByEffectiveAtAscIdAsc(credential.getId());
        List<EvidenceSummary> evidence = credential.getEvidences().stream()
                .map(e -> new EvidenceSummary(e.getItem(), e.getResultNo(), e.getCompletedAt(), e.getValidUntil()))
                .toList();
        return new VerificationView(
                credential.getCredentialNo(),
                credential.getPersonId(),
                credential.getTypeCode(),
                credential.getIssuedAt(),
                credential.getExpiresAt(),
                at,
                statusAt(credential, events, at),
                evidence);
    }

    /**
     * 依据签发时间、自然到期时间和状态事件，重算指定时点的真实状态。
     */
    private VerificationStatus statusAt(Credential credential, List<CredentialEvent> events, Instant at) {
        if (at.isBefore(credential.getIssuedAt())) {
            return VerificationStatus.NOT_YET_VALID;
        }
        VerificationStatus status = VerificationStatus.ACTIVE;
        for (CredentialEvent event : events) {
            if (event.getEffectiveAt().isAfter(at)) {
                break;
            }
            status = switch (event.getAction()) {
                case SUSPEND -> VerificationStatus.SUSPENDED;
                case RESUME -> VerificationStatus.ACTIVE;
                case REVOKE -> VerificationStatus.REVOKED;
            };
        }
        if (status != VerificationStatus.REVOKED && !at.isBefore(credential.getExpiresAt())) {
            return VerificationStatus.EXPIRED;
        }
        return status;
    }
}
