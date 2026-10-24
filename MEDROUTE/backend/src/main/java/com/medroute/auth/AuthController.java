package com.medroute.auth;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserDirectory userDirectory;
    private final JwtService jwtService;

    public AuthController(UserDirectory userDirectory, JwtService jwtService) {
        this.userDirectory = userDirectory;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid AuthRequest request) {
        AppPrincipal principal = userDirectory.authenticate(request.username(), request.password())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        return new AuthResponse(jwtService.issue(principal), "Bearer", principal.username(),
                principal.displayName(), principal.role(), principal.hospitalId());
    }

    @PostMapping("/logout")
    public Map<String, String> logout() {
        return Map.of("message", "Remove the bearer token from the client to end this session");
    }
}