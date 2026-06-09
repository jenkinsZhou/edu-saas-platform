package com.edusphere.api.service.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordService {

    private static final String BCRYPT_PREFIX = "$2";
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        if (isEncoded(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return rawPassword.equals(storedPassword);
    }

    public boolean needsUpgrade(String storedPassword) {
        return storedPassword != null && !storedPassword.isBlank() && !isEncoded(storedPassword);
    }

    private boolean isEncoded(String storedPassword) {
        return storedPassword.startsWith(BCRYPT_PREFIX);
    }
}
