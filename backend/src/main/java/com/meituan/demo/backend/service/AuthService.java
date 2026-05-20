package com.meituan.demo.backend.service;

import com.meituan.demo.backend.model.ApiModels.LoginRequest;
import com.meituan.demo.backend.model.ApiModels.LoginResponse;
import com.meituan.demo.backend.model.DomainModels.Role;
import com.meituan.demo.backend.model.DomainModels.User;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final DemoDataStore dataStore;

    public AuthService(DemoDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public LoginResponse login(String roleSegment, LoginRequest request) {
        Role role = Role.valueOf(roleSegment.toUpperCase(Locale.ROOT));
        User user = dataStore.users().values().stream()
                .filter(candidate -> candidate.role() == role)
                .filter(candidate -> candidate.username().equalsIgnoreCase(request.username())
                        || candidate.displayName().equalsIgnoreCase(request.username()))
                .findFirst()
                .orElseGet(() -> dataStore.users().values().stream()
                        .filter(candidate -> candidate.role() == role)
                        .findFirst()
                        .orElseThrow());

        String accessToken = "demo-" + role.name().toLowerCase(Locale.ROOT) + "-" + user.id();
        String refreshToken = accessToken + "-refresh";
        return new LoginResponse(accessToken, refreshToken, user);
    }
}

