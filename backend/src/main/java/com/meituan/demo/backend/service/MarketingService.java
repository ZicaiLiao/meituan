package com.meituan.demo.backend.service;

import com.meituan.demo.backend.model.ApiModels.MembershipRulesResponse;
import com.meituan.demo.backend.model.ApiModels.RecommendationConfigResponse;
import com.meituan.demo.backend.model.DomainModels.Coupon;
import com.meituan.demo.backend.model.DomainModels.MemberProfile;
import com.meituan.demo.backend.repository.MarketingRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

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

    public MemberProfile membershipForUser(Long userId) {
        return marketingRepository.findMemberProfile(userId)
                .orElseThrow(() -> new IllegalArgumentException("Member profile not found for user " + userId));
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
}
