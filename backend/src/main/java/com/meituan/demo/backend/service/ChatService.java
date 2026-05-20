package com.meituan.demo.backend.service;

import com.meituan.demo.backend.model.ApiModels.CreateConversationRequest;
import com.meituan.demo.backend.model.ApiModels.CreateMessageRequest;
import com.meituan.demo.backend.model.DomainModels.Conversation;
import com.meituan.demo.backend.model.DomainModels.Message;
import com.meituan.demo.backend.model.DomainModels.MessageType;
import com.meituan.demo.backend.model.DomainModels.Role;
import com.meituan.demo.backend.security.DemoUserPrincipal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final DemoDataStore dataStore;
    private final StreamService streamService;
    private final IntegrationEventService integrationEventService;

    public ChatService(DemoDataStore dataStore, StreamService streamService, IntegrationEventService integrationEventService) {
        this.dataStore = dataStore;
        this.streamService = streamService;
        this.integrationEventService = integrationEventService;
    }

    public List<Conversation> conversationsForUser(DemoUserPrincipal principal) {
        return dataStore.conversations().values().stream()
                .filter(conversation -> conversation.participantIds().contains(principal.id()))
                .sorted(Comparator.comparing(Conversation::id).reversed())
                .toList();
    }

    public Conversation createConversation(DemoUserPrincipal principal, CreateConversationRequest request) {
        Set<Long> participantIds = Set.copyOf(request.participantIds());
        Map<Long, Role> roles = participantIds.stream()
                .collect(Collectors.toMap(id -> id, id -> dataStore.users().get(id).role()));
        Conversation conversation = new Conversation(dataStore.nextConversationId(), request.scene(),
                request.orderId(), request.title(), participantIds, roles);
        dataStore.conversations().put(conversation.id(), conversation);
        dataStore.messagesByConversation().put(conversation.id(), new ArrayList<>(List.of(new Message(
                dataStore.nextMessageId(),
                conversation.id(),
                principal.id(),
                principal.role(),
                MessageType.SYSTEM,
                "会话已创建",
                Instant.now()))));
        streamService.notifyUsers(roles, "conversation.created", "新会话", request.title());
        return conversation;
    }

    public List<Message> messages(Long conversationId) {
        return dataStore.messagesByConversation().getOrDefault(conversationId, List.of());
    }

    public Message createMessage(DemoUserPrincipal principal, CreateMessageRequest request) {
        MessageType type = request.type() == null ? MessageType.TEXT : request.type();
        Message message = new Message(dataStore.nextMessageId(), request.conversationId(), principal.id(),
                principal.role(), type, request.content(), Instant.now());
        dataStore.messagesByConversation()
                .computeIfAbsent(request.conversationId(), unused -> new ArrayList<>())
                .add(message);

        Conversation conversation = dataStore.conversations().get(request.conversationId());
        Map<Long, Role> recipients = conversation.participantRoles().entrySet().stream()
                .filter(entry -> !entry.getKey().equals(principal.id()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        streamService.notifyUsers(recipients, "chat.message", "新消息", request.content());
        integrationEventService.publish("chat-message-created",
                Map.of("conversationId", request.conversationId(), "senderId", principal.id(), "type", type.name()));
        return message;
    }
}

