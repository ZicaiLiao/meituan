package com.meituan.demo.backend.repository;

import com.meituan.demo.backend.model.DomainModels.Coupon;
import com.meituan.demo.backend.model.DomainModels.MemberProfile;
import com.meituan.demo.backend.persistence.PersistenceEntities.CouponEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.MemberProfileEntity;
import com.meituan.demo.backend.persistence.mapper.MarketingMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MarketingRepository {

    private final MarketingMapper marketingMapper;

    public MarketingRepository(MarketingMapper marketingMapper) {
        this.marketingMapper = marketingMapper;
    }

    public List<Coupon> findCouponsByUserId(Long userId) {
        return marketingMapper.findCouponsByUserId(userId).stream().map(this::toCoupon).toList();
    }

    public List<Coupon> findCouponsByShopId(Long shopId) {
        return marketingMapper.findCouponsByShopId(shopId).stream().map(this::toCoupon).toList();
    }

    public List<Coupon> findAllCoupons() {
        return marketingMapper.findAllCoupons().stream().map(this::toCoupon).toList();
    }

    public Optional<Coupon> findCouponById(Long couponId) {
        return Optional.ofNullable(marketingMapper.findCouponById(couponId)).map(this::toCoupon);
    }

    public Optional<MemberProfile> findMemberProfile(Long userId) {
        return Optional.ofNullable(marketingMapper.findMemberProfile(userId)).map(this::toProfile);
    }

    public List<String> findConfigValues(String configGroup) {
        return marketingMapper.findConfigsByGroup(configGroup).stream().map(entity -> entity.configValue()).toList();
    }

    public Optional<String> findConfigValue(String configGroup, String configKey) {
        return Optional.ofNullable(marketingMapper.findConfigByGroupAndKey(configGroup, configKey)).map(entity -> entity.configValue());
    }

    public boolean claimCoupon(Long userId, Long couponId) {
        int stockUpdated = marketingMapper.decreaseCouponStock(couponId);
        if (stockUpdated == 0) {
            return false;
        }
        boolean claimed = marketingMapper.claimCoupon(userId, couponId, java.time.Instant.now()) > 0;
        if (!claimed) {
            marketingMapper.increaseCouponStock(couponId);
        }
        return claimed;
    }

    public void upsertMemberProfile(Long userId, String level, int growthPoints, int rewardPoints, List<String> benefits, List<String> tasks) {
        marketingMapper.upsertMemberProfile(userId, level, growthPoints, rewardPoints, String.join("|", benefits), String.join("|", tasks));
    }

    private Coupon toCoupon(CouponEntity entity) {
        return new Coupon(
                entity.id(),
                entity.scope(),
                entity.title(),
                entity.description(),
                entity.discountAmount(),
                entity.minimumSpend(),
                entity.stock(),
                entity.validUntil(),
                entity.shopId());
    }

    private MemberProfile toProfile(MemberProfileEntity entity) {
        return new MemberProfile(
                entity.userId(),
                entity.level(),
                entity.growthPoints(),
                entity.rewardPoints(),
                splitPipes(entity.benefitsText()),
                splitPipes(entity.tasksText()));
    }

    private List<String> splitPipes(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\|")).map(String::trim).filter(item -> !item.isBlank()).toList();
    }
}
