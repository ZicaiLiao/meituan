package com.meituan.demo.backend.service;

import com.meituan.demo.backend.model.ApiModels.LoginRequest;
import com.meituan.demo.backend.model.ApiModels.LoginResponse;
import com.meituan.demo.backend.model.DomainModels.Role;
import com.meituan.demo.backend.model.DomainModels.User;
import com.meituan.demo.backend.repository.UserRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LoginResponse login(String roleSegment, LoginRequest request) {
        Role role = Role.valueOf(roleSegment.toUpperCase(Locale.ROOT));
        User user = userRepository.findByRoleAndIdentifier(role, request.username())
                .or(() -> userRepository.findFirstByRole(role))
                .orElseThrow(() -> new IllegalArgumentException("User not found for role " + role));

        String accessToken = "demo-" + role.name().toLowerCase(Locale.ROOT) + "-" + user.id();
        String refreshToken = accessToken + "-refresh";
        return new LoginResponse(accessToken, refreshToken, user);
    }
}
