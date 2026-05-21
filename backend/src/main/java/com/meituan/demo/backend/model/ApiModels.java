package com.meituan.demo.backend.model;

import com.meituan.demo.backend.model.DomainModels.ConversationScene;
import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class ApiModels {

    private ApiModels() {
    }

    public record LoginRequest(@NotBlank String username, String password, String phoneCode) {
    }

    public record LoginResponse(String accessToken, String refreshToken, DomainModels.User user) {
    }

    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String displayName,
            @NotBlank String phone,
            String email,
            String shopName,
            String shopCategory,
            String vehicleType) {
    }

    public record UpdateProfileRequest(
            @NotBlank String displayName,
            @NotBlank String phone,
            String email,
            String avatarUrl) {
    }

    public record AddCartItemRequest(@NotNull Long productId, @Min(1) int quantity) {
    }

    public record CreateOrderRequest(Long couponId, Long addressId) {
    }

    public record AddressUpsertRequest(
            @NotBlank String label,
            @NotBlank String detail,
            @NotBlank String contactName,
            @NotBlank String phone,
            boolean isDefault) {
    }

    public record PayOrderRequest(String paymentChannel) {
    }

    public record ClaimCouponResponse(boolean success, String message) {
    }

    public record ToggleFavoriteResponse(boolean favorite) {
    }

    public record ReviewOrderRequest(@Min(1) int score, @NotBlank String content) {
    }

    public record CreateConversationRequest(
            @NotNull ConversationScene scene, Long orderId, @NotBlank String title, @NotNull List<Long> participantIds) {
    }

    public record CreateMessageRequest(
            @NotNull Long conversationId, @NotBlank String content, DomainModels.MessageType type) {
    }

    public record SearchRebuildResponse(boolean success, String indexName, int shopCount, int productCount) {
    }

    public record RecommendationConfigResponse(List<String> channels, BigDecimal activityBoost, BigDecimal distanceBoost) {
    }

    public record MembershipRulesResponse(List<String> levels, List<String> pointRules, List<String> growthRules) {
    }

    public record ShopUpsertRequest(
            @NotBlank String name,
            @NotBlank String category,
            @NotNull BigDecimal deliveryFee,
            @Min(1) int deliveryMinutes,
            @NotNull BigDecimal averagePrice,
            @NotNull BigDecimal minOrderAmount,
            @NotNull List<String> tags,
            @NotBlank String announcement,
            @NotNull List<String> serviceModes,
            @NotBlank String status) {
    }

    public record ProductUpsertRequest(
            @NotBlank String name,
            @NotBlank String category,
            @NotNull BigDecimal price,
            @NotNull BigDecimal originalPrice,
            @Min(0) int stock,
            @NotBlank String description,
            boolean enabled) {
    }

    public record RiderAvailabilityRequest(boolean online, @Min(1) int capacity) {
    }

}
