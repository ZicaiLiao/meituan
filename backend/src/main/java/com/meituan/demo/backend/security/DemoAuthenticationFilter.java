package com.meituan.demo.backend.security;

import com.meituan.demo.backend.model.DomainModels.Role;
import java.io.IOException;
import java.util.Locale;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class DemoAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = null;
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            token = header.substring(7);
        } else if (StringUtils.hasText(request.getParameter("token"))) {
            token = request.getParameter("token");
        }
        if (StringUtils.hasText(token)) {
            DemoUserPrincipal principal = parseToken(token);
            if (principal != null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private DemoUserPrincipal parseToken(String token) {
        String[] parts = token.split("-");
        if (parts.length != 3 || !"demo".equals(parts[0])) {
            return null;
        }
        try {
            Role role = Role.valueOf(parts[1].toUpperCase(Locale.ROOT));
            Long userId = Long.parseLong(parts[2]);
            return new DemoUserPrincipal(userId, parts[1] + "-" + userId, role);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
