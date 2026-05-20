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

    public record AddCartItemRequest(@NotNull Long productId, @Min(1) int quantity) {
    }

    public record CreateOrderRequest(Long couponId, Long addressId) {
    }

    public record PayOrderRequest(String paymentChannel) {
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
}

