package com.chris64233.cc.credential.web;

import com.chris64233.cc.credential.domain.ExamResult;
import com.chris64233.cc.credential.service.CredentialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exam-results")
public class ExamResultController {

    private final CredentialService credentialService;

    public ExamResultController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExamResult record(@Valid @RequestBody RecordResultRequest request) {
        return credentialService.recordResult(request.resultNo(), request.personId(), request.item(),
                request.passed(), request.completedAt(), request.validUntil());
    }
}
