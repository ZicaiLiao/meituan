package com.meituan.demo.backend.service;

import com.meituan.demo.backend.model.DomainModels.EventPayload;
import com.meituan.demo.backend.model.DomainModels.Role;
import com.meituan.demo.backend.security.DemoUserPrincipal;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class StreamService {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(DemoUserPrincipal principal) {
        String key = key(principal.role(), principal.id());
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(key, unused -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(key, emitter));
        emitter.onTimeout(() -> remove(key, emitter));
        emitter.onError(unused -> remove(key, emitter));
        send(key, emitter, "connected", new EventPayload("connected", "连接成功", "实时事件流已建立", Instant.now()));
        return emitter;
    }

    public void notifyUser(Role role, Long userId, String event, String title, String content) {
        String emitterKey = key(role, userId);
        List<SseEmitter> targets = emitters.getOrDefault(emitterKey, List.of());
        EventPayload payload = new EventPayload(event, title, content, Instant.now());
        targets.forEach(emitter -> send(emitterKey, emitter, event, payload));
    }

    public void notifyUsers(Map<Long, Role> users, String event, String title, String content) {
        users.forEach((userId, role) -> notifyUser(role, userId, event, title, content));
    }

    private void remove(String key, SseEmitter emitter) {
        emitters.getOrDefault(key, List.of()).remove(emitter);
    }

    private void send(String key, SseEmitter emitter, String event, EventPayload payload) {
        try {
            emitter.send(SseEmitter.event().name(event).data(payload));
        } catch (IOException | IllegalStateException ex) {
            remove(key, emitter);
            emitter.complete();
        }
    }

    private String key(Role role, Long userId) {
        return role.name() + ":" + userId;
    }
}
