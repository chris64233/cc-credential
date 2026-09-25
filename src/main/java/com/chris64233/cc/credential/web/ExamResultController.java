package com.chris64233.cc.credential.web;

import com.chris64233.cc.credential.service.CredentialService;
import com.chris64233.cc.credential.web.dto.RecordResultRequest;
import com.chris64233.cc.credential.web.dto.ResultResponse;
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

    private final CredentialService service;

    public ExamResultController(CredentialService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResultResponse record(@Valid @RequestBody RecordResultRequest request) {
        return ResultResponse.from(service.recordResult(request.externalNo(), request.personId(),
                request.itemCode(), request.passed(), request.completedAt(), request.validUntil()));
    }
}
