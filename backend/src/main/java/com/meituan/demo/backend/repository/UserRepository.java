package com.meituan.demo.backend.repository;

import com.meituan.demo.backend.model.DomainModels.Role;
import com.meituan.demo.backend.model.DomainModels.User;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findById(Long id) {
        return jdbcTemplate.query("SELECT * FROM users WHERE id = ?", this::mapUser, id).stream().findFirst();
    }

    public Optional<User> findByRoleAndIdentifier(Role role, String identifier) {
        return jdbcTemplate.query("""
                SELECT * FROM users
                WHERE role = ?
                  AND active = TRUE
                  AND (LOWER(username) = LOWER(?) OR LOWER(display_name) = LOWER(?))
                """, this::mapUser, role.name(), identifier, identifier).stream().findFirst();
    }

    public Optional<User> findFirstByRole(Role role) {
        return jdbcTemplate.query("SELECT * FROM users WHERE role = ? ORDER BY id LIMIT 1", this::mapUser, role.name())
                .stream()
                .findFirst();
    }

    public List<User> findByRole(Role role) {
        return jdbcTemplate.query("SELECT * FROM users WHERE role = ? ORDER BY id", this::mapUser, role.name());
    }

    public Map<Long, Role> findRolesByUserIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = userIds.stream().map(unused -> "?").collect(Collectors.joining(","));
        List<Object> params = userIds.stream().map(Long.class::cast).map(Object.class::cast).toList();
        return jdbcTemplate.query("SELECT id, role FROM users WHERE id IN (" + placeholders + ")",
                        (rs, rowNum) -> Map.entry(rs.getLong("id"), Role.valueOf(rs.getString("role"))),
                        params.toArray())
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private User mapUser(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new User(
                rs.getLong("id"),
                Role.valueOf(rs.getString("role")),
                rs.getString("username"),
                rs.getString("display_name"),
                rs.getString("phone"),
                rs.getString("level"),
                rs.getObject("shop_id", Long.class),
                rs.getBoolean("active"));
    }
}

