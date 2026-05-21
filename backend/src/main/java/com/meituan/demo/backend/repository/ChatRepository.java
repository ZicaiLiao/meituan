package com.meituan.demo.backend.repository;

import com.meituan.demo.backend.model.DomainModels.Conversation;
import com.meituan.demo.backend.model.DomainModels.ConversationScene;
import com.meituan.demo.backend.model.DomainModels.Message;
import com.meituan.demo.backend.model.DomainModels.MessageType;
import com.meituan.demo.backend.model.DomainModels.Role;
import com.meituan.demo.backend.persistence.PersistenceEntities.ConversationEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.ConversationParticipantEntity;
import com.meituan.demo.backend.persistence.PersistenceEntities.MessageEntity;
import com.meituan.demo.backend.persistence.mapper.ChatMapper;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class ChatRepository {

    private final ChatMapper chatMapper;

    public ChatRepository(ChatMapper chatMapper) {
        this.chatMapper = chatMapper;
    }

    public List<Conversation> findConversationsByUserId(Long userId) {
        return enrichParticipants(chatMapper.findConversationsByUserId(userId));
    }

    public Optional<Conversation> findConversationById(Long conversationId) {
        ConversationEntity entity = chatMapper.findConversationById(conversationId);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(enrichParticipants(List.of(entity)).getFirst());
    }

    public Conversation createConversation(ConversationScene scene, Long orderId, String title) {
        chatMapper.insertConversation(scene.name(), orderId, title);
        return findConversationById(chatMapper.lastInsertId()).orElseThrow();
    }

    public void addParticipants(Long conversationId, Map<Long, Role> roles) {
        roles.forEach((userId, role) -> chatMapper.insertParticipant(conversationId, userId, role.name()));
    }

    public List<Message> findMessages(Long conversationId) {
        return chatMapper.findMessages(conversationId).stream().map(this::toMessage).toList();
    }

    public Message createMessage(Long conversationId, Long senderId, Role senderRole, MessageType type, String content) {
        chatMapper.insertMessage(conversationId, senderId, senderRole.name(), type.name(), content, Instant.now());
        return Optional.ofNullable(chatMapper.findMessageById(chatMapper.lastInsertId())).map(this::toMessage).orElseThrow();
    }

    public List<Conversation> findSupportConversations() {
        return enrichParticipants(chatMapper.findSupportConversations());
    }

    private List<Conversation> enrichParticipants(List<ConversationEntity> conversations) {
        if (conversations.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ConversationParticipantEntity>> groupedParticipants =
                findParticipants(conversations.stream().map(ConversationEntity::id).toList());
        return conversations.stream().map(conversation -> {
            List<ConversationParticipantEntity> participants = groupedParticipants.getOrDefault(conversation.id(), List.of());
            Set<Long> participantIds = participants.stream().map(ConversationParticipantEntity::userId).collect(Collectors.toSet());
            Map<Long, Role> participantRoles = participants.stream()
                    .collect(Collectors.toMap(ConversationParticipantEntity::userId, entity -> Role.valueOf(entity.userRole())));
            return new Conversation(
                    conversation.id(),
                    ConversationScene.valueOf(conversation.scene()),
                    conversation.orderId(),
                    conversation.title(),
                    participantIds,
                    participantRoles);
        }).toList();
    }

    private Map<Long, List<ConversationParticipantEntity>> findParticipants(Collection<Long> conversationIds) {
        return chatMapper.findParticipants(conversationIds).stream()
                .collect(Collectors.groupingBy(ConversationParticipantEntity::conversationId));
    }

    private Message toMessage(MessageEntity entity) {
        return new Message(
                entity.id(),
                entity.conversationId(),
                entity.senderId(),
                Role.valueOf(entity.senderRole()),
                MessageType.valueOf(entity.messageType()),
                entity.content(),
                entity.createdAt());
    }
}
