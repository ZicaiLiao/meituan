package com.meituan.demo.backend.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DomainModels {

    private DomainModels() {
    }

    public enum Role {
        CUSTOMER, MERCHANT, RIDER, ADMIN, SUPPORT
    }

    public enum OrderStatus {
        PENDING_PAYMENT,
        PAID_WAITING_MERCHANT,
        RIDER_PENDING,
        DELIVERING,
        COMPLETED,
        CANCELLED,
        REJECTED,
        REFUNDED
    }

    public enum ConversationScene {
        CONSULTING, ORDER
    }

    public enum MessageType {
        TEXT, SYSTEM, ORDER_CARD
    }

    public record User(
            Long id,
            Role role,
            String username,
            String displayName,
            String phone,
            String email,
            String level,
            Long shopId,
            boolean active,
            String avatarUrl) {
    }

    public record Shop(
            Long id,
            Long merchantId,
            String name,
            String category,
            BigDecimal score,
            int monthlySales,
            BigDecimal deliveryFee,
            int deliveryMinutes,
            BigDecimal averagePrice,
            double distanceKm,
            List<String> tags,
            String announcement,
            String status,
            List<String> serviceModes,
            BigDecimal minOrderAmount) {
    }

    public record Product(
            Long id,
            Long shopId,
            String name,
            String category,
            BigDecimal price,
            BigDecimal originalPrice,
            int stock,
            int monthlySales,
            String description,
            boolean enabled) {
    }

    public record Address(
            Long id,
            Long userId,
            String label,
            String detail,
            String contactName,
            String phone,
            boolean isDefault) {
    }

    public record Coupon(
            Long id,
            String scope,
            String title,
            String description,
            BigDecimal discountAmount,
            BigDecimal minimumSpend,
            int stock,
            Instant validUntil,
            Long shopId) {
    }

    public record MemberProfile(
            Long userId, String level, int growthPoints, int rewardPoints, List<String> benefits, List<String> tasks) {
    }

    public record CartItem(Long productId, int quantity) {
    }

    public record OrderLine(Long productId, String productName, int quantity, BigDecimal unitPrice) {
    }

    public record OrderStatusLog(Long orderId, OrderStatus status, String note, Instant createdAt) {
    }

    public record Order(
            Long id,
            Long userId,
            Long shopId,
            Long riderId,
            OrderStatus status,
            BigDecimal totalAmount,
            BigDecimal payableAmount,
            Long couponId,
            Long addressId,
            Instant createdAt,
            List<OrderLine> items,
            Integer reviewScore,
            String reviewContent,
            Instant reviewedAt) {
    }

    public record Conversation(
            Long id,
            ConversationScene scene,
            Long orderId,
            String title,
            Set<Long> participantIds,
            Map<Long, Role> participantRoles) {
    }

    public record Message(
            Long id,
            Long conversationId,
            Long senderId,
            Role senderRole,
            MessageType type,
            String content,
            Instant createdAt) {
    }

    public record SearchResult(List<Shop> shops, List<Product> products, String appliedSort, List<String> suggestions) {
    }

    public record RecommendationBundle(List<Shop> guessYouLike, List<Shop> nearbyHot, List<Shop> similarShops) {
    }

    public record EventPayload(String event, String title, String content, Instant createdAt) {
    }
}
