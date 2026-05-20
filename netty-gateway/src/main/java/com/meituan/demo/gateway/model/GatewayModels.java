package com.meituan.demo.gateway.model;

import java.time.Instant;
import java.util.Map;

public final class GatewayModels {

    private GatewayModels() {
    }

    public enum FrameType {
        AUTH, PING, PONG, CHAT, SYSTEM, ACK
    }

    public record GatewayFrame(FrameType type, String traceId, Map<String, Object> payload) {
    }

    public record SessionInfo(Long userId, String role, String token, Instant connectedAt) {
    }
}

