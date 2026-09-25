package com.chris64233.cc.credential.web;

import com.chris64233.cc.credential.domain.CredentialType;
import com.chris64233.cc.credential.service.CredentialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/credential-types")
public class CredentialTypeController {

    private final CredentialService credentialService;

    public CredentialTypeController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CredentialType define(@Valid @RequestBody DefineTypeRequest request) {
        return credentialService.defineType(request.code(), request.name(),
                request.validityDays(), request.requiredItems());
    }
}
