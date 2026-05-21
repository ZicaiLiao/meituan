package com.meituan.demo.backend.persistence.mapper;

import com.meituan.demo.backend.persistence.PersistenceEntities.RiderProfileEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.UserEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserMapper {

    @Select("""
            SELECT id, role, username, display_name, phone, email, password_hash, level, shop_id, active, avatar_url, created_at, last_login_at
            FROM users
            WHERE id = #{id}
            """)
    UserEntity findById(@Param("id") Long id);

    @Select("""
            SELECT id, role, username, display_name, phone, email, password_hash, level, shop_id, active, avatar_url, created_at, last_login_at
            FROM users
            WHERE role = #{role}
              AND active = TRUE
              AND (LOWER(username) = LOWER(#{identifier}) OR LOWER(display_name) = LOWER(#{identifier}) OR phone = #{identifier})
            LIMIT 1
            """)
    UserEntity findByRoleAndIdentifier(@Param("role") String role, @Param("identifier") String identifier);

    @Select("""
            SELECT id, role, username, display_name, phone, email, password_hash, level, shop_id, active, avatar_url, created_at, last_login_at
            FROM users
            WHERE username = #{username}
            LIMIT 1
            """)
    UserEntity findByUsername(@Param("username") String username);

    @Select("""
            SELECT id, role, username, display_name, phone, email, password_hash, level, shop_id, active, avatar_url, created_at, last_login_at
            FROM users
            WHERE phone = #{phone}
            LIMIT 1
            """)
    UserEntity findByPhone(@Param("phone") String phone);

    @Select("""
            SELECT id, role, username, display_name, phone, email, password_hash, level, shop_id, active, avatar_url, created_at, last_login_at
            FROM users
            WHERE role = #{role}
            ORDER BY id
            LIMIT 1
            """)
    UserEntity findFirstByRole(@Param("role") String role);

    @Select("""
            SELECT id, role, username, display_name, phone, email, password_hash, level, shop_id, active, avatar_url, created_at, last_login_at
            FROM users
            WHERE role = #{role}
            ORDER BY id
            """)
    List<UserEntity> findByRole(@Param("role") String role);

    @Select("""
            <script>
            SELECT id, role, username, display_name, phone, email, password_hash, level, shop_id, active, avatar_url, created_at, last_login_at
            FROM users
            WHERE id IN
            <foreach collection='userIds' item='userId' open='(' separator=',' close=')'>
                #{userId}
            </foreach>
            </script>
            """)
    List<UserEntity> findByIds(@Param("userIds") Collection<Long> userIds);

    @Insert("""
            INSERT INTO users
            (role, username, display_name, phone, email, password_hash, level, shop_id, active, avatar_url, created_at, last_login_at)
            VALUES
            (#{role}, #{username}, #{displayName}, #{phone}, #{email}, #{passwordHash}, #{level}, #{shopId}, #{active}, #{avatarUrl}, #{createdAt}, #{lastLoginAt})
            """)
    int insertUser(
            @Param("role") String role,
            @Param("username") String username,
            @Param("displayName") String displayName,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("passwordHash") String passwordHash,
            @Param("level") String level,
            @Param("shopId") Long shopId,
            @Param("active") boolean active,
            @Param("avatarUrl") String avatarUrl,
            @Param("createdAt") Instant createdAt,
            @Param("lastLoginAt") Instant lastLoginAt);

    @Select("SELECT LAST_INSERT_ID()")
    Long lastInsertId();

    @Update("""
            UPDATE users
            SET display_name = #{displayName},
                phone = #{phone},
                email = #{email},
                avatar_url = #{avatarUrl}
            WHERE id = #{id}
            """)
    int updateProfile(
            @Param("id") Long id,
            @Param("displayName") String displayName,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("avatarUrl") String avatarUrl);

    @Update("UPDATE users SET shop_id = #{shopId} WHERE id = #{id}")
    int updateShopId(@Param("id") Long id, @Param("shopId") Long shopId);

    @Update("UPDATE users SET last_login_at = #{lastLoginAt} WHERE id = #{id}")
    int updateLastLoginAt(@Param("id") Long id, @Param("lastLoginAt") Instant lastLoginAt);

    @Select("SELECT user_id, online, capacity, vehicle_type FROM rider_profiles WHERE user_id = #{userId}")
    RiderProfileEntity findRiderProfile(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO rider_profiles (user_id, online, capacity, vehicle_type)
            VALUES (#{userId}, #{online}, #{capacity}, #{vehicleType})
            ON DUPLICATE KEY UPDATE
              online = VALUES(online),
              capacity = VALUES(capacity),
              vehicle_type = VALUES(vehicleType)
            """)
    int upsertRiderProfile(
            @Param("userId") Long userId,
            @Param("online") boolean online,
            @Param("capacity") int capacity,
            @Param("vehicleType") String vehicleType);

    @Update("""
            UPDATE rider_profiles
            SET online = #{online},
                capacity = #{capacity}
            WHERE user_id = #{userId}
            """)
    int updateRiderAvailability(
            @Param("userId") Long userId,
            @Param("online") boolean online,
            @Param("capacity") int capacity);
}
