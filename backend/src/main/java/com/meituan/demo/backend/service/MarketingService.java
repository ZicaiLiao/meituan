package com.meituan.demo.backend.service;

import com.meituan.demo.backend.model.ApiModels.MembershipRulesResponse;
import com.meituan.demo.backend.model.ApiModels.RecommendationConfigResponse;
import com.meituan.demo.backend.model.DomainModels.Coupon;
import com.meituan.demo.backend.model.DomainModels.MemberProfile;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MarketingService {

    private final DemoDataStore dataStore;

    public MarketingService(DemoDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<Coupon> couponsForUser(Long userId) {
        return dataStore.userCoupons().getOrDefault(userId, List.of()).stream()
                .map(couponId -> dataStore.coupons().get(couponId))
                .collect(Collectors.toList());
    }

    public List<Coupon> couponsForShop(Long shopId) {
        return dataStore.coupons().values().stream()
                .filter(coupon -> shopId.equals(coupon.shopId()))
                .toList();
    }

    public MemberProfile membershipForUser(Long userId) {
        return dataStore.memberProfiles().get(userId);
    }

    public MembershipRulesResponse membershipRules() {
        return new MembershipRulesResponse(
                List.of("普通", "银卡", "金卡", "黑金"),
                List.of("支付每 1 元获得 1 积分", "评价订单奖励 20 积分", "会员日积分翻倍"),
                List.of("支付每 1 元获得 2 成长值", "连续活跃额外奖励 50 成长值", "活动任务奖励成长值"));
    }

    public RecommendationConfigResponse recommendationConfig() {
        return new RecommendationConfigResponse(List.of("热门召回", "附近召回", "复购召回", "活动召回"),
                new BigDecimal("1.2"), new BigDecimal("1.1"));
    }
}
