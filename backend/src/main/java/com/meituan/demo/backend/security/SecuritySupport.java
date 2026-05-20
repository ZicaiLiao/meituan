package com.meituan.demo.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecuritySupport {

    public DemoUserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof DemoUserPrincipal principal)) {
            throw new IllegalStateException("No authenticated principal");
        }
        return principal;
    }
}

