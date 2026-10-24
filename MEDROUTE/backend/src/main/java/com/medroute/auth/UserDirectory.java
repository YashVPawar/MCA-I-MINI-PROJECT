package com.medroute.auth;

import java.util.Map;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserDirectory {
    private final Map<String, Account> accounts;
    private final PasswordEncoder passwordEncoder;

    public UserDirectory(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.accounts = Map.of(
                "admin", account("admin", "Operations Admin", "ADMIN", null, "admin123"),
                "ambulance", account("ambulance", "Ambulance Unit 01", "AMBULANCE", null, "ambulance123"),
                "ruby", account("ruby", "Ruby Hall ER", "HOSPITAL", "ruby", "hospital123"));
    }

    public Optional<AppPrincipal> authenticate(String username, String password) {
        Account account = accounts.get(username.toLowerCase());
        if (account == null || !passwordEncoder.matches(password, account.passwordHash())) {
            return Optional.empty();
        }
        return Optional.of(new AppPrincipal(account.username(), account.displayName(), account.role(), account.hospitalId()));
    }

    private Account account(String username, String name, String role, String hospitalId, String password) {
        return new Account(username, name, role, hospitalId, passwordEncoder.encode(password));
    }

    private record Account(String username, String displayName, String role, String hospitalId, String passwordHash) {
    }
}