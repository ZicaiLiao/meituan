package com.meituan.demo.gateway.core;

import com.meituan.demo.gateway.model.GatewayModels.SessionInfo;
import io.netty.channel.Channel;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SessionRegistry {

    private final Map<Channel, SessionInfo> byChannel = new ConcurrentHashMap<>();
    private final Map<String, Channel> byUserKey = new ConcurrentHashMap<>();

    public void register(Channel channel, SessionInfo sessionInfo) {
        byChannel.put(channel, sessionInfo);
        byUserKey.put(userKey(sessionInfo.role(), sessionInfo.userId()), channel);
    }

    public Optional<SessionInfo> session(Channel channel) {
        return Optional.ofNullable(byChannel.get(channel));
    }

    public Optional<Channel> channel(String role, Long userId) {
        return Optional.ofNullable(byUserKey.get(userKey(role, userId)));
    }

    public void unregister(Channel channel) {
        SessionInfo info = byChannel.remove(channel);
        if (info != null) {
            byUserKey.remove(userKey(info.role(), info.userId()));
        }
    }

    private String userKey(String role, Long userId) {
        return role + ":" + userId;
    }
}

