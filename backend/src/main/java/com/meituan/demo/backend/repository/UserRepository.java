package com.meituan.demo.backend.repository;

import com.meituan.demo.backend.model.DomainModels.Role;
import com.meituan.demo.backend.model.DomainModels.User;
import com.meituan.demo.backend.persistence.PersistenceEntities.RiderProfileEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.UserEntity;
import com.meituan.demo.backend.persistence.mapper.UserMapper;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final UserMapper userMapper;

    public UserRepository(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.findById(id)).map(this::toDomain);
    }

    public Optional<UserEntity> findEntityById(Long id) {
        return Optional.ofNullable(userMapper.findById(id));
    }

    public Optional<User> findByRoleAndIdentifier(Role role, String identifier) {
        return Optional.ofNullable(userMapper.findByRoleAndIdentifier(role.name(), identifier)).map(this::toDomain);
    }

    public Optional<UserEntity> findEntityByUsername(String username) {
        return Optional.ofNullable(userMapper.findByUsername(username));
    }

    public Optional<UserEntity> findEntityByPhone(String phone) {
        return Optional.ofNullable(userMapper.findByPhone(phone));
    }

    public Optional<User> findFirstByRole(Role role) {
        return Optional.ofNullable(userMapper.findFirstByRole(role.name())).map(this::toDomain);
    }

    public List<User> findByRole(Role role) {
        return userMapper.findByRole(role.name()).stream().map(this::toDomain).toList();
    }

    public Map<Long, Role> findRolesByUserIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.findByIds(userIds).stream()
                .collect(Collectors.toMap(UserEntity::id, entity -> Role.valueOf(entity.role())));
    }

    public User register(
            Role role,
            String username,
            String displayName,
            String phone,
            String email,
            String passwordHash,
            String level,
            Long shopId,
            String avatarUrl) {
        Instant now = Instant.now();
        userMapper.insertUser(role.name(), username, displayName, phone, email, passwordHash, level, shopId, true, avatarUrl, now, now);
        Long userId = userMapper.lastInsertId();
        return findById(userId).orElseThrow();
    }

    public void updateProfile(Long userId, String displayName, String phone, String email, String avatarUrl) {
        userMapper.updateProfile(userId, displayName, phone, email, avatarUrl);
    }

    public void updateShopId(Long userId, Long shopId) {
        userMapper.updateShopId(userId, shopId);
    }

    public void touchLogin(Long userId) {
        userMapper.updateLastLoginAt(userId, Instant.now());
    }

    public Optional<RiderProfileEntity> findRiderProfile(Long userId) {
        return Optional.ofNullable(userMapper.findRiderProfile(userId));
    }

    public void upsertRiderProfile(Long userId, boolean online, int capacity, String vehicleType) {
        userMapper.upsertRiderProfile(userId, online, capacity, vehicleType);
    }

    public void updateRiderAvailability(Long userId, boolean online, int capacity) {
        userMapper.updateRiderAvailability(userId, online, capacity);
    }

    private User toDomain(UserEntity entity) {
        return new User(
                entity.id(),
                Role.valueOf(entity.role()),
                entity.username(),
                entity.displayName(),
                entity.phone(),
                entity.email(),
                entity.level(),
                entity.shopId(),
                Boolean.TRUE.equals(entity.active()),
                entity.avatarUrl());
    }
}
