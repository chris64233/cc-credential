package com.chris64233.cc.credential.web;

import com.chris64233.cc.credential.service.ApiException;
import com.chris64233.cc.credential.service.CredentialService;
import com.chris64233.cc.credential.web.dto.DefineTypeRequest;
import com.chris64233.cc.credential.web.dto.TypeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/credential-types")
public class CredentialTypeController {

    private final CredentialService service;

    public CredentialTypeController(CredentialService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TypeResponse define(@Valid @RequestBody DefineTypeRequest request) {
        return TypeResponse.from(service.defineType(request.code(), request.name(),
                request.validityDays(), request.requiredItems()));
    }

    @GetMapping("/{code}")
    public TypeResponse get(@PathVariable String code) {
        return TypeResponse.from(service.getType(code)
                .orElseThrow(() -> ApiException.notFound("资质类型不存在: " + code)));
    }
}
