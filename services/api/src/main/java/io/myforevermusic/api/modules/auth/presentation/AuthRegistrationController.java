package io.myforevermusic.api.modules.auth.presentation;

import io.myforevermusic.api.modules.auth.application.AuthRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthRegistrationController {

    private final AuthRegistrationService authRegistrationService;

    public AuthRegistrationController(AuthRegistrationService authRegistrationService) {
        this.authRegistrationService = authRegistrationService;
    }

    @Operation(summary = "Register a new user account and capture the initial streaming platform choice")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthRegistrationResponse register(@Valid @RequestBody AuthRegistrationRequest request) {
        return authRegistrationService.register(request);
    }
}
