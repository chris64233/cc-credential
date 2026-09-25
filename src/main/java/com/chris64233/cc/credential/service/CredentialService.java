package com.chris64233.cc.credential.service;

import com.chris64233.cc.credential.domain.Credential;
import com.chris64233.cc.credential.domain.CredentialEvidence;
import com.chris64233.cc.credential.domain.CredentialEvent;
import com.chris64233.cc.credential.domain.CredentialType;
import com.chris64233.cc.credential.domain.EventType;
import com.chris64233.cc.credential.domain.ExamResult;
import com.chris64233.cc.credential.domain.VerificationStatus;
import com.chris64233.cc.credential.repo.CredentialRepository;
import com.chris64233.cc.credential.repo.CredentialTypeRepository;
import com.chris64233.cc.credential.repo.ExamResultRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CredentialService {

    private final CredentialTypeRepository typeRepository;
    private final ExamResultRepository resultRepository;
    private final CredentialRepository credentialRepository;
    private final Clock clock;

    public CredentialService(CredentialTypeRepository typeRepository,
                             ExamResultRepository resultRepository,
                             CredentialRepository credentialRepository,
                             Clock clock) {
        this.typeRepository = typeRepository;
        this.resultRepository = resultRepository;
        this.credentialRepository = credentialRepository;
        this.clock = clock;
    }

    @Transactional
    public CredentialType defineType(String code, String name, int validityDays, List<String> requiredItems) {
        typeRepository.findByCode(code).ifPresent(existing -> {
            throw ApiException.conflict("资质类型已存在: " + code);
        });
        if (requiredItems == null || requiredItems.isEmpty()) {
            throw ApiException.unprocessable("资质类型必须定义至少一个必需考核项目");
        }
        return typeRepository.save(new CredentialType(code, name, validityDays, requiredItems));
    }

    @Transactional(readOnly = true)
    public Optional<CredentialType> getType(String code) {
        return typeRepository.findByCode(code);
    }

    @Transactional
    public ExamResult recordResult(String externalNo, String personId, String itemCode, boolean passed,
                                   Instant completedAt, Instant validUntil) {
        resultRepository.findByExternalNo(externalNo).ifPresent(existing -> {
            throw ApiException.conflict("外部结果号已存在: " + externalNo);
        });
        if (!validUntil.isAfter(completedAt)) {
            throw ApiException.unprocessable("结果有效截止时间必须晚于完成时间");
        }
        return resultRepository.save(
                new ExamResult(externalNo, personId, itemCode, passed, completedAt, validUntil));
    }

    @Transactional
    public Credential issue(String idempotencyKey, String personId, String typeCode, Instant issuedAt) {
        String fingerprint = fingerprint(personId, typeCode, issuedAt);
        var replayed = credentialRepository.findByIdempotencyKey(idempotencyKey);
        if (replayed.isPresent()) {
            Credential existing = replayed.get();
            if (existing.getRequestFingerprint().equals(fingerprint)) {
                return existing;
            }
            throw ApiException.conflict("相同签发幂等键但请求内容不同");
        }

        CredentialType type = typeRepository.findByCode(typeCode)
                .orElseThrow(() -> ApiException.notFound("资质类型不存在: " + typeCode));

        Instant effectiveIssuedAt = issuedAt != null ? issuedAt : clock.instant();
        Instant expiresAt = effectiveIssuedAt.plus(type.getValidityDays(), ChronoUnit.DAYS);

        credentialRepository.findByPersonIdAndTypeCodeAndActiveKey(personId, typeCode, Credential.ACTIVE_KEY)
                .ifPresent(current -> {
                    if (current.getExpiresAt().isAfter(effectiveIssuedAt)) {
                        throw ApiException.conflict("该人员已持有当前有效的同类型凭证: " + current.getCredentialNo());
                    }
                    current.clearActiveKey();
                });

        Credential credential = new Credential(newCredentialNo(), personId, typeCode,
                effectiveIssuedAt, expiresAt, idempotencyKey, fingerprint);
        for (String itemCode : type.getRequiredItems()) {
            ExamResult latest = resultRepository
                    .findTopByPersonIdAndItemCodeAndCompletedAtLessThanEqualOrderByCompletedAtDesc(
                            personId, itemCode, effectiveIssuedAt)
                    .orElseThrow(() -> ApiException.unprocessable(
                            "缺少必需考核项目的结果: " + itemCode));
            if (!latest.isPassed()) {
                throw ApiException.unprocessable(
                        "项目 " + itemCode + " 的最新结果为未通过，不能采用更早的通过结果");
            }
            if (!latest.getValidUntil().isAfter(effectiveIssuedAt)) {
                throw ApiException.unprocessable(
                        "项目 " + itemCode + " 的最新通过结果在签发时已过期");
            }
            credential.addEvidence(new CredentialEvidence(credential, latest.getExternalNo(),
                    itemCode, latest.getCompletedAt(), latest.getValidUntil()));
        }

        try {
            return credentialRepository.saveAndFlush(credential);
        } catch (DataIntegrityViolationException e) {
            // 并发签发兜底：当前凭证唯一约束或幂等键唯一约束冲突
            throw ApiException.conflict("并发签发冲突：同一人员同一资质类型只能存在一张当前有效凭证");
        }
    }

    @Transactional
    public CredentialEvent addEvent(String credentialNo, EventType eventType, Instant effectiveAt, String reason) {
        Credential credential = credentialRepository.findByCredentialNo(credentialNo)
                .orElseThrow(() -> ApiException.notFound("凭证不存在: " + credentialNo));

        CredentialEvent last = credential.getEvents().isEmpty()
                ? null
                : credential.getEvents().get(credential.getEvents().size() - 1);

        if (last != null && last.getEventType() == EventType.REVOKE) {
            throw ApiException.unprocessable("凭证已撤销，撤销为终态，不能再登记状态事件");
        }
        switch (eventType) {
            case RESUME -> {
                if (last == null || last.getEventType() != EventType.SUSPEND) {
                    throw ApiException.unprocessable("仅暂停状态的凭证可以恢复");
                }
            }
            case SUSPEND -> {
                if (last != null && last.getEventType() == EventType.SUSPEND) {
                    throw ApiException.unprocessable("凭证已处于暂停状态");
                }
            }
            default -> {
            }
        }
        if (effectiveAt.isBefore(credential.getIssuedAt())) {
            throw ApiException.unprocessable("事件生效时间不得早于签发时间");
        }
        if (last != null && effectiveAt.isBefore(last.getEffectiveAt())) {
            throw ApiException.unprocessable("事件生效时间不得早于上一事件");
        }
        if (effectiveAt.isAfter(credential.getExpiresAt())) {
            throw ApiException.unprocessable("事件生效时间不得晚于凭证自然到期时间");
        }

        CredentialEvent event = new CredentialEvent(credential, credential.getEvents().size() + 1,
                eventType, effectiveAt, reason, clock.instant());
        credential.addEvent(event);
        if (eventType == EventType.REVOKE) {
            credential.clearActiveKey();
        }
        credentialRepository.saveAndFlush(credential);
        return event;
    }

    @Transactional(readOnly = true)
    public VerificationView verify(String credentialNo, Instant asOf) {
        Credential credential = credentialRepository.findByCredentialNo(credentialNo)
                .orElseThrow(() -> ApiException.notFound("凭证不存在: " + credentialNo));

        VerificationStatus status;
        if (asOf.isBefore(credential.getIssuedAt())) {
            status = VerificationStatus.NOT_ISSUED;
        } else {
            status = VerificationStatus.VALID;
            for (CredentialEvent event : credential.getEvents()) {
                if (!event.getEffectiveAt().isAfter(asOf)) {
                    switch (event.getEventType()) {
                        case SUSPEND -> status = VerificationStatus.SUSPENDED;
                        case RESUME -> status = VerificationStatus.VALID;
                        case REVOKE -> status = VerificationStatus.REVOKED;
                    }
                }
            }
            if (status != VerificationStatus.REVOKED && !asOf.isBefore(credential.getExpiresAt())) {
                status = VerificationStatus.EXPIRED;
            }
        }

        List<VerificationView.EvidenceSummary> evidence = credential.getEvidence().stream()
                .map(item -> new VerificationView.EvidenceSummary(item.getExternalNo(), item.getItemCode(),
                        item.getCompletedAt(), item.getValidUntil()))
                .toList();
        return new VerificationView(credential.getCredentialNo(), credential.getPersonId(),
                credential.getTypeCode(), credential.getIssuedAt(), credential.getExpiresAt(),
                asOf, status, evidence);
    }

    private String newCredentialNo() {
        return "CRD-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private String fingerprint(String personId, String typeCode, Instant issuedAt) {
        String raw = personId + '\n' + typeCode + '\n' + (issuedAt == null ? "" : issuedAt.toString());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
