package com.meituan.demo.backend.persistence;

import java.math.BigDecimal;
import java.time.Instant;

public final class PersistenceEntities {

    private PersistenceEntities() {
    }

    public record UserEntity(
            Long id,
            String role,
            String username,
            String displayName,
            String phone,
            String email,
            String passwordHash,
            String level,
            Long shopId,
            Boolean active,
            String avatarUrl,
            Instant createdAt,
            Instant lastLoginAt) {
    }

    public record ShopEntity(
            Long id,
            Long merchantId,
            String name,
            String category,
            BigDecimal score,
            Integer monthlySales,
            BigDecimal deliveryFee,
            Integer deliveryMinutes,
            BigDecimal averagePrice,
            Double distanceKm,
            String tags,
            String announcement,
            String status,
            String serviceModes,
            String minOrderAmount,
            Instant createdAt) {
    }

    public record ProductEntity(
            Long id,
            Long shopId,
            String name,
            String category,
            BigDecimal price,
            BigDecimal originalPrice,
            Integer stock,
            Integer monthlySales,
            String description,
            Boolean enabled,
            Instant createdAt) {
    }

    public record AddressEntity(
            Long id,
            Long userId,
            String label,
            String detail,
            String contactName,
            String phone,
            Boolean isDefault) {
    }

    public record CouponEntity(
            Long id,
            String scope,
            String title,
            String description,
            BigDecimal discountAmount,
            BigDecimal minimumSpend,
            Integer stock,
            Instant validUntil,
            Long shopId) {
    }

    public record UserCouponEntity(
            Long userId,
            Long couponId,
            Instant claimedAt) {
    }

    public record MemberProfileEntity(
            Long userId,
            String level,
            Integer growthPoints,
            Integer rewardPoints,
            String benefitsText,
            String tasksText) {
    }

    public record CartEntity(
            Long userId,
            Long productId,
            Integer quantity) {
    }

    public record OrderEntity(
            Long id,
            Long userId,
            Long shopId,
            Long riderId,
            String status,
            BigDecimal totalAmount,
            BigDecimal payableAmount,
            Long couponId,
            Long addressId,
            Instant createdAt,
            Integer reviewScore,
            String reviewContent,
            Instant reviewedAt) {
    }

    public record OrderItemEntity(
            Long id,
            Long orderId,
            Long productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice) {
    }

    public record OrderStatusLogEntity(
            Long id,
            Long orderId,
            String status,
            String note,
            Instant createdAt) {
    }

    public record ConversationEntity(
            Long id,
            String scene,
            Long orderId,
            String title) {
    }

    public record ConversationParticipantEntity(
            Long conversationId,
            Long userId,
            String userRole) {
    }

    public record MessageEntity(
            Long id,
            Long conversationId,
            Long senderId,
            String senderRole,
            String messageType,
            String content,
            Instant createdAt) {
    }

    public record SystemConfigEntity(
            Long id,
            String configGroup,
            String configKey,
            String configValue) {
    }

    public record FavoriteShopEntity(
            Long userId,
            Long shopId,
            Instant createdAt) {
    }

    public record SearchHistoryEntity(
            Long id,
            Long userId,
            String keyword,
            Instant createdAt) {
    }

    public record RiderProfileEntity(
            Long userId,
            Boolean online,
            Integer capacity,
            String vehicleType) {
    }
}
