package com.chris64233.cc.credential;

import com.chris64233.cc.credential.repo.ActiveCredentialRepository;
import com.chris64233.cc.credential.repo.CredentialEventRepository;
import com.chris64233.cc.credential.repo.CredentialRepository;
import com.chris64233.cc.credential.repo.CredentialTypeRepository;
import com.chris64233.cc.credential.repo.ExamResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CredentialApiTest {

    @Autowired
    MockMvc mockMvc;
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

    @Test
    void fullLifecycleOverHttp() throws Exception {
        mockMvc.perform(post("/api/credential-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"WELDER","name":"焊工","validityDays":180,
                                 "requiredItems":["SAFETY","SKILL"]}
                                """))
                .andExpect(status().isCreated());
        Instant now = Instant.now();
        recordResult("R-1", "P-1", "SAFETY", true,
                now.minus(2, ChronoUnit.HOURS), now.plus(90, ChronoUnit.DAYS));
        recordResult("R-2", "P-1", "SKILL", true,
                now.minus(1, ChronoUnit.HOURS), now.plus(90, ChronoUnit.DAYS));
        String issueBody = """
                {"idempotencyKey":"IK-1","personId":"P-1","typeCode":"WELDER"}
                """;
        String credentialNo = mockMvc.perform(post("/api/credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issueBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.credentialNo").exists())
                .andExpect(jsonPath("$.evidence.length()").value(2))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"credentialNo\":\"([^\"]+)\".*", "$1");
        mockMvc.perform(post("/api/credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issueBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialNo").value(credentialNo));
        mockMvc.perform(post("/api/credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"IK-1","personId":"P-2","typeCode":"WELDER"}
                                """))
                .andExpect(status().isConflict());
        Instant suspendAt = Instant.now();
        mockMvc.perform(post("/api/credentials/{no}/events", credentialNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"SUSPEND","effectiveAt":"%s","reason":"抽查"}
                                """.formatted(suspendAt)))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/credentials/{no}/verification", credentialNo)
                        .param("at", suspendAt.plus(1, ChronoUnit.HOURS).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.evidence[0].resultNo").exists());
        mockMvc.perform(get("/api/credentials/{no}/verification", credentialNo)
                        .param("at", suspendAt.minus(1, ChronoUnit.MILLIS).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(get("/api/credentials/{no}/verification", "CRD-MISSING"))
                .andExpect(status().isNotFound());
    }

    private void recordResult(String resultNo, String personId, String item, boolean passed,
                              Instant completedAt, Instant validUntil) throws Exception {
        mockMvc.perform(post("/api/exam-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resultNo":"%s","personId":"%s","item":"%s","passed":%s,
                                 "completedAt":"%s","validUntil":"%s"}
                                """.formatted(resultNo, personId, item, passed, completedAt, validUntil)))
                .andExpect(status().isCreated());
    }
}
