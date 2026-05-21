package com.meituan.demo.backend.service;

import com.meituan.demo.backend.model.ApiModels.ClaimCouponResponse;
import com.meituan.demo.backend.model.ApiModels.MembershipRulesResponse;
import com.meituan.demo.backend.model.ApiModels.RecommendationConfigResponse;
import com.meituan.demo.backend.model.DomainModels.Coupon;
import com.meituan.demo.backend.model.DomainModels.MemberProfile;
import com.meituan.demo.backend.repository.MarketingRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketingService {

    private final MarketingRepository marketingRepository;

    public MarketingService(MarketingRepository marketingRepository) {
        this.marketingRepository = marketingRepository;
    }

    public List<Coupon> couponsForUser(Long userId) {
        return marketingRepository.findCouponsByUserId(userId);
    }

    public List<Coupon> couponsForShop(Long shopId) {
        return marketingRepository.findCouponsByShopId(shopId);
    }

    public List<Coupon> allCoupons() {
        return marketingRepository.findAllCoupons();
    }

    @Transactional
    public ClaimCouponResponse claimCoupon(Long userId, Long couponId) {
        boolean success = marketingRepository.claimCoupon(userId, couponId);
        return success
                ? new ClaimCouponResponse(true, "领取成功")
                : new ClaimCouponResponse(false, "优惠券已抢完或已领取");
    }

    public MemberProfile membershipForUser(Long userId) {
        return marketingRepository.findMemberProfile(userId)
                .orElseThrow(() -> new IllegalArgumentException("Member profile not found for user " + userId));
    }

    @Transactional
    public MemberProfile rewardOrderCompleted(Long userId, BigDecimal payableAmount) {
        MemberProfile profile = currentOrDefault(userId);
        int extraPoints = payableAmount.intValue();
        int extraGrowth = payableAmount.multiply(BigDecimal.valueOf(2)).intValue();
        return saveProfile(
                userId,
                profile.growthPoints() + extraGrowth,
                profile.rewardPoints() + extraPoints,
                profile.benefits(),
                profile.tasks());
    }

    @Transactional
    public MemberProfile rewardReview(Long userId) {
        MemberProfile profile = currentOrDefault(userId);
        return saveProfile(
                userId,
                profile.growthPoints() + 20,
                profile.rewardPoints() + 20,
                profile.benefits(),
                profile.tasks());
    }

    public MembershipRulesResponse membershipRules() {
        return new MembershipRulesResponse(
                marketingRepository.findConfigValues("membership_levels"),
                marketingRepository.findConfigValues("point_rules"),
                marketingRepository.findConfigValues("growth_rules"));
    }

    public RecommendationConfigResponse recommendationConfig() {
        return new RecommendationConfigResponse(
                marketingRepository.findConfigValues("recommendation_channels"),
                new BigDecimal(marketingRepository.findConfigValue("recommendation_weights", "activity_boost").orElse("1.2")),
                new BigDecimal(marketingRepository.findConfigValue("recommendation_weights", "distance_boost").orElse("1.1")));
    }

    private MemberProfile currentOrDefault(Long userId) {
        return marketingRepository.findMemberProfile(userId)
                .orElseGet(() -> new MemberProfile(
                        userId,
                        "普通",
                        0,
                        0,
                        List.of("新客券包", "会员中心"),
                        List.of("完成首单可升级银卡", "评价订单奖励积分")));
    }

    private MemberProfile saveProfile(
            Long userId,
            int growthPoints,
            int rewardPoints,
            List<String> benefits,
            List<String> tasks) {
        String level = resolveLevel(growthPoints);
        marketingRepository.upsertMemberProfile(userId, level, growthPoints, rewardPoints, benefits, tasks);
        return membershipForUser(userId);
    }

    private String resolveLevel(int growthPoints) {
        if (growthPoints >= 3000) {
            return "黑金";
        }
        if (growthPoints >= 1500) {
            return "金卡";
        }
        if (growthPoints >= 600) {
            return "银卡";
        }
        return "普通";
    }
}
