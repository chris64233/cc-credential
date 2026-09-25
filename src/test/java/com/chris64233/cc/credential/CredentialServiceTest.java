package com.chris64233.cc.credential;

import com.chris64233.cc.credential.domain.Credential;
import com.chris64233.cc.credential.domain.EventType;
import com.chris64233.cc.credential.domain.VerificationStatus;
import com.chris64233.cc.credential.repo.CredentialRepository;
import com.chris64233.cc.credential.repo.CredentialTypeRepository;
import com.chris64233.cc.credential.repo.ExamResultRepository;
import com.chris64233.cc.credential.service.ApiException;
import com.chris64233.cc.credential.service.CredentialService;
import com.chris64233.cc.credential.service.VerificationView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CredentialServiceTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant DAY = Instant.parse("2026-01-02T00:00:00Z");

    @Autowired
    private CredentialService service;
    @Autowired
    private CredentialRepository credentialRepository;
    @Autowired
    private ExamResultRepository resultRepository;
    @Autowired
    private CredentialTypeRepository typeRepository;

    @BeforeEach
    void clean() {
        credentialRepository.deleteAll();
        resultRepository.deleteAll();
        typeRepository.deleteAll();
    }

    private void defineWeldType() {
        service.defineType("WELD", "焊工资质", 365, List.of("THEORY", "PRACTICE"));
    }

    private void recordPass(String externalNo, String personId, String itemCode,
                            Instant completedAt, Instant validUntil) {
        service.recordResult(externalNo, personId, itemCode, true, completedAt, validUntil);
    }

    private void recordAllPassing(String personId) {
        recordPass(personId + "-T1", personId, "THEORY", T0, T0.plusSeconds(400L * 24 * 3600));
        recordPass(personId + "-P1", personId, "PRACTICE", T0, T0.plusSeconds(400L * 24 * 3600));
    }

    @Test
    void issueSucceedsWithEvidenceSnapshotAndComputedExpiry() {
        defineWeldType();
        recordAllPassing("p1");

        Credential credential = service.issue("idem-1", "p1", "WELD", DAY);

        assertThat(credential.getCredentialNo()).startsWith("CRD-");
        assertThat(credential.getIssuedAt()).isEqualTo(DAY);
        assertThat(credential.getExpiresAt()).isEqualTo(DAY.plusSeconds(365L * 24 * 3600));
        assertThat(credential.getEvidence()).hasSize(2);
        assertThat(credential.getEvidence())
                .extracting("externalNo")
                .containsExactlyInAnyOrder("p1-T1", "p1-P1");
    }

    @Test
    void issueFailsWhenRequiredItemMissing() {
        defineWeldType();
        recordPass("only-theory", "p1", "THEORY", T0, T0.plusSeconds(400L * 24 * 3600));

        assertThatThrownBy(() -> service.issue("idem-1", "p1", "WELD", DAY))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(ex.getMessage()).contains("PRACTICE");
                });
    }

    @Test
    void issueFailsWhenLatestResultIsFailureOverridingEarlierPass() {
        defineWeldType();
        recordAllPassing("p1");
        service.recordResult("p1-T2-fail", "p1", "THEORY", false,
                T0.plusSeconds(3600), T0.plusSeconds(400L * 24 * 3600));

        assertThatThrownBy(() -> service.issue("idem-1", "p1", "WELD", DAY))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(ex.getMessage()).contains("未通过");
                });
    }

    @Test
    void issueFailsWhenLatestPassExpiredAtIssueTime() {
        defineWeldType();
        recordPass("expired-theory", "p1", "THEORY", T0, T0.plusSeconds(3600));
        recordPass("ok-practice", "p1", "PRACTICE", T0, T0.plusSeconds(400L * 24 * 3600));

        assertThatThrownBy(() -> service.issue("idem-1", "p1", "WELD", DAY))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(ex.getMessage()).contains("过期");
                });
    }

    @Test
    void issueIgnoresResultsCompletedAfterIssueTime() {
        defineWeldType();
        recordAllPassing("p1");
        // 签发时点之后的更新结果不应被采用
        recordPass("future-theory", "p1", "THEORY",
                DAY.plusSeconds(3600), DAY.plusSeconds(400L * 24 * 3600));

        Credential credential = service.issue("idem-1", "p1", "WELD", DAY);

        assertThat(credential.getEvidence())
                .extracting("externalNo")
                .containsExactlyInAnyOrder("p1-T1", "p1-P1");
    }

    @Test
    void idempotentReplayReturnsOriginalAndDifferentContentConflicts() {
        defineWeldType();
        recordAllPassing("p1");
        recordAllPassing("p2");

        Credential first = service.issue("idem-1", "p1", "WELD", DAY);
        Credential replay = service.issue("idem-1", "p1", "WELD", DAY);

        assertThat(replay.getCredentialNo()).isEqualTo(first.getCredentialNo());
        assertThat(credentialRepository.count()).isEqualTo(1);

        assertThatThrownBy(() -> service.issue("idem-1", "p2", "WELD", DAY))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void secondCurrentCredentialRejectedUntilRevokedOrExpired() {
        defineWeldType();
        recordAllPassing("p1");
        Credential first = service.issue("idem-1", "p1", "WELD", DAY);

        assertThatThrownBy(() -> service.issue("idem-2", "p1", "WELD", DAY))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        service.addEvent(first.getCredentialNo(), EventType.REVOKE,
                DAY.plusSeconds(3600), "伪造材料");

        Credential second = service.issue("idem-3", "p1", "WELD", DAY.plusSeconds(7200));
        assertThat(second.getCredentialNo()).isNotEqualTo(first.getCredentialNo());
    }

    @Test
    void concurrentIssueProducesOnlyOneCurrentCredential() throws Exception {
        defineWeldType();
        recordAllPassing("p1");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        AtomicInteger conflicts = new AtomicInteger();
        Runnable task = () -> {
            try {
                gate.await();
                service.issue("idem-" + Thread.currentThread().getName(), "p1", "WELD", DAY);
            } catch (ApiException ex) {
                if (ex.getStatus() == HttpStatus.CONFLICT) {
                    conflicts.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        Future<?> f1 = pool.submit(task);
        Future<?> f2 = pool.submit(task);
        gate.countDown();
        f1.get(30, TimeUnit.SECONDS);
        f2.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(credentialRepository.findAll()).hasSize(1);
        assertThat(conflicts.get()).isEqualTo(1);
    }

    @Test
    @Transactional
    void eventStateMachineAndTimeRulesEnforced() {
        defineWeldType();
        recordAllPassing("p1");
        Credential credential = service.issue("idem-1", "p1", "WELD", DAY);
        String no = credential.getCredentialNo();
        Instant expiresAt = credential.getExpiresAt();

        // 恢复仅允许暂停状态
        assertThatThrownBy(() -> service.addEvent(no, EventType.RESUME, DAY.plusSeconds(3600), "r"))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        // 生效时间不得早于签发时间
        assertThatThrownBy(() -> service.addEvent(no, EventType.SUSPEND, DAY.minusSeconds(1), "r"))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        // 生效时间不得晚于自然到期
        assertThatThrownBy(() -> service.addEvent(no, EventType.SUSPEND, expiresAt.plusSeconds(1), "r"))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        service.addEvent(no, EventType.SUSPEND, DAY.plusSeconds(3600), "例行检查");
        // 重复暂停不允许
        assertThatThrownBy(() -> service.addEvent(no, EventType.SUSPEND, DAY.plusSeconds(7200), "r"))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        // 事件生效时间不得早于上一事件
        assertThatThrownBy(() -> service.addEvent(no, EventType.RESUME, DAY.plusSeconds(1800), "r"))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        service.addEvent(no, EventType.RESUME, DAY.plusSeconds(7200), "检查通过");
        service.addEvent(no, EventType.REVOKE, DAY.plusSeconds(10800), "违规");
        // 撤销为终态
        assertThatThrownBy(() -> service.addEvent(no, EventType.SUSPEND, DAY.plusSeconds(14400), "r"))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        Credential reloaded = credentialRepository.findByCredentialNo(no).orElseThrow();
        assertThat(reloaded.getEvents()).hasSize(3);
        assertThat(reloaded.isActiveMarked()).isFalse();
    }

    @Test
    void verifyReturnsHistoricalStatusAndEvidenceSummary() {
        defineWeldType();
        recordAllPassing("p1");
        Credential credential = service.issue("idem-1", "p1", "WELD", DAY);
        String no = credential.getCredentialNo();
        Instant suspendAt = DAY.plusSeconds(10L * 24 * 3600);
        Instant resumeAt = DAY.plusSeconds(20L * 24 * 3600);
        Instant revokeAt = DAY.plusSeconds(30L * 24 * 3600);
        service.addEvent(no, EventType.SUSPEND, suspendAt, "抽查");
        service.addEvent(no, EventType.RESUME, resumeAt, "复核通过");
        service.addEvent(no, EventType.REVOKE, revokeAt, "造假");

        assertThat(service.verify(no, DAY.minusSeconds(1)).status())
                .isEqualTo(VerificationStatus.NOT_ISSUED);
        assertThat(service.verify(no, DAY).status())
                .isEqualTo(VerificationStatus.VALID);
        assertThat(service.verify(no, suspendAt).status())
                .isEqualTo(VerificationStatus.SUSPENDED);
        assertThat(service.verify(no, resumeAt).status())
                .isEqualTo(VerificationStatus.VALID);
        assertThat(service.verify(no, revokeAt).status())
                .isEqualTo(VerificationStatus.REVOKED);
        // 撤销为终态：即使过了自然到期日仍为已撤销
        assertThat(service.verify(no, credential.getExpiresAt().plusSeconds(3600)).status())
                .isEqualTo(VerificationStatus.REVOKED);

        VerificationView view = service.verify(no, resumeAt);
        assertThat(view.evidence()).hasSize(2);
        assertThat(view.evidence())
                .extracting(VerificationView.EvidenceSummary::externalNo)
                .containsExactlyInAnyOrder("p1-T1", "p1-P1");
    }

    @Test
    void verifyReturnsExpiredAfterNaturalExpiry() {
        defineWeldType();
        recordAllPassing("p1");
        Credential credential = service.issue("idem-1", "p1", "WELD", DAY);

        assertThat(service.verify(credential.getCredentialNo(),
                credential.getExpiresAt().minusSeconds(1)).status())
                .isEqualTo(VerificationStatus.VALID);
        assertThat(service.verify(credential.getCredentialNo(), credential.getExpiresAt()).status())
                .isEqualTo(VerificationStatus.EXPIRED);
    }

    @Test
    void duplicateExternalResultNoRejected() {
        defineWeldType();
        recordPass("dup-no", "p1", "THEORY", T0, T0.plusSeconds(400L * 24 * 3600));

        assertThatThrownBy(() -> recordPass("dup-no", "p1", "THEORY", T0,
                T0.plusSeconds(400L * 24 * 3600)))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }
}
