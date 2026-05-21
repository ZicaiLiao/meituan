package com.meituan.demo.backend.repository;

import com.meituan.demo.backend.model.DomainModels.Coupon;
import com.meituan.demo.backend.model.DomainModels.MemberProfile;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MarketingRepository {

    private final JdbcTemplate jdbcTemplate;

    public MarketingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Coupon> findCouponsByUserId(Long userId) {
        return jdbcTemplate.query("""
                SELECT c.* FROM coupons c
                INNER JOIN user_coupons uc ON uc.coupon_id = c.id
                WHERE uc.user_id = ?
                ORDER BY c.valid_until, c.id
                """, this::mapCoupon, userId);
    }

    public List<Coupon> findCouponsByShopId(Long shopId) {
        return jdbcTemplate.query("SELECT * FROM coupons WHERE shop_id = ? ORDER BY valid_until, id", this::mapCoupon, shopId);
    }

    public List<Coupon> findAllCoupons() {
        return jdbcTemplate.query("SELECT * FROM coupons ORDER BY valid_until, id", this::mapCoupon);
    }

    public Optional<Coupon> findCouponById(Long couponId) {
        return jdbcTemplate.query("SELECT * FROM coupons WHERE id = ?", this::mapCoupon, couponId).stream().findFirst();
    }

    public Optional<MemberProfile> findMemberProfile(Long userId) {
        return jdbcTemplate.query("SELECT * FROM member_profiles WHERE user_id = ?", this::mapProfile, userId).stream().findFirst();
    }

    public List<String> findConfigValues(String configGroup) {
        return jdbcTemplate.query("SELECT config_value FROM system_configs WHERE config_group = ? ORDER BY id",
                (rs, rowNum) -> rs.getString("config_value"), configGroup);
    }

    public Optional<String> findConfigValue(String configGroup, String configKey) {
        return jdbcTemplate.query("""
                SELECT config_value FROM system_configs
                WHERE config_group = ? AND config_key = ?
                LIMIT 1
                """, (rs, rowNum) -> rs.getString("config_value"), configGroup, configKey).stream().findFirst();
    }

    private Coupon mapCoupon(ResultSet rs, int rowNum) throws SQLException {
        return new Coupon(
                rs.getLong("id"),
                rs.getString("scope"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("minimum_spend"),
                rs.getInt("stock"),
                rs.getTimestamp("valid_until").toInstant(),
                rs.getObject("shop_id", Long.class));
    }

    private MemberProfile mapProfile(ResultSet rs, int rowNum) throws SQLException {
        return new MemberProfile(
                rs.getLong("user_id"),
                rs.getString("level"),
                rs.getInt("growth_points"),
                rs.getInt("reward_points"),
                splitPipes(rs.getString("benefits_text")),
                splitPipes(rs.getString("tasks_text")));
    }

    private List<String> splitPipes(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\|")).map(String::trim).filter(item -> !item.isBlank()).toList();
    }
}
