package com.meituan.demo.gateway.core;

import com.meituan.demo.gateway.model.GatewayModels.FrameType;
import com.meituan.demo.gateway.model.GatewayModels.GatewayFrame;
import com.meituan.demo.gateway.model.GatewayModels.SessionInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatewayServerHandler extends SimpleChannelInboundHandler<GatewayFrame> {

    private static final Logger log = LoggerFactory.getLogger(GatewayServerHandler.class);

    private final SessionRegistry sessionRegistry;
    private final TokenAuthenticator tokenAuthenticator;

    public GatewayServerHandler(SessionRegistry sessionRegistry, TokenAuthenticator tokenAuthenticator) {
        this.sessionRegistry = sessionRegistry;
        this.tokenAuthenticator = tokenAuthenticator;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        sessionRegistry.unregister(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GatewayFrame frame) {
        switch (frame.type()) {
            case AUTH -> handleAuth(ctx, frame);
            case PING -> handlePing(ctx, frame);
            case CHAT -> handleChat(ctx, frame);
            case ACK -> handleAck(ctx, frame);
            default -> ctx.writeAndFlush(new GatewayFrame(FrameType.SYSTEM, frame.traceId(),
                    Map.of("message", "Unsupported frame type")));
        }
    }

    private void handleAuth(ChannelHandlerContext ctx, GatewayFrame frame) {
        Object tokenValue = frame.payload().get("token");
        if (!(tokenValue instanceof String token)) {
            ctx.writeAndFlush(new GatewayFrame(FrameType.SYSTEM, frame.traceId(), Map.of("success", false, "reason", "missing token")));
            return;
        }
        Optional<SessionInfo> sessionInfo = tokenAuthenticator.authenticate(token);
        if (sessionInfo.isEmpty()) {
            ctx.writeAndFlush(new GatewayFrame(FrameType.SYSTEM, frame.traceId(), Map.of("success", false, "reason", "invalid token")));
            return;
        }
        sessionRegistry.register(ctx.channel(), sessionInfo.get());
        ctx.writeAndFlush(new GatewayFrame(FrameType.SYSTEM, frame.traceId(),
                Map.of("success", true, "userId", sessionInfo.get().userId(), "role", sessionInfo.get().role())));
    }

    private void handlePing(ChannelHandlerContext ctx, GatewayFrame frame) {
        ctx.writeAndFlush(new GatewayFrame(FrameType.PONG, frame.traceId(), Map.of("ts", System.currentTimeMillis())));
    }

    private void handleChat(ChannelHandlerContext ctx, GatewayFrame frame) {
        Optional<SessionInfo> sessionInfo = sessionRegistry.session(ctx.channel());
        if (sessionInfo.isEmpty()) {
            ctx.writeAndFlush(new GatewayFrame(FrameType.SYSTEM, frame.traceId(), Map.of("success", false, "reason", "unauthenticated")));
            return;
        }
        Long receiverId = Long.parseLong(String.valueOf(frame.payload().get("receiverId")));
        String receiverRole = String.valueOf(frame.payload().getOrDefault("receiverRole", "CUSTOMER"));
        String content = String.valueOf(frame.payload().getOrDefault("content", ""));
        log.info("Routing chat message from {}:{} to {}:{} content={}",
                sessionInfo.get().role(), sessionInfo.get().userId(), receiverRole, receiverId, content);

        sessionRegistry.channel(receiverRole, receiverId)
                .ifPresentOrElse(target -> target.writeAndFlush(new GatewayFrame(FrameType.CHAT, frame.traceId(),
                                Map.of("fromUserId", sessionInfo.get().userId(),
                                        "fromRole", sessionInfo.get().role(),
                                        "content", content))),
                        () -> log.info("Receiver {}:{} is offline", receiverRole, receiverId));

        ctx.writeAndFlush(new GatewayFrame(FrameType.ACK, frame.traceId(), Map.of("status", "DELIVERED_TO_GATEWAY")));
    }

    private void handleAck(ChannelHandlerContext ctx, GatewayFrame frame) {
        log.info("Received ack traceId={} payload={}", frame.traceId(), frame.payload());
        ctx.writeAndFlush(new GatewayFrame(FrameType.SYSTEM, frame.traceId(), Map.of("acknowledged", true)));
    }
}

