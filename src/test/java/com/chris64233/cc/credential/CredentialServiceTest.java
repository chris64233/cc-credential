package com.chris64233.cc.credential;

import com.chris64233.cc.credential.domain.Credential;
import com.chris64233.cc.credential.domain.EventAction;
import com.chris64233.cc.credential.repo.ActiveCredentialRepository;
import com.chris64233.cc.credential.repo.CredentialEventRepository;
import com.chris64233.cc.credential.repo.CredentialRepository;
import com.chris64233.cc.credential.repo.CredentialTypeRepository;
import com.chris64233.cc.credential.repo.ExamResultRepository;
import com.chris64233.cc.credential.service.BusinessRuleException;
import com.chris64233.cc.credential.service.ConflictException;
import com.chris64233.cc.credential.service.CredentialService;
import com.chris64233.cc.credential.service.IssueCommand;
import com.chris64233.cc.credential.service.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
class CredentialServiceTest {

    private static final String TYPE = "ELECTRICIAN";
    private static final AtomicLong SEQ = new AtomicLong();

    @Autowired
    CredentialService service;
    @Autowired
    CredentialTypeRepository typeRepository;
    @Autowired
    ExamResultRepository examResultRepository;
    @Autowired
    CredentialRepository credentialRepository;
    @Autowired
    CredentialEventRepository eventRepository;
    @Autowired
    ActiveCredentialRepository activeCredentialRepository;

    @BeforeEach
    void clean() {
        eventRepository.deleteAll();
        activeCredentialRepository.deleteAll();
        credentialRepository.deleteAll();
        examResultRepository.deleteAll();
        typeRepository.deleteAll();
    }

    private String nextId(String prefix) {
        return prefix + "-" + SEQ.incrementAndGet();
    }

    private void defineType() {
        service.defineType(TYPE, "电工", 365, Set.of("THEORY", "PRACTICE"));
    }

    private void passResult(String personId, String item) {
        Instant now = Instant.now();
        service.recordResult(nextId("RES"), personId, item, true,
                now.minus(1, ChronoUnit.HOURS), now.plus(180, ChronoUnit.DAYS));
    }

    private Credential issue(String key, String personId) {
        return service.issue(new IssueCommand(key, personId, TYPE));
    }

    @Test
    void issueSucceedsWithAllRequiredEvidenceAndSnapshot() {
        defineType();
        String person = nextId("P");
        passResult(person, "THEORY");
        passResult(person, "PRACTICE");

        Credential credential = issue(nextId("IK"), person);

        assertThat(credential.getCredentialNo()).startsWith("CRD-");
        assertThat(credential.getExpiresAt())
                .isCloseTo(credential.getIssuedAt().plus(365, ChronoUnit.DAYS), within(2, ChronoUnit.SECONDS));
        assertThat(credential.getEvidences()).hasSize(2);
        assertThat(credential.getEvidences())
                .allSatisfy(e -> assertThat(e.getResultNo()).startsWith("RES-"));
        assertThat(credential.getEvidences())
                .extracting("item")
                .containsExactlyInAnyOrder("THEORY", "PRACTICE");
    }

    @Test
    void issueFailsWhenAnyRequiredItemMissing() {
        defineType();
        String person = nextId("P");
        passResult(person, "THEORY");

        assertThatThrownBy(() -> issue(nextId("IK"), person))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PRACTICE");
    }

    @Test
    void issueFailsWhenLatestResultIsFailureCoveringEarlierPass() {
        defineType();
        String person = nextId("P");
        passResult(person, "THEORY");
        passResult(person, "PRACTICE");
        Instant now = Instant.now();
        service.recordResult(nextId("RES"), person, "PRACTICE", false,
                now.minus(10, ChronoUnit.MINUTES), now.plus(180, ChronoUnit.DAYS));

        assertThatThrownBy(() -> issue(nextId("IK"), person))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("未通过");
    }

    @Test
    void issueFailsWhenLatestResultExpiredAtIssuance() {
        defineType();
        String person = nextId("P");
        passResult(person, "THEORY");
        Instant now = Instant.now();
        service.recordResult(nextId("RES"), person, "PRACTICE", true,
                now.minus(400, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS));

        assertThatThrownBy(() -> issue(nextId("IK"), person))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("已过期");
    }

    @Test
    void issueUsesLatestValidPassNotOlderOnes() {
        defineType();
        String person = nextId("P");
        passResult(person, "THEORY");
        Instant now = Instant.now();
        service.recordResult("OLD-PASS", person, "PRACTICE", true,
                now.minus(300, ChronoUnit.DAYS), now.plus(10, ChronoUnit.DAYS));
        service.recordResult("NEW-PASS", person, "PRACTICE", true,
                now.minus(1, ChronoUnit.HOURS), now.plus(180, ChronoUnit.DAYS));

        Credential credential = issue(nextId("IK"), person);

        assertThat(credential.getEvidences())
                .filteredOn(e -> e.getItem().equals("PRACTICE"))
                .singleElement()
                .extracting("resultNo")
                .isEqualTo("NEW-PASS");
    }

    @Test
    void idempotentReplayReturnsOriginalAndDifferentContentConflicts() {
        defineType();
        String person = nextId("P");
        passResult(person, "THEORY");
        passResult(person, "PRACTICE");
        String key = nextId("IK");

        Credential first = issue(key, person);
        Credential replay = issue(key, person);

        assertThat(replay.getCredentialNo()).isEqualTo(first.getCredentialNo());
        assertThat(credentialRepository.findByPersonIdAndTypeCode(person, TYPE)).hasSize(1);

        assertThatThrownBy(() -> issue(key, nextId("P")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void secondIssueWhileCurrentCredentialValidConflicts() {
        defineType();
        String person = nextId("P");
        passResult(person, "THEORY");
        passResult(person, "PRACTICE");
        issue(nextId("IK"), person);

        assertThatThrownBy(() -> issue(nextId("IK"), person))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("当前有效凭证");
    }

    @Test
    void concurrentIssuanceProducesExactlyOneCurrentCredential() throws Exception {
        defineType();
        String person = nextId("P");
        passResult(person, "THEORY");
        passResult(person, "PRACTICE");

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            String key = nextId("IK");
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    return issue(key, person).getCredentialNo();
                } catch (ConflictException e) {
                    return "CONFLICT";
                }
            }));
        }
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<String> outcomes = new ArrayList<>();
        for (Future<String> future : futures) {
            outcomes.add(future.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        List<String> issued = outcomes.stream().filter(o -> !o.equals("CONFLICT")).distinct().toList();
        assertThat(issued).hasSize(1);
        assertThat(credentialRepository.findByPersonIdAndTypeCode(person, TYPE)).hasSize(1);
        assertThat(activeCredentialRepository.findByPersonIdAndTypeCode(person, TYPE)).isPresent();
    }

    @Test
    void statusEventsFollowLifecycleRules() {
        defineType();
        String person = nextId("P");
        passResult(person, "THEORY");
        passResult(person, "PRACTICE");
        Credential credential = issue(nextId("IK"), person);
        String no = credential.getCredentialNo();
        Instant t0 = credential.getIssuedAt();

        assertThatThrownBy(() -> service.addEvent(no, EventAction.RESUME, t0.plusSeconds(60), "过早恢复"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("暂停");

        service.addEvent(no, EventAction.SUSPEND, t0.plusSeconds(60), "例行检查");
        assertThatThrownBy(() -> service.addEvent(no, EventAction.SUSPEND, t0.plusSeconds(120), "重复暂停"))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.addEvent(no, EventAction.RESUME, t0.plusSeconds(30), "时间回退"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("上一事件");
        assertThatThrownBy(() -> service.addEvent(no, EventAction.RESUME,
                credential.getExpiresAt().plusSeconds(60), "超期"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("到期");

        service.addEvent(no, EventAction.RESUME, t0.plusSeconds(120), "检查通过");
        service.addEvent(no, EventAction.REVOKE, t0.plusSeconds(180), "造假");

        assertThatThrownBy(() -> service.addEvent(no, EventAction.SUSPEND, t0.plusSeconds(240), "终态后操作"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("终态");
    }

    @Test
    void verifyReconstructsHistoricalStatusFromEvents() {
        defineType();
        String person = nextId("P");
        passResult(person, "THEORY");
        passResult(person, "PRACTICE");
        Credential credential = issue(nextId("IK"), person);
        String no = credential.getCredentialNo();
        Instant t0 = credential.getIssuedAt();

        service.addEvent(no, EventAction.SUSPEND, t0.plus(1, ChronoUnit.HOURS), "检查");
        service.addEvent(no, EventAction.RESUME, t0.plus(2, ChronoUnit.HOURS), "恢复");
        service.addEvent(no, EventAction.REVOKE, t0.plus(3, ChronoUnit.HOURS), "撤销");

        assertThat(service.verify(no, t0.minusSeconds(1)).status())
                .isEqualTo(VerificationStatus.NOT_YET_VALID);
        assertThat(service.verify(no, t0.plus(30, ChronoUnit.MINUTES)).status())
                .isEqualTo(VerificationStatus.ACTIVE);
        assertThat(service.verify(no, t0.plus(90, ChronoUnit.MINUTES)).status())
                .isEqualTo(VerificationStatus.SUSPENDED);
        assertThat(service.verify(no, t0.plus(150, ChronoUnit.MINUTES)).status())
                .isEqualTo(VerificationStatus.ACTIVE);
        assertThat(service.verify(no, t0.plus(200, ChronoUnit.MINUTES)).status())
                .isEqualTo(VerificationStatus.REVOKED);
        // 撤销为终态，即使超过自然到期时间仍为 REVOKED
        assertThat(service.verify(no, credential.getExpiresAt().plus(1, ChronoUnit.DAYS)).status())
                .isEqualTo(VerificationStatus.REVOKED);
    }

    @Test
    void verifyReturnsExpiredAfterNaturalExpiry() {
        defineType();
        String person = nextId("P");
        passResult(person, "THEORY");
        passResult(person, "PRACTICE");
        Credential credential = issue(nextId("IK"), person);

        assertThat(service.verify(credential.getCredentialNo(),
                credential.getExpiresAt().plusSeconds(1)).status())
                .isEqualTo(VerificationStatus.EXPIRED);
        assertThat(service.verify(credential.getCredentialNo(),
                credential.getExpiresAt().minusSeconds(1)).status())
                .isEqualTo(VerificationStatus.ACTIVE);
    }

    @Test
    void verifyIncludesEvidenceSummarySnapshot() {
        defineType();
        String person = nextId("P");
        passResult(person, "THEORY");
        passResult(person, "PRACTICE");
        Credential credential = issue(nextId("IK"), person);

        var view = service.verify(credential.getCredentialNo(), Instant.now());

        assertThat(view.status()).isEqualTo(VerificationStatus.ACTIVE);
        assertThat(view.evidence()).hasSize(2);
        assertThat(view.evidence())
                .extracting("item")
                .containsExactlyInAnyOrder("THEORY", "PRACTICE");
        assertThat(view.evidence())
                .allSatisfy(e -> {
                    assertThat(e.resultNo()).startsWith("RES-");
                    assertThat(e.validUntil()).isAfter(e.completedAt());
                });
    }

    @Test
    void reissueAllowedAfterRevoke() {
        defineType();
        String person = nextId("P");
        passResult(person, "THEORY");
        passResult(person, "PRACTICE");
        Credential first = issue(nextId("IK"), person);
        service.addEvent(first.getCredentialNo(), EventAction.REVOKE,
                first.getIssuedAt().plusSeconds(60), "资格取消");

        Credential second = issue(nextId("IK"), person);

        assertThat(second.getCredentialNo()).isNotEqualTo(first.getCredentialNo());
        assertThat(activeCredentialRepository.findByPersonIdAndTypeCode(person, TYPE))
                .isPresent()
                .get()
                .extracting("credentialNo")
                .isEqualTo(second.getCredentialNo());
    }
}
