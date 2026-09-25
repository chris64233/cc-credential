package com.chris64233.cc.credential.web;

import com.chris64233.cc.credential.service.CredentialService;
import com.chris64233.cc.credential.service.IssueCommand;
import com.chris64233.cc.credential.service.VerificationView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/credentials")
public class CredentialController {

    private final CredentialService credentialService;

    public CredentialController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @PostMapping
    public ResponseEntity<CredentialResponse> issue(@Valid @RequestBody IssueCredentialRequest request) {
        boolean replay = credentialService.findByIdempotencyKey(request.idempotencyKey()).isPresent();
        var credential = credentialService.issue(
                new IssueCommand(request.idempotencyKey(), request.personId(), request.typeCode()));
        return ResponseEntity.status(replay ? HttpStatus.OK : HttpStatus.CREATED)
                .body(CredentialResponse.from(credential));
    }

    @PostMapping("/{credentialNo}/events")
    public ResponseEntity<EventResponse> addEvent(@PathVariable String credentialNo,
                                                  @Valid @RequestBody AddEventRequest request) {
        var event = credentialService.addEvent(credentialNo, request.action(),
                request.effectiveAt(), request.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(EventResponse.from(event));
    }

    @GetMapping("/{credentialNo}/verification")
    public VerificationView verify(@PathVariable String credentialNo,
                                   @RequestParam(required = false) Instant at) {
        return credentialService.verify(credentialNo, at == null ? Instant.now() : at);
    }
}
