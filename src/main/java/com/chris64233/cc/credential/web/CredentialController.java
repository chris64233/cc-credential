package com.chris64233.cc.credential.web;

import com.chris64233.cc.credential.service.CredentialService;
import com.chris64233.cc.credential.web.dto.CredentialResponse;
import com.chris64233.cc.credential.web.dto.EventRequest;
import com.chris64233.cc.credential.web.dto.EventResponse;
import com.chris64233.cc.credential.web.dto.IssueRequest;
import com.chris64233.cc.credential.web.dto.VerifyResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/credentials")
public class CredentialController {

    private final CredentialService service;

    public CredentialController(CredentialService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CredentialResponse issue(@Valid @RequestBody IssueRequest request) {
        return CredentialResponse.from(service.issue(request.idempotencyKey(),
                request.personId(), request.typeCode(), request.issuedAt()));
    }

    @PostMapping("/{credentialNo}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse addEvent(@PathVariable String credentialNo,
                                  @Valid @RequestBody EventRequest request) {
        return EventResponse.from(service.addEvent(credentialNo, request.type(),
                request.effectiveAt(), request.reason()), credentialNo);
    }

    @GetMapping("/{credentialNo}/verify")
    public VerifyResponse verify(@PathVariable String credentialNo,
                                 @RequestParam(required = false) Instant asOf) {
        return VerifyResponse.from(service.verify(credentialNo,
                asOf != null ? asOf : Instant.now()));
    }
}
