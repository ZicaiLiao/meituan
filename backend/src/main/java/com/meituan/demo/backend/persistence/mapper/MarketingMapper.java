package com.meituan.demo.backend.persistence.mapper;

import com.meituan.demo.backend.persistence.PersistenceEntities.CouponEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.MemberProfileEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.SystemConfigEntity;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface MarketingMapper {

    @Select("""
            SELECT c.id, c.scope, c.title, c.description, c.discount_amount, c.minimum_spend, c.stock, c.valid_until, c.shop_id
            FROM coupons c
            INNER JOIN user_coupons uc ON uc.coupon_id = c.id
            WHERE uc.user_id = #{userId}
            ORDER BY c.valid_until, c.id
            """)
    List<CouponEntity> findCouponsByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT id, scope, title, description, discount_amount, minimum_spend, stock, valid_until, shop_id
            FROM coupons
            WHERE shop_id = #{shopId}
            ORDER BY valid_until, id
            """)
    List<CouponEntity> findCouponsByShopId(@Param("shopId") Long shopId);

    @Select("""
            SELECT id, scope, title, description, discount_amount, minimum_spend, stock, valid_until, shop_id
            FROM coupons
            ORDER BY valid_until, id
            """)
    List<CouponEntity> findAllCoupons();

    @Select("""
            SELECT id, scope, title, description, discount_amount, minimum_spend, stock, valid_until, shop_id
            FROM coupons
            WHERE id = #{couponId}
            """)
    CouponEntity findCouponById(@Param("couponId") Long couponId);

    @Select("""
            SELECT user_id, level, growth_points, reward_points, benefits_text, tasks_text
            FROM member_profiles
            WHERE user_id = #{userId}
            """)
    MemberProfileEntity findMemberProfile(@Param("userId") Long userId);

    @Select("""
            SELECT id, config_group, config_key, config_value
            FROM system_configs
            WHERE config_group = #{configGroup}
            ORDER BY id
            """)
    List<SystemConfigEntity> findConfigsByGroup(@Param("configGroup") String configGroup);

    @Select("""
            SELECT id, config_group, config_key, config_value
            FROM system_configs
            WHERE config_group = #{configGroup}
              AND config_key = #{configKey}
            LIMIT 1
            """)
    SystemConfigEntity findConfigByGroupAndKey(@Param("configGroup") String configGroup, @Param("configKey") String configKey);

    @Insert("""
            INSERT IGNORE INTO user_coupons (user_id, coupon_id, claimed_at)
            VALUES (#{userId}, #{couponId}, #{claimedAt})
            """)
    int claimCoupon(@Param("userId") Long userId, @Param("couponId") Long couponId, @Param("claimedAt") Instant claimedAt);

    @Update("UPDATE coupons SET stock = stock - 1 WHERE id = #{couponId} AND stock > 0")
    int decreaseCouponStock(@Param("couponId") Long couponId);

    @Update("UPDATE coupons SET stock = stock + 1 WHERE id = #{couponId}")
    int increaseCouponStock(@Param("couponId") Long couponId);

    @Insert("""
            INSERT INTO coupons (scope, title, description, discount_amount, minimum_spend, stock, valid_until, shop_id)
            VALUES (#{scope}, #{title}, #{description}, #{discountAmount}, #{minimumSpend}, #{stock}, #{validUntil}, #{shopId})
            """)
    int insertCoupon(
            @Param("scope") String scope,
            @Param("title") String title,
            @Param("description") String description,
            @Param("discountAmount") java.math.BigDecimal discountAmount,
            @Param("minimumSpend") java.math.BigDecimal minimumSpend,
            @Param("stock") int stock,
            @Param("validUntil") Instant validUntil,
            @Param("shopId") Long shopId);

    @Select("SELECT LAST_INSERT_ID()")
    Long lastInsertId();

    @Update("""
            UPDATE member_profiles
            SET level = #{level},
                growth_points = #{growthPoints},
                reward_points = #{rewardPoints},
                benefits_text = #{benefitsText},
                tasks_text = #{tasksText}
            WHERE user_id = #{userId}
            """)
    int updateMemberProfile(
            @Param("userId") Long userId,
            @Param("level") String level,
            @Param("growthPoints") int growthPoints,
            @Param("rewardPoints") int rewardPoints,
            @Param("benefitsText") String benefitsText,
            @Param("tasksText") String tasksText);

    @Insert("""
            INSERT INTO member_profiles (user_id, level, growth_points, reward_points, benefits_text, tasks_text)
            VALUES (#{userId}, #{level}, #{growthPoints}, #{rewardPoints}, #{benefitsText}, #{tasksText})
            ON DUPLICATE KEY UPDATE
              level = VALUES(level),
              growth_points = VALUES(growth_points),
              reward_points = VALUES(reward_points),
              benefits_text = VALUES(benefits_text),
              tasks_text = VALUES(tasks_text)
            """)
    int upsertMemberProfile(
            @Param("userId") Long userId,
            @Param("level") String level,
            @Param("growthPoints") int growthPoints,
            @Param("rewardPoints") int rewardPoints,
            @Param("benefitsText") String benefitsText,
            @Param("tasksText") String tasksText);
}
