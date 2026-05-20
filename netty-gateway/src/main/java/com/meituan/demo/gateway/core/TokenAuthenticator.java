package com.meituan.demo.gateway.core;

import com.meituan.demo.gateway.model.GatewayModels.SessionInfo;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

public class TokenAuthenticator {

    public Optional<SessionInfo> authenticate(String token) {
        String[] parts = token.split("-");
        if (parts.length != 3 || !"demo".equals(parts[0])) {
            return Optional.empty();
        }
        try {
            String role = parts[1].toUpperCase(Locale.ROOT);
            Long userId = Long.parseLong(parts[2]);
            return Optional.of(new SessionInfo(userId, role, token, Instant.now()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}

