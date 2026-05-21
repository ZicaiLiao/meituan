package com.meituan.demo.backend.service;

import com.meituan.demo.backend.model.ApiModels.CreateConversationRequest;
import com.meituan.demo.backend.model.ApiModels.CreateMessageRequest;
import com.meituan.demo.backend.model.DomainModels.Conversation;
import com.meituan.demo.backend.model.DomainModels.Message;
import com.meituan.demo.backend.model.DomainModels.MessageType;
import com.meituan.demo.backend.model.DomainModels.Role;
import com.meituan.demo.backend.repository.ChatRepository;
import com.meituan.demo.backend.repository.UserRepository;
import com.meituan.demo.backend.security.DemoUserPrincipal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final StreamService streamService;
    private final IntegrationEventService integrationEventService;

    public ChatService(
            ChatRepository chatRepository,
            UserRepository userRepository,
            StreamService streamService,
            IntegrationEventService integrationEventService) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.streamService = streamService;
        this.integrationEventService = integrationEventService;
    }

    public List<Conversation> conversationsForUser(DemoUserPrincipal principal) {
        return chatRepository.findConversationsByUserId(principal.id());
    }

    public Conversation createConversation(DemoUserPrincipal principal, CreateConversationRequest request) {
        Map<Long, Role> roles = new LinkedHashMap<>(userRepository.findRolesByUserIds(request.participantIds()));
        roles.put(principal.id(), principal.role());
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("At least one participant is required");
        }

        Conversation created = chatRepository.createConversation(request.scene(), request.orderId(), request.title());
        chatRepository.addParticipants(created.id(), roles);
        chatRepository.createMessage(created.id(), principal.id(), principal.role(), MessageType.SYSTEM, "会话已创建");
        Conversation conversation = chatRepository.findConversationById(created.id()).orElseThrow();
        streamService.notifyUsers(roles, "conversation.created", "新会话", request.title());
        return conversation;
    }

    public List<Message> messages(Long conversationId) {
        return chatRepository.findMessages(conversationId);
    }

    public Message createMessage(DemoUserPrincipal principal, CreateMessageRequest request) {
        MessageType type = request.type() == null ? MessageType.TEXT : request.type();
        Message message = chatRepository.createMessage(request.conversationId(), principal.id(), principal.role(), type, request.content());
        Conversation conversation = chatRepository.findConversationById(request.conversationId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + request.conversationId()));
        Map<Long, Role> recipients = conversation.participantRoles().entrySet().stream()
                .filter(entry -> !entry.getKey().equals(principal.id()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        streamService.notifyUsers(recipients, "chat.message", "新消息", request.content());
        integrationEventService.publish("chat-message-created",
                Map.of("conversationId", request.conversationId(), "senderId", principal.id(), "type", type.name()));
        return message;
    }
}
