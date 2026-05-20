package com.meituan.demo.backend.controller;

import com.meituan.demo.backend.model.ApiModels.LoginRequest;
import com.meituan.demo.backend.model.ApiModels.LoginResponse;
import com.meituan.demo.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/{role}/login")
    public LoginResponse login(@PathVariable String role, @Valid @RequestBody LoginRequest request) {
        return authService.login(role, request);
    }
}

