package com.meituan.demo.backend.service;

import com.meituan.demo.backend.model.ApiModels.LoginRequest;
import com.meituan.demo.backend.model.ApiModels.LoginResponse;
import com.meituan.demo.backend.model.ApiModels.RegisterRequest;
import com.meituan.demo.backend.model.DomainModels.Role;
import com.meituan.demo.backend.model.DomainModels.Shop;
import com.meituan.demo.backend.model.DomainModels.User;
import com.meituan.demo.backend.persistence.PersistenceEntities.UserEntity;
import com.meituan.demo.backend.repository.CatalogRepository;
import com.meituan.demo.backend.repository.MarketingRepository;
import com.meituan.demo.backend.repository.UserRepository;
import com.meituan.demo.backend.security.JwtService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CatalogRepository catalogRepository;
    private final MarketingRepository marketingRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            CatalogRepository catalogRepository,
            MarketingRepository marketingRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.catalogRepository = catalogRepository;
        this.marketingRepository = marketingRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(String roleSegment, LoginRequest request) {
        Role role = Role.valueOf(roleSegment.toUpperCase(Locale.ROOT));
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        UserEntity user = userRepository.findEntityByUsername(request.username())
                .or(() -> userRepository.findEntityByPhone(request.username()))
                .filter(entity -> role.name().equals(entity.role()))
                .orElseThrow(() -> new IllegalArgumentException("User not found for role " + role));

        if (!Boolean.TRUE.equals(user.active())) {
            throw new IllegalStateException("Account is disabled");
        }
        if (user.passwordHash() == null || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new IllegalArgumentException("Username or password is incorrect");
        }

        userRepository.touchLogin(user.id());
        User domainUser = userRepository.findById(user.id()).orElseThrow();
        return buildLoginResponse(domainUser);
    }

    @Transactional
    public LoginResponse register(String roleSegment, RegisterRequest request) {
        Role role = Role.valueOf(roleSegment.toUpperCase(Locale.ROOT));
        validateRegistration(role, request);

        if (userRepository.findEntityByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findEntityByPhone(request.phone()).isPresent()) {
            throw new IllegalArgumentException("Phone already exists");
        }

        User created = userRepository.register(
                role,
                request.username(),
                request.displayName(),
                request.phone(),
                request.email(),
                passwordEncoder.encode(request.password()),
                role == Role.CUSTOMER ? "普通" : null,
                null,
                null);

        if (role == Role.MERCHANT) {
            Shop shop = catalogRepository.createShop(
                    created.id(),
                    request.shopName(),
                    request.shopCategory(),
                    new BigDecimal("4.00"),
                    30,
                    new BigDecimal("25.00"),
                    List.of("新店开业", "品质商家"),
                    "欢迎光临，支持到店自取和外卖配送",
                    "OPEN",
                    List.of("DELIVERY", "PICKUP"),
                    new BigDecimal("20.00"));
            userRepository.updateShopId(created.id(), shop.id());
            created = userRepository.findById(created.id()).orElseThrow();
        } else if (role == Role.RIDER) {
            userRepository.upsertRiderProfile(created.id(), true, 3, request.vehicleType());
        } else if (role == Role.CUSTOMER) {
            marketingRepository.upsertMemberProfile(
                    created.id(),
                    "普通",
                    0,
                    0,
                    List.of("新客券包", "会员中心"),
                    List.of("完成首单可升级银卡", "评价订单奖励积分"));
        }

        return buildLoginResponse(created);
    }

    private LoginResponse buildLoginResponse(User user) {
        String accessToken = jwtService.issueAccessToken(user.id(), user.username(), user.role());
        String refreshToken = jwtService.issueRefreshToken(user.id(), user.username(), user.role());
        return new LoginResponse(accessToken, refreshToken, user);
    }

    private void validateRegistration(Role role, RegisterRequest request) {
        if (request.password() == null || request.password().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (role == Role.MERCHANT && (isBlank(request.shopName()) || isBlank(request.shopCategory()))) {
            throw new IllegalArgumentException("Merchant registration requires shopName and shopCategory");
        }
        if (role == Role.RIDER && isBlank(request.vehicleType())) {
            throw new IllegalArgumentException("Rider registration requires vehicleType");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
